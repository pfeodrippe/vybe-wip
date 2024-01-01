(ns vybe.game
  "Namespace for game stuff."
  (:require
   [clojure.walk :as walk]
   [nextjournal.beholder :as beholder]
   [clojure.java.io :as io]
   [clojure.string :as str]
   [clojure2d.color :as c]
   [clojure.set :as set]
   [potemkin :refer [def-map-type]]
   [vybe.protocol :as proto]
   [clojure.math.combinatorics :as combo]
   [vybe.component :as vy.c]
   [vy.visibility :as-alias vi]
   [clj-java-decompiler.core :refer [decompile disassemble]]
   [vybe.api :as vy]
   [vybe.util :as vy.u]
   [jsonista.core :as json])
  (:import
   (com.badlogic.gdx.graphics.glutils ShaderProgram FrameBuffer)
   (com.badlogic.gdx Game Gdx Graphics Screen InputAdapter)
   (com.badlogic.gdx.graphics.g2d.freetype FreeTypeFontGenerator FreeTypeFontGenerator$FreeTypeFontParameter)
   (com.badlogic.gdx.graphics Color GL20 OrthographicCamera Texture Mesh VertexAttribute VertexAttributes
                              Cursor$SystemCursor
                              VertexAttributes VertexAttribute VertexAttributes$Usage
                              Pixmap$Format)
   (com.badlogic.gdx.graphics.g2d BitmapFont GlyphLayout CustomSpriteBatch Batch TextureRegion Animation)
   (java.util Iterator)
   (clojure.lang IAtom2)))

(set! *warn-on-reflection* true)
#_(set! *unchecked-math* :warn-on-boxed)

;; ------------- Utils ---------------

(set! ShaderProgram/pedantic true)
#_(set! ShaderProgram/pedantic false)

(defonce *resources (atom {}))

(defmacro with-managed
  [game-id resource-path builder opts & body]
  `(let [resource# (do ~@body)
         canonical-path# (when ~resource-path
                           (.getPath (io/resource ~resource-path)))]
     (swap! *resources update-in [~game-id (or canonical-path#
                                               (gensym))]
            conj (merge {:resource-path canonical-path#
                         :resource resource#
                         :builder ~builder}
                        ~opts))
     resource#))

;; Used for `env`, this acts as a persistent (immutable) map if you only
;; use the usual persistent functions (`get`, `assoc`, `update` etc), while
;; it will change the underlying atom if you use `IAtom` (and `IAtom2`) functions
;; like `swap!`, `reset!` etc.
(def-map-type MutableMap [^IAtom2 *m ^IAtom2 *temp-m]
  (get [_ k default-value]
       (if (contains? @*temp-m k)
         (get @*temp-m k default-value)
         (get @*m k default-value)))
  (assoc [_ k v]
         (MutableMap. *m (atom (assoc @*temp-m k v))))
  (dissoc [_ k]
          (cond
            (contains? @*temp-m k)
            (MutableMap. *m (atom (dissoc @*temp-m k)))

            (contains? @*m k)
            (throw (ex-info (str "Can't dissoc a MutableMap, use `swap!` "
                                 "instead if you want to change it globally")
                            {:k k}))))
  (keys [_]
        (distinct (concat (keys @*temp-m)
                          (keys @*m))))
  (meta [_]
        (meta @*m))
  (with-meta [_ metadata]
    (MutableMap. (swap! *m with-meta metadata) *temp-m))

  IAtom2
  (swap [_ f]
        (.swap *m f))
  (swap [_ f a1]
        (.swap *m f a1))
  (swap [_ f a1 a2]
        (.swap *m f a1 a2))
  (swap [_ f a1 a2 a-seq]
        (.swap *m f a1 a2 a-seq))
  (compareAndSet [_ old new]
                 (.compareAndSet *m old new))
  (reset [_ v]
         (.reset *m v))

  (swapVals [_ f]
            (.swapVals *m f))
  (swapVals [_ f a1]
            (.swapVals *m f a1))
  (swapVals [_ f a1 a2]
            (.swapVals *m f a1 a2))
  (swapVals [_ f a1 a2 a-seq]
            (.swapVals *m f a1 a2 a-seq))
  (resetVals [_ v]
             (.resetVals *m v)))

(defn make-env
  []
  (->MutableMap (atom {}) (atom {})))

(comment

  (let [a (make-env)]
    (swap! a assoc :a 4)
    [(assoc a :a 5 :b 55)
     (keys (assoc a :a 5 :b 55))
     (keys a)
     a
     (:a a)])

  ())

;; -------------- COLOR -----------------

(defn ->color
  "Converts a color from Clojure to a map (:r :g :b :a)."
  [color]
  (zipmap [:r :g :b :a] (c/scale-down color true)))

(defn set-color
  [{::keys [^Batch batch] :as env} color]
  (let [color (or color (->color :white))
        {:keys [r g b a]} color]
    (doto batch
      (.setColor (float r) (float g) (float b) (float a))))
  (assoc env ::color color))

;; ------------- SHAPE ----------------------

(defn rect
  "Draw a rectangle."
  [{::keys [^Batch batch ^Texture one-pixel-tex screen-size] :as env}
   {:keys [x y]}
   {:keys [width height]}]
  (let [[_screen-width screen-height] screen-size]
    (doto batch
      (.draw one-pixel-tex
             (float x) (float (- ^long screen-height ^float y))
             (float width) (float (- ^float height)))))
  env)

;; --------------- FONT ------------------

(defn font-params
  "Accepted font params for the `font` function."
  []
  (->> (.getDeclaredFields FreeTypeFontGenerator$FreeTypeFontParameter)
       (mapv bean)
       (mapv (juxt (comp keyword :name) :type))
       (into (sorted-map))))

#_ (font-params)

(def -font-fields
  (->> (.getDeclaredFields FreeTypeFontGenerator$FreeTypeFontParameter)
       (mapv (fn [^java.lang.reflect.Field field]
               (let [{:keys [name type]} (bean field)
                     f (or ({"int" int
                             "boolean" boolean
                             "float" float}
                            (.getName ^Class type))
                           identity)]
                 [(keyword name) (fn [obj v]
                                   (.set field ^Object obj ^Object (f v)))])))
       (into (sorted-map))))

(defn font
  "Managed. Generate a font with some params (derived from a TTF file in the classpath).

  `params` is a map, see `vg/font-params` for the available options."
  [game-id resource-path params]
  (with-managed game-id resource-path #(font game-id resource-path params) {}
    (let [generator (FreeTypeFontGenerator. (.classpath Gdx/files resource-path))
          params-obj (FreeTypeFontGenerator$FreeTypeFontParameter.)]
      (doseq [[param-n param-v] params]
        ((-font-fields param-n) params-obj param-v))
      (try
        (.generateFont generator params-obj)
        (finally
          (.dispose generator))))))

(defn text
  "Writes some text to the screen."
  [{::keys [batch screen-size ^BitmapFont font color font-scale]
    :or {font-scale 1.0}
    :as env}
   ^String txt
   position]
  (let [[x y] position
        [_screen-width screen-height] screen-size
        {:keys [r g b a]} (or color (->color :white))]
    (when font-scale
      (.setScale (.getData font) font-scale))
    (doto font
      (.setColor (float r) (float g) (float b) (float a))
      (.draw ^Batch batch (str txt) (float x) (float (- ^long screen-height ^float y))
             #_ #_ #_ (float 400) com.badlogic.gdx.utils.Align/left true)))
  env)

;; --------------- HELPER --------------

(defn fps
  "Return current frames per second."
  []
  (.getFramesPerSecond Gdx/graphics))

(defn gl
  "Abstraction for the GL component."
  []
  (Gdx/gl20))

(defn clear-color
  "Clear color (set color)."
  [color]
  (let [{:keys [r g b a]} color]
    (doto ^GL20 (gl)
      (.glClearColor r g b a))))

;; --------------- SHADER --------------

(defn- pre-process-shader
  [shader-res-path]
  (let [file (.classpath Gdx/files shader-res-path)
        folder-name (str (.parent file))]
    (->> (slurp (.read file))
         str/split-lines
         (mapv (fn [line]
                 (if-let [dep-relative-path (-> (re-matches #"#include \"(.*)\"" line)
                                                last)]
                   (pre-process-shader (-> (io/file folder-name dep-relative-path)
                                           .toPath
                                           ;; Normalize so we get rid of any `../`
                                           .normalize
                                           str))
                   line)))
         (str/join "\n"))))

(comment

  (str/split-lines (pre-process-shader "shaders/main.frag"))

  ())

(defonce *shaders-cache (atom {}))

(defn builtin-path
  "Build the path for a built-in resource."
  [res-path]
  (str "com/pfeodrippe/vybe/" res-path))

(defn shader-program
  "Create a shader-program."
  ([]
   (shader-program (builtin-path "shaders/default.vert") (builtin-path "shaders/default.frag")))
  ([frag-res-path]
   (shader-program (builtin-path "shaders/default.vert") frag-res-path))
  ([vertex-res-path frag-res-path]
   ;; This first should be the one used in PRD.
   #_(or (get @*shaders-cache [vertex-res-path frag-res-path])
         (let [vertex-shader-str (pre-process-shader vertex-res-path)
               frag-shader-str (pre-process-shader frag-res-path)
               shader (ShaderProgram. ^String vertex-shader-str ^String frag-shader-str)]
           (when-not (.isCompiled shader)
             (throw (ex-info "Error when compiling shader" {:vertex-res-path vertex-res-path
                                                            :frag-res-path frag-res-path
                                                            :log (.getLog shader)})))
           (swap! *shaders-cache assoc [vertex-res-path frag-res-path] shader)
           shader))
   (let [vertex-shader-str (pre-process-shader vertex-res-path)
         frag-shader-str (pre-process-shader frag-res-path)]
     (or (get @*shaders-cache [vertex-shader-str frag-shader-str])
         (let [shader (ShaderProgram. ^String vertex-shader-str ^String frag-shader-str)]
           (when-not (.isCompiled shader)
             (throw (ex-info "Error when compiling shader" {:vertex-res-path vertex-res-path
                                                            :frag-res-path frag-res-path
                                                            :log (.getLog shader)})))
           (swap! *shaders-cache assoc [vertex-shader-str frag-shader-str] shader)
           shader)))))

(defn -adapt-shader
  [shader]
  (if (instance? ShaderProgram shader)
    shader
    (shader-program (or (::shader.vert shader)
                        (builtin-path "shaders/default.vert"))
                    (or (::shader.frag shader)
                        (builtin-path "shaders/default.frag")))))

(defn set-shader
  [{::keys [^Batch batch] :as env} shader]
  (.setShader batch (-adapt-shader shader))
  env)

(defn set-uniform
  [env shader uniform value]
  (let [sp ^ShaderProgram (-adapt-shader shader)]
    (.bind sp)
    (if (vector? value)
      (.setUniform2fv sp (name uniform)
                      (float-array value)
                      0 2)
      (.setUniformf sp (name uniform) (float value))))
  env)

(defonce ^:dynamic *custom-attrs* (atom {}))

(defmacro with-batch
  [env & body]
  (let [batch (with-meta (gensym) {:tag `Batch})
        opts (first body)
        {:keys [shader]} (when (map? opts) opts)
        [set-shader-expr dispose-expr] (when shader
                                         [`(set-shader ~{::batch batch} ~shader)
                                          `(set-shader ~{::batch batch} nil)])]
    `(let [~batch (::batch ~env)]
       (binding [*custom-attrs* (atom {})]
         (do
           (try
             (.begin ~batch)
             (catch Exception _#
               (.end ~batch)
               (.begin ~batch)))
           ~set-shader-expr
           (try
             ~@body
             (finally
               ~dispose-expr
               (.end ~batch))))))))

(defn batch
  [game-id]
  (with-managed game-id nil #(batch game-id) {}
    (let [new-attrs [[:a_resolution {:size 2}]
                     [:a_stroke {:size 1}]
                     [:a_default {:size 1}]]
          new-attrs-map (->> (drop 1 new-attrs)
                             (map-indexed (fn [idx [attr v]]
                                            [attr (merge v {:idx idx})]))
                             (into {}))]
      (CustomSpriteBatch.
       1000
       (shader-program)
       (fn []
         (->> (concat [(VertexAttribute. VertexAttributes$Usage/Position 2 ShaderProgram/POSITION_ATTRIBUTE)
                       (VertexAttribute. VertexAttributes$Usage/ColorPacked 4 ShaderProgram/COLOR_ATTRIBUTE)
                       (VertexAttribute. VertexAttributes$Usage/TextureCoordinates 2 (str ShaderProgram/TEXCOORD_ATTRIBUTE
                                                                                          "0"))]
                      (->> new-attrs
                           (mapv (fn [[attr {:keys [size]}]]
                                   (VertexAttribute. VertexAttributes$Usage/Generic size (name attr))))))
              (into-array VertexAttribute)))
       (fn []
         (int (* (reduce (fn [acc [_attr {:keys [size]}]]
                           (+ acc size))
                         5
                         new-attrs)
                 4)))
       (fn [vertices idx x y width height]
         (aset-float vertices idx width)
         (aset-float vertices (inc ^int idx) height)
         (->> @*custom-attrs*
              (mapv (fn [[attr v]]
                      (aset-float vertices
                                  (+ ^int idx 2 ^long (get-in new-attrs-map [attr :idx]))
                                  v)))))))))

(defn set-custom
  "Set custom vertex atribute.

  E.g. `(set-custom :a_stroke 1)"
  [env attr v]
  (swap! *custom-attrs* assoc attr v)
  env)

;; ----------------------- TEXTURE/ANIMATION ---------------------

(defn texture
  "Managed, builds a libgdx texture."
  (^Texture [game-id resource-path]
   (texture game-id resource-path {}))
  (^Texture [game-id resource-path {:keys [managed-opts]}]
   (with-managed game-id resource-path #(texture game-id resource-path managed-opts) managed-opts
     (Texture. (.classpath Gdx/files resource-path)))))

(defn animation
  "Create a libgdx animation, texture created from `resource-path` will be
  managed.

  `json-path` should be in the aseprite format (array, not hash!).

  E.g. of the required keys (JSON parsed to EDN format)
  {:frames [{:frame {:x 10 :y 10 :w 30 :h 40}}]}"
  [game-id resource-path json-path]
  (with-managed game-id resource-path #(animation game-id resource-path json-path) {:dispose (constantly nil)}
    (let [tex (texture game-id resource-path {:managed-opts {:builder (constantly nil)}})
          {:keys [frames]} (vy.u/parse-json (io/resource json-path))
          ^{:tag "[Ljava.lang.Object;"} frame-arr (->> frames
                                                       (mapv (fn [{:keys [frame]}]
                                                               (let [{:keys [x y w h]} frame]
                                                                 (TextureRegion. tex
                                                                                 ^int x
                                                                                 ^int y
                                                                                 ^int w
                                                                                 ^int h))))
                                                       (into-array Object))]
      (Animation. ^float (float 0.1) frame-arr))))

(defn draw-animation
  [{::keys [screen-size total-time ^Batch batch] :as env}
   texture-region
   {:keys [x y]}
   {:keys [width height]}]
  (let [[_screen-width screen-height] screen-size
        frame-tex ^TextureRegion (.getKeyFrame ^Animation texture-region total-time true)]
    (doto batch
      (.draw frame-tex
             (float x) (float (- ^long screen-height (float y)))
             (float width) (float height))))
  env)

;; ----------------------- FBO -------------------------

(defn fbo
  "Create a FBO."
  [game-id]
  (with-managed game-id nil #(fbo game-id) {}
    (let [graphics ^com.badlogic.gdx.backends.lwjgl3.Lwjgl3Graphics (Gdx/graphics)]
      (FrameBuffer. Pixmap$Format/RGBA8888
                    (.getBackBufferWidth graphics)
                    (.getBackBufferHeight graphics)
                    false))))

;; TODO Fix the FBO parameters (it's using an specific shader, but this should
;; come from config)
(defn draw-fbo
  "Draw frame buffer object."
  ([env position size]
   (draw-fbo env position size {}))
  ([{::keys [^FrameBuffer fbo ^Batch fbo-batch screen-size] :as env} position size
    {:keys [shader]}]
   (let [[x y] position
         [width height] size
         [_screen-width screen-height] screen-size
         draw (fn []
                (.draw fbo-batch
                       ^Texture (.getColorBufferTexture fbo)
                       (float x) (float (- screen-height y))
                       (float width) (float (- height))))]
     (if (.isDrawing fbo-batch)
       (draw)
       (with-batch (cond-> (assoc env ::batch fbo-batch)
                     shader
                     (-> (set-shader shader)
                         (set-uniform {::shader.frag "shaders/grain.frag"} "u_time" (* (::total-time env) 0.1))))
         (draw))))))

(defmacro with-fbo
  "Bind FBO at `::vg/fbo`."
  [env & body]
  `(try
     (.begin ^FrameBuffer (::fbo ~env))
     (doto ^GL20 (gl)
       (.glClear GL20/GL_COLOR_BUFFER_BIT)
       (.glEnable GL20/GL_TEXTURE_2D))
     ~@body
     (finally
       (.end ^FrameBuffer (::fbo ~env)))))

(defmacro with-fx
  "Apply a special effect (shader) to the entire screen.

  E.g.

  (with-fx env {::vg/shader.frag \"...\"}
    ...
    ...)"
  [env shader & body]
  `(do
     (with-fbo ~env
       ~@body)

     (let [[width# height#] (::screen-size ~env)]
       (draw-fbo ~env [0 0] [width# height#] {:shader ~shader}))))

;; ----------------------- HELPER -------------------------

(defn window-pos
  "Set window position."
  [x y]
  (-> ^com.badlogic.gdx.backends.lwjgl3.Lwjgl3Graphics (Gdx/graphics)
      .getWindow
      (.setPosition x y)))

(defn close-app
  []
  (some-> Gdx/app .exit))

(defn dispose-resources
  "Dispose resources for a game."
  ([game-id]
   (dispose-resources game-id :all))
  ([game-id resources-paths]
   (->> (get @*resources game-id)
        (filter (if (= resources-paths :all)
                  (constantly true)
                  (comp (set resources-paths) key)))
        (mapv (fn [[id coll]]
                (mapv (fn [{:keys [resource dispose] :as res-params}]
                        #_(println "Disposing" resource "for" id)
                        (try
                          (if dispose
                            (dispose)
                            (.dispose ^com.badlogic.gdx.utils.Disposable resource))
                          (swap! *resources update-in [game-id id] #(remove #{res-params} %))
                          (catch Exception e
                            (println e))))
                      coll))))
   (when (= resources-paths :all)
     (swap! *resources dissoc game-id))))

(defn recreate-resources
  "Recreate (possibly) modified resources for a game."
  [env game-id resources-paths]
  #_ (def game-id (ffirst @*resources))
  #_(println "Recreating resources" {:resources-paths resources-paths})
  (let [resources (get @*resources game-id)
        resources-group (->> resources
                             (filter (comp (set resources-paths) key))
                             vals
                             (apply concat)
                             (mapv (juxt :resource identity))
                             (into {}))
        reset (fn []
                (reset! env (walk/prewalk (fn [v]
                                            (if-let [r-params (get resources-group v)]
                                              ((:builder r-params))
                                              v))
                                          env)))]
    (dispose-resources game-id resources-paths)
    (try
      (reset)
      (catch Exception _
        ;; Try again in case some resource is in an invalid format.
        (reset)))))

;; ------------------------- SYSTEMS ------------------------

(defn dev-resources-watcher
  "To be used with `dev-system`.

  Initiates resources watcher, default path is `resources`."
  ([world]
   (dev-resources-watcher world {}))
  ([world {:keys [path]
           :or {path "resources"}}]
   (future
     (beholder/watch
      (fn [{:keys [type path]}]
        (try
          (when (contains? #{:create :modify :overflow} type)
            (vy/add-c world (vy.c/ResourceChanged {:path (str path)})))
          (catch Exception e
            (println e))))
      path))))

(defn dev-system
  "This system runs on Flec's OnLoad phase and checks if any resources or vars
  needs to be reloaded.

  It should only be used in development so you can have propery hot reload!"
  {:vy/query [vy.c/ResourceChanged]
   :vy/phase :vy.b/EcsOnLoad}
  [{::keys [game] :as env} iter]
  (vy/with-changed iter
    (vy/with-each iter [res vy.c/ResourceChanged
                        entity :vy/entity]
      (let [path (vy/pget res :path)
            world (vy/iter-world iter)]
        (recreate-resources env game #{path})
        (vy/delete world entity)))))
