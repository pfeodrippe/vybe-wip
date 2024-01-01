(ns pfeodrippe.healthcare
  (:require
   [clojure.string :as str]
   [clojure2d.color :as color]
   [clojure2d.core :as c2d]
   [vy.event :as-alias ev]
   [vy.id :as-alias id]
   [vy.tag :as-alias t]
   [vy.visibility :as-alias vi]
   [vy.scene :as-alias scn]
   [vybe.audio :as audio]
   [vybe.api :as vy]
   [vybe.component :as vy.c]
   [vybe.jnr :as jnr :refer [c-api]]
   [vybe.game :as vg]
   [clj-java-decompiler.core :refer [decompile disassemble]]
   [jsonista.core :as json]
   [nextjournal.beholder :as beholder]
   [portal.api :as p]
   vybe.type)
  (:import
   (com.badlogic.gdx Game Gdx Graphics Screen InputAdapter Audio Input$Keys)
   (com.badlogic.gdx.utils.viewport ExtendViewport)
   (com.badlogic.gdx.math Matrix4)
   (com.badlogic.gdx.graphics.glutils ShaderProgram)
   (com.badlogic.gdx.graphics Color GL20 OrthographicCamera Texture Mesh VertexAttribute VertexAttributes
                              Cursor$SystemCursor
                              VertexAttributes VertexAttribute VertexAttributes$Usage
                              Camera
                              Pixmap$Format)
   (com.badlogic.gdx.graphics.g2d BitmapFont SpriteBatch GlyphLayout CustomSpriteBatch Batch Animation TextureRegion)
   (com.badlogic.gdx.scenes.scene2d Stage)
   (com.badlogic.gdx.scenes.scene2d.ui Label Label$LabelStyle)
   (com.badlogic.gdx.backends.lwjgl3 Lwjgl3Application Lwjgl3ApplicationConfiguration)
   (com.badlogic.gdx.math Rectangle Vector3)
   (org.lwjgl.glfw GLFW)
   #_(org.lwjgl.opengl.awt GLData AWTGLCanvas)
   (com.badlogic.gdx.graphics.g2d.freetype FreeTypeFontGenerator FreeTypeFontGenerator$FreeTypeFontParameter)
   (com.badlogic.gdx.graphics.profiling GLProfiler)
   (com.badlogic.gdx.audio Sound)
   (jnr.ffi Struct)
   (vybe.type VybeComponent)))

(set! *warn-on-reflection* false)
#_(set! *unchecked-math* :warn-on-boxed)

;; DONE Add prefabs
;;   DONE add-prefabs
;;   DONE Maybe in another ns (:vy.c/... ?)
;;   DONE Can query prefabs
;;     DONE See prefabs available
;;     DONE See prefab details
;; DONE Make sure we return only valid components in entity-info (maybe using ecs_has_id)
;; DONE Use `EcsExclusive` for visibility
;; DONE Ability to do a query for a given entity
;; DONE Render
;;   DONE Cursor
;;   DONE Pass env to the system
;;   DONE FPS
;;     DONE TextSection component
;;     DONE Render
;;   DONE Buttons
;; DONE Use implicit type hint for `pset`
;; DONE Support boolean fields
;;   DONE pset
;;   DONE pget
;; DONE Hover
;;   DONE Get an entity
;;   DONE Events (observers?)
;;   DONE Hover sound
;; DONE Click sound
;; LATER Fix continuosly sorting!
;; DONE Should all the query components be `in` by default?
;; DONE Email screen
;;   DONE Fix world cache issue
;;   DONE Map from deferred_world to real_world
;;   DONE It seems that we have some issues with the email fg vs bg color
;;   DONE Email hover
;;   DONE Scene
;;   DONE Fix fg/bg issue
;;   DONE Fix flickering
;;   DONE See how to get debug information about system/pipe execution
;;   DONE When clicking on `x`
;;     DONE `t` does not appear anymore
;;     DONE Click on `m` does not work
;;   DONE Fix small flickering when changing scenes
;;   DONE Remove entity from `:component` when repairing
;;   DONE Style and scene
;;     DONE Get active scene related data
;;     DONE Clear screen with appropriate color
;;   DONE Add `x` button
;;     DONE Button
;;     DONE Bar
;; DONE New prefabs as we have lots of repetition everywhere
;; DONE Fix "Can't set a read only field" issue in enable-ui
;; DONE Make build work
;;   - When really packaging, make sure the vybe.dylib lib is in the file system,
;;     not in resources!
;;   - https://github.com/jnr/jnr-ffi/issues/93
;; DONE Use `[Position :global]` as the real position
;;   - https://github.com/flecs-hub/flecs-systems-transform/blob/master/src/main.c
;;     LATER Check it later for using instancing (optimization)
;;   DONE Add system to calculate it
;;   DONE Fix flickering
;; DONE Get relationship value
;; DONE Email details
;;   DONE Fix hover/click for buttons behind the message modal
;;   DONE Gray overlay with message details
;;   DONE Pass parameters to the on-add event
;;   DONE Fix text
;;   DONE Close email details
;;     DONE Create something more generic for it
;;   DONE Picture in pixel art inside the message
;;     DONE Dog
;; DONE Make texture, batch etc managed resources
;;   DONE Opt-in system that checks the file watcher
;; DONE Animation
;;   DONE Support hot reload in dev mode
;;     DONE File watcher
;; DONE Render animation only when opening an email
;; DONE Dynamic list of emails
;; DONE Make most recent email appear at the top
;; DONE "Remove an email" button
;; DONE Use something other than `:dffaf`
;; DONE Add some shader filter so we can have a blueprint
;; DONE Test shader filter for some elements
;; DONE Edit mode (`e` toggles it)
;;   DONE Show all positions points (parents first)
;;   DONE Edit position by clicking and dragging the mouse
;;   DONE You can see and copy the modified properties
;; TODO Write story in org mode
;;   TODO Minimal outline for a dialogue
;;   TODO Derive dialogue from it
;;   - https://www.tomheon.com/2019/04/10/how-an-uber-geeky-text-mode-in-a-40-year-old-editor-saved-my-novel
;; TODO Dialogue system
;;   DONE Reply button
;;   DONE Show answer options
;;   TODO Expand selected answer and "send" the email response
;;   DONE "No" leads to some message ("Hunnn, don't care")
;;   TODO Show a picture for `No` to show that's the player that's talking
;;   TODO "Yes"
;;     TODO Message being typed
;;     TODO Sent Action
;;   TODO Next action (what happens?)
;; TODO News system
;;   TODO Can be in the email
;; TODO Notifications system
;; TODO Onboarding
;;   TODO Hi email
;; TODO External intimate person
;; TODO Create a 5-min game E2E
;; TODO Music
;; TODO Use clerk in edit mode
;;   TODO Show element being modified in real time
;; TODO To fix the sort problem, maybe we can create some groups (4?) that store
;;      the depth like layers
;; TODO See what we mutate in env so we can put it in Flecs
;; TODO Assert when a pget/pselect is using a writeonly component or when
;;      a pset/pupdate is using a readonly one?
;; TODO Z ordering driven by query sorting?
;; TODO System for the global visibility?
;; TODO Add tests
;; TODO Test using Flavinha's Mac
;; TODO Create render component out of prefabs?
;; TODO Debug log
;;   TODO Instead of writing to a file system, pass a callback function that can be used
;;        with DDD
;; TODO Use :with as an alias for :none in queries
;; TODO Check if we can use color as a reference to an index so we can have
;;      real-time color pallete changes
;; TODO Opt-in system that hot reloads systems
;; TODO Convert from `ui` to `2d`
;; TODO Fix add-systems memory leak (maybe by making it resource-like)
;; TODO Try to fix the query of components (tags, keywords mostly) that are
;;      children of some other entity
;; TODO Destructuring in `with-each` to get a given component
;; TODO Cache text to a FBO
;;   - https://www.reddit.com/r/libgdx/comments/5vpfp1/render_lots_of_text_to_texture_to_improve/de42ian/?utm_source=share&utm_medium=web3x&utm_name=web3xcss&utm_term=1&utm_content=share_button
;; TODO Do some 2d culling so we don't draw "out of the view port" objects
;; TODO Helper function to return components/tags that have a parent (and their
;;      full paths)
;;   - Useful for debugging queries that are not working as expected
;; TODO Should bounding boxes (e.g. for clicks) in some sort of debug mode
;; TODO How to do UI tests?
;; TODO Fix sorting issue (again)
;; TODO Support `ctrl-z` in edit mode

(comment

  (do
    (def world (vy/init))

    (vy.c/initialize! world)

    (vy/entity-info world :vy.pf/sprite)

    (vy/entity-info world :fff)

    (vy/add-c world :fff :a)

    (vy/query-debug world [:vy.b/EcsPrefab] {:entity-info? true})

    (vy/add-c world :pitoco (vy/is-a :vy.pf/sprite))
    (vy/add-c world :pitoco [:vy.c/visibility :vy.visibility/hidden])
    (vy/entity-info world :pitoco)

    (vy/query-debug world [[:vy.c/visibility :*]] {:entity-info? true})

    (vy/query-debug world [[vy.c/Position :global]
                           vy.c/Position
                           [:vy.c/visibility :vy.visibility/hidden]]
                    {:entity-info? true})

    (doto world
      (vy/add-many ::id/cursor [(vy/is-a :vy.pf/sprite)
                                (vy.c/Size {:width screen-width
                                            :height screen-height})])
      (vy/add-many ::id/fps
                   [(vy/is-a :vy.pf/sprite)
                    (vy.c/Position {:x 513 :y 570})
                    (vy.c/Size {:width 10 :height 20})
                    (TextSection {:text "xss"
                                  :scale 10/38
                                  :fg_color (get colors 2)
                                  :bg_color :white})])))

  (vy/add-c world :gaga (vy.c/Position {:x 44}))
  (vy/query-debug world vy.c/Position {:entity-info? true})

  (vy/add-many world :eita [[:d :fdf]
                            :vy.b/EcsExclusive])
  (vy/entity-info world :eita)
  (vy/delete world :eita)

  (c-api :stage_is_readonly world)
  (c-api :is_deferred world)

  #_ (vy/-entity-info world vy.c/Position)

  (for [_ (range 3000)]
    (vy/query-debug world [vy.c/Position vy.c/Size [:maybe TextSection]] #_{:entity-info? true}))

  (do

    (vy/with-scope world :parent6
      (vy/add-c world :abc1 :eee)
      (vy/with-scope world :parent8
        (vy/add-c world :abc2 :eee)))

    (vy/entity-info world :abc1)
    (vy/entity-info world :abc2)
    (vy/delete-children world :parent6)
    (vy/delete-children world :sss)
    (vy/alive? world :parent6)

    (vy/query-debug world [[:vy.b/EcsChildOf :parent6]])

    (vy/add-pipeline world :fafa [:asss])
    (vy/entity-info world :fafa)

    ())




  (do

    (vy/entity-info world :vy.b/EcsIsA)
    (c-api :lookup_symbol
           world
           (str "flecs.core")
           false
           false)
    (vy/get-c world :vy.b/EcsIsA [:vy.b/EcsChildOf "flecs.core"])

    (c-api :has_id world (vy/->id world :vy.b/EcsIsA) (vy/->id world [:vy.b/EcsChildOf "flecs.core"]))
    (c-api :get_target_for_id
           world
           (vy/->id world :vy.b/EcsIsA)
           (vy/->id world :vy.b/EcsChildOf)
           (vy/->id world "flecs.core"))
    (vy/entity-info world (c-api :get_parent world (vy/->id world :vy.b/EcsIsA)))

    (c-api :lookup_path_w_sep world 0 "flecs.core" "." "" false)

    (vy/get-c world (c-api :get_parent world (vy/->id world :vy.b/EcsIsA))
              [:vy.b/EcsIdentifier :vy.b/EcsSymbol])

    (c-api :id_is_pair (vy/->id world [:vy.b/EcsIsA :a]))
    (= (vy/->id world :vy.b/EcsIsA)
       (c-api :vybe_pair_first world (vy/->id world [:vy.b/EcsIsA :a])))

    ())

  ())





;; DONE Render rectangle
;; DONE Render button
;; DONE Render text
;; DONE Draw FPS
;; DONE Cursor
;; DONE Button line
;; DONE We should use vertex attributes to pass the size
;; DONE Fix font
;; DONE Shader
;; DONE ECS
;;   DONE Hover effect
;;   DONE Update an entity
;;   DONE System for adding/removing hovering
;;   DONE Cursor
;;   DONE Draw position
;;   DONE See if we could use https://github.com/dominion-dev/dominion-ecs-java
;;     Yes, we can
;;     LATER Maybe use keywords instead of classes so we don't have to import
;;           classes all the time
;;     LATER We can define things using a map instead of multiple `defcomp`s
;;   DONE Fix weird Z ordering
;;   DONE Apply effect
;;   DONE Events
;;     DONE Custom events
;;     DONE Hover
;;   DONE Play sound when hovering
;; DONE Use one rect shader and pass vertex attributes for the selected state
;; DONE Text inside buttons
;;   DONE Variable text font
;;   DONE Email
;;   DONE Trash
;; DONE Systems switch when opening the email and going back
;;   DONE Create click event (in a more generic way)
;;   DONE Play sound on click
;;   DONE Go to another scene when clicking on the email icon
;;   DONE Re-drain and cleanup
;;     DONE When starting a new scene, dispose old ecs and related automatically
;; TODO Email screen
;;   DONE Create classes on the fly
;;     DONE Entity
;;     LATER Pairs
;;   DONE Make actions happen using a `Action` component instead of the entity
;;     - https://github.com/Leafwing-Studios/leafwing-input-manager/blob/main/examples/ui_driven_actions.rs
;;     - Used just a `Click` component for now
;;   DONE Draw top bar
;;     LATER Defer drawing to the end so we can collected things to be batched
;;   DONE Go back to the main window when clicking on x
;;   DONE Hover for button
;;   DONE Hover for `x`
;;   DONE Text color for `M` and FPS
;;   DONE Add `T` button
;;   DONE Make FPS dynamic
;;   DONE Email subject
;;   TODO Email header hover
;;   TODO Use keyword namespace to create a enum from it
;;   TODO Email body
;;   TODO Hover email
;;   TODO When going back to a scene, make a "constructor"
;; TODO Notifications
;; TODO Create `draw` abstraction so we can "correct" the coordinates
;; TODO Setup builder
;; TODO `defsys`
;; TODO Accept multiple-arity systems
;; TODO Change print/pprint for the components
;; TODO See how to maintain a sorted sequence (for z-ordering)
;; TODO Make it work with morethan 6 components (even if slower)
;; DONE Abstract mouse events (so we don't use `tp`)
;; TODO Audio namespace
;; TODO Only check ui events when needed
;; TODO Creating a new Dominion as a limit, see how to reuse it
;; TODO Declarative way to build an UI
;; TODO Find a way to break out of the loop (using :while like in doseq or for?)

#_ (start-game)

(gen-class
 :name demo.core.Game
 :extends com.badlogic.gdx.Game)

(def screen-width 600)
(def screen-height 600)
(def desired-fps 60)

#_ (start-game)

(defonce env (vg/make-env))

(defonce *exception (atom nil))
(defonce *exceptions (atom []))

(def colors
  (color/palette #_82 133 #_153 #_164 #_@i))

(def colors-2
  (color/palette #_82 #_133 153 #_164 #_@i))

;; ------------ COMPONENTS ----------

(vy/defcomp TextSection
  [:text :string]
  [:scale :float]
  [:fg_color vy.c/Color]
  [:bg_color vy.c/Color])

;; --------------- SETUP ----------------

(defn setup-ecs-common
  [{::vg/keys [world]}]
  (doto world
    vy.c/initialize!

    ;; Prefabs.
    (vy/add-prefab :vy.pf/ui
                   [(vy/is-a :vy.pf/sprite)
                    (vy.c/Position {:x 0 :y 0 :z 0})
                    (vy.c/Size {:width 50 :height 50})
                    (vy.c/Enabled {:enabled false})
                    :vy.t/ui])

    (vy/add-prefab :vy.pf.2d/animation
                   [(vy/is-a :vy.pf/ui)
                    (vy.c/EnvResource {:resource :NOT_SET})])

    (vy/add-prefab :vy.pf/ui-interactive
                   [(vy/is-a :vy.pf/ui)
                    :vy.t/interactive])

    (vy/add-prefab :vy.pf/ui-clickable
                   [(vy/is-a :vy.pf/ui-interactive)
                    (vy.c/Hover)])

    ;; Components.
    (vy/add-many ::id/cursor
                 [(vy/is-a :vy.pf/sprite)
                  (vy.c/Size {:width screen-width
                              :height screen-height})])
    (vy/add-many ::id/fps
                 [(vy/is-a :vy.pf/ui)
                  (vy.c/Position {:x 513 :y 570})
                  (vy.c/Size {:width 10 :height 20})
                  (TextSection {:text ""
                                :scale 10/38
                                :fg_color (get colors 2)
                                :bg_color :white})])

    ;; Phases.
    (vy/add-phase :vy.phase/pre-update :vy.b/EcsPreUpdate)
    (vy/add-phase :vy.phase/correction :vy.b/EcsPreStore)
    ;; Render.
    (vy/add-phase :vy.phase/pre-render :vy.b/EcsOnStore)
    (vy/add-phase :vy.phase/ui-render :vy.phase/pre-render)
    (vy/add-phase :vy.phase/overlay-render :vy.phase/ui-render)))

#_ (start-game)
#_ (vg/close-app)

#_ (vy/log-set 2)
#_ (vy/log-set :all)
#_ (vy/log-set false)

(defn new-world!
  []
  (vy/world-destroy (::vg/world env))
  (let [world (vy/init)
        [old-env _new-env] (swap-vals! env merge {::vg/world world})]
    (setup-ecs-common env)
    world))

#_ (restart)

#_ (start-game)

;; ---------------- TEXT ----------------

(defn text-shadow
  ([env text position]
   (text-shadow env text position {}))
  ([env text {:keys [x y]} {:keys [bg-color fg-color]
                            :or {bg-color :white
                                 fg-color (get colors 2)}}]
   (-> env
       (vg/set-color bg-color)
       (vg/text text [(+ ^float x 2) (+ ^float y 2)])
       (vg/set-color fg-color)
       (vg/text text [x y]))))

#_ (.getClassIndex (.compositions (::vy/ecs env)))

;; ---------------- SYSTEMS ---------------

#_ (do @*exception)

#_ (start-game)

;; ------------- SYSTEM/COMMON -------------

(defonce world-lock (Object.))

(defn -delete
  "To be used in the REPL."
  [v]
  (locking world-lock
    (try
      (when (::vg/world env) (vy/delete (::vg/world env) v))
      (catch Exception _))))

(defn -debug
  "To be used in the REPL."
  ([v]
   (-debug v {}))
  ([v params]
   (locking world-lock
     (vy/query-debug (::vg/world env)
                     (or (:vy/query (meta v))
                         v)
                     params))))

(defn -ei
  "To be used in the REPL."
  [v]
  (locking world-lock
    (vy/entity-info (::vg/world env) v)))

#_ (start-game)
#_ (vg/close-app)

(defn ui-wrapper
  [env _iter f]
  (let [*hover (volatile! false)]
    (try
      (swap! env assoc ::vg/*hover *hover)
      (f)
      (finally
        (swap! env dissoc ::vg/*hover)))))

;; LATER Fix order_by interaction in this system with `draw-ui`
;;   - Each one is invalidating the other sorting
;;   LATER When clicking, the tag triggers a sort as it's adding a new tag
;; LATER Fix order for email hover!! Check the last hovered or start doing layered groups?
(defn check-ui-input
  ;; DONE Maybe use :vy.t/ui instead of :vy.t/interactive
  ;;   - Actually no, it's important that we control this
  {:vy/query [vy.c/Size [vy.c/Position :global] [:maybe [:inout vy.c/Hover]] [:maybe :vy.t/click-down] :vy.t/interactive
              [:query {:order_by_component [vy.c/Position :global]
                       :order_by (jnr/long-ptr-long-ptr-callback
                                  (fn [e1 ^::vy.c/Position pos-1 e2 ^::vy.c/Position pos-2]
                                    #_(println :aaa)
                                    (let [c1 (compare (vy/pget pos-2 :z)
                                                      (vy/pget pos-1 :z))]
                                      (if (zero? c1)
                                        (compare e2 e1)
                                        c1))))}]]
   :vy/wrapper #'ui-wrapper}
  [{::vg/keys [mouse-pos mouse-dragged entity-dragged mouse-state *hover] :as env} iter]
  (vy/with-each iter [entity :vy/entity
                      size vy.c/Size
                      pos vy.c/Position
                      hover vy.c/Hover
                      click-down :vy.t/click-down]
    (if entity-dragged
      (if mouse-dragged
        (vy/emit-event (vy/iter-world iter) entity-dragged :vy.ev/click-down)
        (swap! env assoc ::vg/entity-dragged nil))
      (let [world (vy/iter-world iter)
            {:keys [x y]} (vy/pselect pos)
            {:keys [width height]} (vy/pselect size)
            mouse-over? (and (c2d/contains-point? (c2d/rect-shape x y width height)
                                                  (first mouse-pos) (second mouse-pos))
                             (not @*hover))]

        (when mouse-over? (vreset! *hover true))

        ;; Just capture the event if hoverable.
        (when hover
          (if mouse-over?
            (do
              (when-not (vy/pget hover :hover)
                (vy/emit-event world entity :vy.ev/hover))

              (cond
                (= mouse-state :vg.mouse/down)
                (do (vy/add-c world entity :vy.t/click-down)
                    (vy/emit-event world entity :vy.ev/click-down))

                (and click-down (= mouse-state :vg.mouse/up))
                (do (vy/remove-c world entity :vy.t/click-down)
                    (vy/emit-event world entity :vy.ev/click)))

              (vy/pset hover {:hover true}))
            (do
              (vy/pset hover {:hover false})
              (vy/remove-c world entity :vy.t/click-down))))))))
#_ (-debug #'check-ui-input)

#_(-delete #'check-ui-input)

#_ (start-game)

(defn enable-ui
  ;; Here we are saying check if the component is hovered or if the parent is!
  {:vy/query [[:maybe vy.c/Hover]
              [:maybe [:meta {:flags #{:parent :cascade}}
                       vy.c/Hover]]
              [:inout vy.c/Enabled]
              :vy.t/ui]}
  [_ iter]
  (vy/with-each iter [hover vy.c/Hover
                      hover-parent vy.c/Hover
                      enabled vy.c/Enabled
                      entity :vy/entity]
    (when (or hover hover-parent)
      (if (or (and hover (vy/pget hover))
              (and hover-parent (vy/pget hover-parent)))
        (vy/pset enabled {:enabled true})
        (vy/pset enabled {:enabled false})))))
#_ (-debug #'enable-ui)
#_ (vy/parse-query-expr (::vg/world env) [[:or vy.c/Hover [:meta {:flags #{:parent :cascade}}
                                                           vy.c/Hover]]
                                          [:inout vy.c/Enabled]
                                          :vy.t/ui])

#_ (vg/fps)

#_(-delete #'enable-ui)

#_ (vy/resolve-query env (first (::vy/queries (meta #'check-button-hover))))

#_(.translate (::vy/camera env) +30 0 0)
#_(set! (.zoom (::vy/camera env)) 1.0)

#_ (restart)

;; DONE Remove c-api and the boilerplate to get component data.
(defn draw-cursor
  {:vy/query [vy.c/Size [vy.c/Position :global] ::id/cursor]
   :vy/phase :vy.phase/overlay-render}
  [{::vg/keys [shaders screen-size mouse-pos] :as env} iter]
  (let [[screen-width screen-height] screen-size]
    (vg/with-batch env {:shader (:cursor shaders)}
      (vg/set-uniform env (:cursor shaders) :u_cursor
                      [(/ (- ^float (first mouse-pos) (/ ^long screen-width 2.0)) ^long screen-width)
                       (/ (- (/ ^long screen-height 2.0) ^float (second mouse-pos)) ^long screen-height)])
      (vy/with-each iter [size vy.c/Size
                          pos vy.c/Position]
        (vg/rect env
                 (vy/pselect pos)
                 (vy/pselect size))))))

#_(-delete #'draw-cursor)
#_ (vy/query-debug (::vg/world env) [vy.c/Size [vy.c/Position :global] ::id/cursor])

#_ (start-game)

(defn batch-wrapper
  [{::vg/keys [shaders ^Batch batch] :as env} _iter f]
  (if (do false #_(do true))
    (vg/with-fx env {::vg/shader.frag "shaders/grain.frag"}
      (try
        (.begin batch)
        (vg/set-shader env (:rect shaders))
        (f)
        (finally
          (vg/set-shader env nil)
          (.end batch))))

    (try
      (.begin batch)
      (vg/set-shader env (:rect shaders))
      (f)
      (finally
        (vg/set-shader env nil)
        (.end batch)))))

(defn draw-ui
  {:vy/query [[vy.c/Position :global]
              vy.c/Size
              [:maybe TextSection]
              vy.c/Enabled
              [:maybe vy.c/Color]
              [:maybe vy.c/EnvResource]
              [:maybe :vy.pf.2d/animation]
              [:vy.c/visibility :vy.visibility/inherited]
              :vy.t/ui
              [:query {:order_by_component [vy.c/Position :global]
                       :order_by (jnr/long-ptr-long-ptr-callback
                                  (fn [e1 ^::vy.c/Position pos-1 e2 ^::vy.c/Position pos-2]
                                    #_(println :bbb)
                                    (let [c1 (compare (vy/pget pos-1 :z)
                                                      (vy/pget pos-2 :z))]
                                      (if (zero? c1)
                                        (compare e1 e2)
                                        c1))))}]]
   :vy/phase :vy.phase/ui-render
   :vy/wrapper #'batch-wrapper}
  [{::vg/keys [total-time batch-secondary batch] :as env} iter]
  (vy/with-each iter [pos vy.c/Position
                      size vy.c/Size
                      section TextSection
                      enabled vy.c/Enabled
                      color vy.c/Color
                      res vy.c/EnvResource
                      animation-2d :vy.pf.2d/animation]
    (let [{:keys [x y] :as pos} (vy/pselect pos [:x :y])
          size (vy/pselect size)]
      (cond
        section
        (let [{:keys [scale text fg_color bg_color]} (vy/pselect section)]
          ;; Test for FBO applied to a specific element.
          ;; You will see the frame rate drop if the shaders are being compiled
          ;; everytime.
          #_(let [env (assoc env
                             ::vg/fbo-batch batch
                             ::vg/batch batch-secondary)]
              (.end batch)
              (-> (vg/set-shader env {::vg/shader.frag "shaders/grain.frag"})
                  (vg/set-uniform {::vg/shader.frag "shaders/grain.frag"} "u_time" (* (::vg/total-time env) 0.2)))
              (vg/with-batch env
                (vg/with-fbo env
                  (-> env
                      (assoc ::vg/font-scale scale)
                      (vg/set-custom :a_default 1)
                      (text-shadow text (update pos :y + 4)
                                   (if (vy/pget enabled :enabled)
                                     {:bg-color fg_color
                                      :fg-color bg_color}
                                     {:bg-color bg_color
                                      :fg-color fg_color})))))
              (.begin batch))
          (-> env
              (assoc ::vg/font-scale scale)
              (vg/set-custom :a_default 1)
              (text-shadow text (update pos :y + 4)
                           (if (vy/pget enabled :enabled)
                             {:bg-color fg_color
                              :fg-color bg_color}
                             {:bg-color bg_color
                              :fg-color fg_color}))))

        (and res animation-2d)
        (-> env
            (vg/set-custom :a_default 1)
            (vg/set-color (vg/->color :white))
            (vg/draw-animation (get env (vy/pget res :resource)) {:x (- x 2) :y (- y 2)} size)
            (vg/set-color (vg/->color :gray))
            (vg/draw-animation (get env (vy/pget res :resource)) pos size))

        :else
        (-> env
            (vg/set-color (vy/pselect color))
            (vg/set-custom :a_default 0)
            (vg/set-custom :a_stroke (if (vy/pget enabled :enabled)
                                       2.0
                                       0.01))
            (vg/rect pos size)))

      ;; For debugging!
      #_(-> env
            (vg/set-color (vg/->color :red))
            (vg/set-custom :a_default 0)
            (vg/set-custom :a_stroke 0.005)
            (vg/rect pos size)))))
#_ (-debug #'draw-ui)
#_ (vg/fps)

#_ (start-game)

#_ (let [world (::vg/world env)
         ecs-query (c-api :system_get_query world (vy/->id world #'draw-ui))]
     (c-api :query_get_filter ecs-query)
     (c-api :query_str ecs-query))

#_(-delete #'draw-ui)

#_ (vy/->id (::vg/world env) :vy.c/Position)

#_ (start-game)

(defn clear-screen
  [{::vg/keys [^Batch batch ^Camera camera]}]
  (.update camera)
  (.setProjectionMatrix batch (.combined camera))

  (doto ^GL20 (vg/gl)
    (.glClear GL20/GL_COLOR_BUFFER_BIT)
    (.glEnable GL20/GL_TEXTURE_2D)))

(defn pre-render
  ;; `[:sync :*]` means that we are going to sync on all component changes here
  ;; so we don't have flickering.
  {:vy/query [vy.c/ActiveScene]
   :vy/phase :vy.phase/pre-render}
  [env iter]
  (vg/window-pos 870 50)

  (vy/with-each iter [active-scene vy.c/ActiveScene]
    (vg/clear-color (vy/pget active-scene :bg_color)))

  (clear-screen env))

(defn update-time
  [_ _]
  (swap! env update ::vg/total-time + (::vg/delta-time env)))

(defn update-fps
  {:vy/query [[:out TextSection] ::id/fps]}
  [_ iter]
  (vy/with-each iter [section TextSection]
    (vy/pset section {:text (str "fps: " (vg/fps))})))

(defn update-global-position
  {:vy/query [vy.c/Position
              [:out [vy.c/Position :global]]
              [:maybe {:flags #{:parent :cascade}}
               [vy.c/Position :global]]
              [:maybe :vy.t/absolute]
              [:sync :*]]
   :vy/phase :vy.phase/correction}
  [_ iter]
  ;; TODO It seems change detection officialy only works with instancing, enable
  ;;      it and deal with it inside `with-each`?
  (vy/with-changed iter
    (vy/with-each iter [position-local vy.c/Position
                        position-global vy.c/Position
                        position-parent vy.c/Position
                        absolute :vy.t/absolute]
      (let [{:keys [x y z]} (vy/pselect position-local)]
        (if (and (not absolute) position-parent)
          (vy/pset position-global {:x (+ x (vy/pget position-parent :x))
                                    :y (+ y (vy/pget position-parent :y))
                                    :z (+ z (vy/pget position-parent :z))})
          (vy/pset position-global {:x x :y y :z z}))))))
#_ (-debug #'update-global-position)

(defn debug-entity-observer
  {:vy/query [vy.c/Position
              [vy.c/Position :global]
              [::vg/debug :*]
              :vy.t/debug]
   :vy/events [:vy.ev/click :vy.ev/click-down]}
  [{::vg/keys [mouse-pos]}
   iter]
  (vy/with-for iter [entity :vy/entity
                     pos vy.c/Position
                     global vy.c/Position
                     eita [::vg/debug :*]]
    (swap! env assoc ::vg/entity-dragged entity)
    (let [entity-local-pos (-> ^::vy.c/Position (vy/get-c (vy/iter-world iter) (last eita) vy.c/Position)
                               vy/pselect)
          entity-global-pos (-> ^::vy.c/Position (vy/get-c (vy/iter-world iter) (last eita) [vy.c/Position :global])
                                vy/pselect)]
      (vy/add-many (vy/iter-world iter)
                (last eita)
                [(vy.c/Position (assoc {:x (- (- (first mouse-pos) 5)
                                              (- (:x entity-global-pos)
                                                 (:x entity-local-pos)))
                                        :y (- (- (second mouse-pos) 5)
                                              (- (:y entity-global-pos)
                                                 (:y entity-local-pos)))}
                                       :z (:z entity-local-pos)))
                 {[vy.c/Position :global]
                  (assoc {:x (- (first mouse-pos) 5)
                          :y (- (second mouse-pos) 5)}
                         :z (:z entity-local-pos))}]))))

#_ (start-game)

(defn add-debug-entity
  [iter]
  (let [world (vy/iter-world iter)]
    (vy/with-each iter [entity :vy/entity
                        pos vy.c/Position
                        size vy.c/Size]
      (vy/with-scope world [::vg/debug entity]
        (vy/add-many world (vy/make-entity world {:name (str "vy-debug-" entity)})
                     [:vy.t/debug
                      [::vg/debug entity]
                      :vy.t/absolute
                      (vy/is-a :vy.pf/ui-clickable)
                      (if size
                        (vy.c/Color (color/set-alpha :blue 200))
                        (vy.c/Color (color/set-alpha :red 200)))
                      (if size
                        (vy.c/Position (assoc (vy/pselect pos) :z 10000))
                        (vy.c/Position (assoc (vy/pselect pos) :z 10100)))
                      (vy.c/Size {:width 10 :height 10})])))))

(def debug-query
  [[vy.c/Position :global]
   [:maybe vy.c/Size]
   [:not :vy.t/debug]])

(defn debug-system
  {:vy/query debug-query}
  [_env iter]
  (let [world (vy/iter-world iter)]
    (when (vy/has-id world ::vg/debug :vy.t/active)
      (vy/with-changed iter
        (add-debug-entity iter)))))

#_ (-debug [(vy/child-of ::vg/debug)] {:entity-info? true})
#_ (-debug [[::vg/debug :*]] {:entity-info? true})

#_ (start-game)

#_(vy/query-debug (::vg/world env) [vy.c/Size ::id/fps])
#_(vy/entity-info (::vg/world env) ::id/fps)
#_(vy/entity-info (::vg/world env) ::id/cursor)
#_(vy/query-debug (::vg/world env) [:vy.b/EcsPrefab] {:entity-info? true})
#_(vy/query-debug (::vg/world env) [vy.c/Size] {:entity-info? true})

#_ (restart)

;; ------------------- SYSTEM/EMAIL ----------------------

(declare setup)

#_ (start-game)

(defn show-email-hover-ui
  {:vy/query [vy.c/Enabled :vy.t/email-hover
              ;; `:notify` notifies another system that it's setting a component.
              ;; Check the sync point section at https://www.flecs.dev/flecs/md_docs_Systems.html.
              [:notify [:vy.c/visibility :*]]]}
  [_ iter]
  (vy/with-each iter [entity :vy/entity
                      enabled vy.c/Enabled]
    (let [world (vy/iter-world iter)]
      (if (vy/pget enabled)
        (vy/add-c world entity [:vy.c/visibility :vy.visibility/inherited])
        (vy/add-c world entity [:vy.c/visibility :vy.visibility/hidden])))))
#_ (-debug #'show-email-hover-ui)

#_ (restart)

;; --------------------- OBSERVERS -------------------

(defn play-hover
  {:vy/query [:vy.t/ui]
   :vy/events [:vy.ev/hover]}
  [{:keys [^Sound vg.sound/button-hover]} _iter]
  (.play button-hover 0.05))

(defn play-click
  {:vy/query [:vy.t/ui]
   :vy/events [:vy.ev/click]}
  [{:keys [^Sound vg.sound/button-click]} _iter]
  (.play button-click 0.1))

#_ (start-game)

#_ (c-api :add_id (::vg/world env)
          (vy/->id (::vg/world env) :vy.t/email-hover)
          (vy/->id (::vg/world env) :vy.b/EcsTag))

#_(vy/entity-info (::vg/world env) :vy.t/email-hover)

#_ (start-game)

(defn switch-scene
  [world scene]
  (vy/add-c world vy.c/ActiveScene [vy.c/ActiveScene scene]))

(defn handle-email-button
  {:vy/query [::id/email-button]
   :vy/events [:vy.ev/click]}
  [_env iter]
  (switch-scene (vy/iter-world iter) :vy.scene/email))

(defn handle-email-close-button
  {:vy/query [::id/email-close-button]
   :vy/events [:vy.ev/click]}
  [_env iter]
  (switch-scene (vy/iter-world iter) :vy.scene/default))

(defn handle-email-click
  {:vy/query [[:vy.c/email :*]]
   :vy/events [:vy.ev/click]}
  [_env iter]
  (vy/with-each iter [entity :vy/entity
                      email-id [:vy.c/email :*]]
    (vy/add-many (vy/iter-world iter) vy.c/ActiveScene [email-id [:vy.email/opened entity]])
    (switch-scene (vy/iter-world iter) :vy.scene/email-details)))

#_ (start-game) #_ (deref vy/*env)
#_ (vy/get-world (::vg/world env))

(defn -scn
  "Helper for the REPL."
  [scene]
  (when (::vg/world env)
    (locking world-lock
      (vy/remove-c (::vg/world env) vy.c/ActiveScene [vy.c/ActiveScene scene])
      (vy/add-c (::vg/world env) vy.c/ActiveScene [vy.c/ActiveScene scene]))))

(defn default-scene-setup
  {:vy/query [[vy.c/ActiveScene :vy.scene/default]]
   :vy/events [:on-add]}
  [_env iter]
  (let [world (vy/iter-world iter)]
    (vy/delete-children world vy.c/ActiveScene)
    (vy/add-c world vy.c/ActiveScene (vy.c/ActiveScene {:bg_color (get colors 4)}))
    (vy/with-scope world vy.c/ActiveScene
      ;; 30000 ~=  5 FPS
      ;; 3000  ~= 37 FPS
      (let [[entities-count enabled] [5 (do false) #_(do true)]
            #_[3000 true]
            button (fn [id [^long x ^long y ^long z] text]
                     (vy/with-scope world (vy/add-c world (vy.c/Position {:x x :y y :z z}))
                       (doto world
                         (vy/add-many [(vy/is-a :vy.pf/ui)
                                       (vy.c/Position {:x 2 :y 2 :z -2})
                                       (vy.c/Size {:width 104 :height 104})
                                       (vy.c/Color (get colors 2))])
                         (vy/add-many [(vy/is-a :vy.pf/ui-clickable)
                                       (vy.c/Position {:z -1})
                                       (vy.c/Size {:width 104 :height 104})
                                       id])
                         (vy/add-many [(vy/is-a :vy.pf/ui)
                                       (vy.c/Position {:x 35 :y 35})
                                       (TextSection {:text text
                                                     :scale 1.0
                                                     :fg_color (get colors 2)
                                                     :bg_color :white})]))))]

        (button ::id/email-button [50 100 1] "m")
        (button ::id/trash-button [220 100 1] "t")

        (when enabled
          (doseq [_ (range entities-count)]
            (button nil [(+ 30 ^int (rand-int 400))
                         (+ 30 ^int (rand-int 400))
                         (rand 20)]
                    "t")
            #_(-> env
                  (vy/add-entities
                   (concat
                    (button ::id/email-button [50 100 1] "m")
                    (button ::id/trash-button [220 100 1] "t")))
                  #_(vy/add-e [(ui-interactive-prefab
                                {:transform {:translation [220 100 3]}
                                 :sprite {:size [104 104]}})
                               ::t/button
                               (vy/map->Text {:text "t"})]))

            #_ (restart)

            ;; 12 fps for 10000
            #_(if enabled
                (range entities-count)
                (range 50))))))))

#_ (start-game)

#_ (-debug [[vy.c/Position :global]] {:entity-info? true})

(defn email-scene-setup
  {:vy/query [[vy.c/ActiveScene :vy.scene/email]]
   :vy/events [:on-add]}
  [_env iter]
  (let [world (vy/iter-world iter)
        _ (vy/add-c world vy.c/ActiveScene (vy.c/ActiveScene {:bg_color (get colors 0)}))
        _ (vy/delete-children world vy.c/ActiveScene)
        email-header (fn [[id email subject] [x y]]

                       #_ (start-game)

                       (vy/with-scope world (vy/add-many world [(vy.c/Position {:x x :y y})
                                                                ::id/email-header])
                         (let [fg (vy/add-many world
                                               [(vy/is-a :vy.pf/ui-clickable)
                                                (vy.c/Position {:x -20 :y -15 :z -1})
                                                (vy.c/Size {:width 500 :height 70})
                                                (vy.c/Color (get colors 3))
                                                [:vy.c/visibility :vy.visibility/hidden]
                                                :vy.t/email-hover
                                                [:vy.c/email id]])]
                           (vy/with-scope world fg
                             (vy/add-many world
                                          [(vy/is-a :vy.pf/ui)
                                           (vy.c/Position {:x 3 :y 3 :z -1})
                                           (vy.c/Size {:width 500 :height 70})
                                           (vy.c/Color :gray)
                                           [:vy.c/visibility :vy.visibility/hidden]
                                           :vy.t/email-hover])))

                         (doto world
                           (vy/add-many [(vy/is-a :vy.pf/ui)
                                         (vy.c/Position {:z 1})
                                         (vy.c/Size {:width 15 :height 20})
                                         (TextSection {:text email
                                                       :scale 9/38
                                                       :fg_color :yellow
                                                       :bg_color :gray})])
                           (vy/add-many [(vy/is-a :vy.pf/ui)
                                         (vy.c/Position {:y 25 :z 1})
                                         (vy.c/Size {:width 15 :height 20})
                                         (TextSection {:text subject
                                                       :scale 16/38
                                                       :fg_color :white
                                                       :bg_color (get colors 2)})]))))]

    (vy/with-scope world vy.c/ActiveScene

      (let [list-parent (vy/add-c world (vy.c/Count {:count 0}))
            query (vy/query world [vy.c/Position ::id/email-header])]
        (def list-parent list-parent)
        (vy/with-scope world list-parent
          (mapv (fn [idx]
                  (vy/add-many world [(vy.c/Size {:width idx :height 200})]))
                (range 1)))

        (vy/add-observer world []
                         (fn [iter]
                           (let [world (vy/iter-world iter)]
                             (when (vy/iter-event? iter :vy.b/EcsOnSet)
                               (vy/with-each iter [entity :vy/entity]
                                 (let [iter-q (c-api :vybe_query_iter world query)]
                                   (while (c-api :query_next iter-q)
                                     (vy/with-each iter-q [pos vy.c/Position]
                                       (vy/pupdate pos :y + 90))))

                                 (vy/with-scope world vy.c/ActiveScene
                                   (email-header [::id/email.hey "pito@aguafria.com" "hey!"]
                                                 [70 80]))))))
                         {:vy/query [vy.c/Size
                                     (vy/child-of list-parent)]
                          :vy/events [:on-set]})

        (vy/add-observer world []
                         (fn [iter]
                           (let [world (vy/iter-world iter)]
                             (when (vy/iter-event? iter :vy.b/EcsOnRemove)
                               (vy/with-each iter [entity :vy/entity
                                                   header-pos vy.c/Position]

                                 (let [iter-q (c-api :vybe_query_iter world query)]
                                   (while (c-api :query_next iter-q)
                                     (vy/with-each iter-q [pos vy.c/Position]
                                       (when (> (vy/pget pos :y)
                                                (vy/pget header-pos :y))
                                         (vy/pupdate pos :y - 90)))))))))
                         {:vy/query [vy.c/Position
                                     (vy/child-of vy.c/ActiveScene)
                                     ::id/email-header]
                          :vy/events [:on-remove]}))

      #_ (locking world-lock
           (let [world (::vg/world env)]
             (vy/with-scope world list-parent
               (vy/add-many world
                            [(vy.c/Size {:width 45 :height 200})]))))
      #_ (vg/close-app)
      #_ (start-game)

      #_(email-header ::id/email.cant-login "rich@compay.com" "member can't login" [70 80])
      #_(email-header ::id/email.hey "pito@aguafria.com" "hey!" [70 170])

      (doto world
        (vy/add-many [(vy/is-a :vy.pf/ui-clickable)
                      (vy.c/Position {:x 575 :y 6 :z 1})
                      (vy.c/Size {:width 15 :height 20})
                      (TextSection {:text "x"
                                    :scale 12/38
                                    :fg_color :black
                                    :bg_color :white})
                      ::id/email-close-button])

        ;; Top bar.
        (vy/add-many [(vy/is-a :vy.pf/ui)
                      (vy.c/Size {:width screen-width :height 30})
                      (vy.c/Color (get colors 2))
                      (vy.c/Enabled {:enabled true})])
        (vy/add-many [(vy/is-a :vy.pf/ui)
                      (vy.c/Position {:y 3 :z -1})
                      (vy.c/Size {:width screen-width :height 30})
                      (vy.c/Color :gray)
                      (vy.c/Enabled {:enabled true})])))))

(defn email-details-scene-setup
  {:vy/query [[:vy.c/email :*]
              [vy.c/ActiveScene :vy.scene/email-details]]
   :vy/events [:on-add]}
  [_env iter]
  (let [world (vy/iter-world iter)
        email (vy/with-first iter [email [:vy.c/email :*]] email)]
    (vy/with-scope world vy.c/ActiveScene
      (vy/delete-children world ::email-details)
      (vy/with-scope world (vy/add-c world ::email-details (vy.c/Position {:x 100 :y 100 :z 10}))
        (doto world
          (vy/add-many [(vy/is-a :vy.pf/ui-interactive)
                        (vy.c/Position {:x -2 :y -2 :z -2})
                        (vy.c/Size {:width 415 :height 445})
                        (vy.c/Color :gray)
                        (vy.c/Enabled {:enabled true})])
          (vy/add-many [(vy/is-a :vy.pf/ui-interactive)
                        (vy.c/Position {:z -1})
                        (vy.c/Size {:width 400 :height 430})
                        (vy.c/Color (get colors 4))
                        (vy.c/Enabled {:enabled true})])

          (vy/add-many [(vy/is-a :vy.pf/ui)
                        (vy.c/Position {:x 20 :y 20})
                        (TextSection {:text "hey!"
                                      :scale 15/38
                                      :bg_color :white
                                      :fg_color :gray})])
          (vy/add-many [(vy/is-a :vy.pf/ui)
                        (vy.c/Position {:x 20 :y 70})
                        (TextSection {:text (->> ["hi!"
                                                  ""
                                                  "welcome to agua fria, we are so"
                                                  "happy to have you around."
                                                  ""
                                                  "before anything, let's start our"
                                                  "onboarding."
                                                  ""
                                                  "are you ready?"]
                                                 (str/join "\n"))
                                      :scale 10/38
                                      :bg_color :gray
                                      :fg_color :white})])

          #_(vy/add-many [(vy/is-a :vy.pf.2d/animation)
                          (vy.c/Position {:x 20 :y 240 :z 1})
                          (vy.c/Size {:width 64 :height 64})
                          (vy.c/EnvResource {:resource ::vg/anim})]))

        (let [id (vy/add-many world
                              [(vy/is-a :vy.pf/ui-clickable)
                               (vy.c/Position {:x 385 :y 8 :z 1})
                               (vy.c/Size {:width 15 :height 20})
                               (TextSection {:text "x"
                                             :scale 10/38
                                             :bg_color :white
                                             :fg_color :gray})])]
          (vy/add-event-handler world id [:vy.ev/click]
                                (fn [_iter]
                                  (vy/add-c world vy.c/ActiveScene [:vy.c/email :none])
                                  (vy/delete-children world ::email-details))))

        (let [id (vy/add-many world
                              [(vy/is-a :vy.pf/ui-clickable)
                               (vy.c/Position {:x 20 :y 400 :z 1})
                               (vy.c/Size {:width 50 :height 20})
                               (TextSection {:text "reply"
                                             :scale 10/38
                                             :bg_color :gray
                                             :fg_color :yellow})])]
          (vy/add-event-handler
           world id [:vy.ev/click]
           (fn [_iter]
             #_ (vy/emit-event world entity :vy.ev/click)
             (vy/with-scope world ::email-details
               (vy/with-scope world (vy/add-c world (vy.c/Position {:x 20 :y 280 :z 10}))
                 (doto world
                   #_(vy/add-many [(vy/is-a :vy.pf/ui)
                                   (vy.c/Position {:x 2 :y 2 :z -2})
                                   (vy.c/Size {:width 350 :height 100})
                                   (vy.c/Color (color/set-alpha :gray 170))
                                   (vy.c/Enabled {:enabled true})])
                   (vy/add-many [(vy/is-a :vy.pf/ui-interactive)
                                 (vy.c/Position {:z -1})
                                 (vy.c/Size {:width 350 :height 100})
                                 (vy.c/Color (color/set-alpha (get colors-2 4) 220))
                                 (vy.c/Enabled {:enabled true})])

                   (vy/add-many [(vy/is-a :vy.pf/ui-clickable)
                                 (vy.c/Position {:x 20 :y 20})
                                 (vy.c/Size {:width 70 :height 20})
                                 (TextSection {:text "yes"
                                               :scale 8/38
                                               :bg_color :white
                                               :fg_color (get colors-2 2)})]))

                 (let [id (vy/add-many world [(vy/is-a :vy.pf/ui-clickable)
                                              (vy.c/Position {:x 20 :y 40})
                                              (vy.c/Size {:width 70 :height 20})
                                              (TextSection {:text "no"
                                                            :scale 8/38
                                                            :bg_color :white
                                                            :fg_color (get colors-2 2)})])]
                   (vy/add-event-handler world id [:vy.ev/click]
                                         (fn [_iter]
                                           (vy/with-scope world ::email-details
                                             (vy/add-many world
                                                          [(vy/is-a :vy.pf/ui)
                                                           :vy.t/absolute
                                                           (vy.c/Position {:z 28})
                                                           (vy.c/Size {:width screen-width :height screen-height})
                                                           (vy.c/Color (color/set-alpha (get colors-2 1) 160))
                                                           (vy.c/Enabled {:enabled true})])

                                             (vy/with-scope world
                                               (vy/add-many world
                                                            [(vy/is-a :vy.pf/ui-interactive)
                                                             :vy.t/absolute
                                                             (vy.c/Position {:x (+ 50 3) :y (+ 50 3) :z 29})
                                                             (vy.c/Size {:width 500 :height 100})
                                                             (vy.c/Color (get colors-2 0))
                                                             (vy.c/Enabled {:enabled true})])
                                               (vy/add-many world
                                                            [(vy/is-a :vy.pf/ui)
                                                             (vy.c/Position {:x 20 :y 20 :z 31})
                                                             (TextSection {:text "hunnn, not right now"
                                                                           :scale 12/38
                                                                           :bg_color (get colors-2 2)
                                                                           :fg_color :white})]))
                                             (vy/add-many world
                                                          [(vy/is-a :vy.pf/ui-interactive)
                                                           :vy.t/absolute
                                                           (vy.c/Position {:x 50 :y 50 :z 30})
                                                           (vy.c/Size {:width 500 :height 100})
                                                           (vy.c/Color (get colors-2 1))
                                                           (vy.c/Enabled {:enabled true})]))))))))))

        #_(let [id (vy/add-many world
                                [(vy/is-a :vy.pf/ui-clickable)
                                 (vy.c/Position {:x 385 :y 44 :z 1})
                                 (vy.c/Size {:width 15 :height 20})
                                 (TextSection {:text "t"
                                               :scale 10/38
                                               :bg_color :black
                                               :fg_color :yellow})])]
            (vy/add-event-handler world id [:vy.ev/click]
                                  (fn [_iter]
                                    (vy/add-c world vy.c/ActiveScene [:vy.c/email :none])
                                    (vy/delete world (c-api :get_parent world (c-api :get_target world
                                                                                     (vy/->id world vy.c/ActiveScene)
                                                                                     (vy/->id world :vy.email/opened)
                                                                                     0)))
                                    (vy/delete-children world ::email-details))))))))
#_ (vy/entity-info (::vg/world env) vy.c/ActiveScene)
#_ (-debug [:vy.b/EcsObserver] {:entity-info? true})
#_ (when (::vg/world env)
     (do
       (vy/delete-children (::vg/world env) ::email-details)
       (-scn :vy.scene/email-details)))

#_ (start-game)
#_ (vg/close-app)

;; ---------------------- RENDER AND SETUP --------------------

(defn render-game
  [{::vg/keys [world anim game] :as env}]
  (locking world-lock
    (when (vy/world-exists? world)

      #_(println "\n\n FRAME")
      (vy/progress world (::vg/delta-time env))
      #_(println "\n\n"))))

#_ (start-game)

(defn restart
  []
  (locking world-lock
    (new-world!)
    (swap! env merge
           {::vg/scene nil})
    (setup env)))

#_ (restart)

#_ (start-game)
#_ (vg/close-app)

#_ (vy/alive? (::vg/world env) ::xxx.scene)
#_ (vy/pair? (::vg/world env) -9223370821379030472)

(defn setup
  [{::vg/keys [world] :as env}]
  (swap! env merge
         {:vg.sound/button-hover (audio/sound "sounds/mixkit-cool-interface-click-tone-2568.wav")
          :vg.sound/button-click (audio/sound "sounds/mixkit-camera-shutter-click-1133.wav")})

  ;; We preregister some components/tags so they aren't owned by the `with-scope`
  ;; below.
  (->> [[:vy.b/EcsChildOf vy.c/ActiveScene]
        :vy.t/email-hover
        :vy.t/interactive
        :vy.t/absolute
        ::id/email-button
        ::id/trash-button
        ::id/email-close-button
        ::id/email.cant-login
        ::id/email.hey
        ::id/email-header
        :vy.t/debug
        :vy.scene/email
        ::vg/debug
        :vy.c/email
        :vy.email/opened
        [:vy.c/email :*]]
       (mapv (partial vy/->id world)))

  (vy/add-c world :vy.c/email :vy.b/EcsExclusive)
  (vy/add-c world :vy.email/opened :vy.b/EcsExclusive)

  #_ (-debug [[:vy.b/EcsChildOf vy.c/ActiveScene]] {:entity-info? true})

  (->> [#'play-hover
        #'play-click
        #'default-scene-setup
        #'email-scene-setup
        #'email-details-scene-setup
        #'handle-email-button
        #'handle-email-close-button
        #'handle-email-click

        #'debug-entity-observer]
       (vy/add-observers world [env]))

  ;; Trigger default scene observer.
  (switch-scene world :vy.scene/default)

  ;; Add systems.
  (->> [#'update-global-position
        #'debug-system
        #'update-time
        #'update-fps
        #'check-ui-input
        #'enable-ui
        #'pre-render
        #'draw-ui
        #'draw-cursor

        #'show-email-hover-ui
        #'vg/dev-system]
       (vy/add-systems world [env])))
#_ (restart)

#_ (start-game)

#_ (vy/entity-info (::vg/world env) ::id/email-button)

(def main-screen
  (let [stage (atom nil)]
    (proxy [Screen] []
      (show []
        #_(reset! stage (Stage.))
        #_(let [style (Label$LabelStyle. (BitmapFont.) (Color. 1 1 1 1))
                label (Label. "Hesllo world!" style)]
            (.addActor @stage label)))
      (render [delta]
        (try

          (swap! env assoc ::vg/delta-time delta)
          (render-game env)

          (catch Exception e
            (println :!!!!<<>><>EXCEPTION<>>>>>)
            (reset! *exception e)
            (swap! *exceptions conj e)
            (vg/clear-color [255.0 0.0 0.0 255.0])
            (doto ^GL20 (vg/gl)
              (.glClear GL20/GL_COLOR_BUFFER_BIT))
            (println e))))
      (dispose[])
      (hide [])
      (pause [])
      (resize [w h])
      (resume []))))

(defn key-up
  [{::vg/keys [world]} key-code]
  (cond
    (= key-code Input$Keys/E)
    (if (vy/has-id world ::vg/debug :vy.t/active)
      (do (vy/delete-with world :vy.t/debug)
          (vy/delete world ::vg/debug))
      (vy/with-scope world (vy/add-c world ::vg/debug :vy.t/active)
        (vy/add-observer world []
                         (fn [iter]
                           (try
                             (let [world (vy/iter-world iter)]
                               (if (vy/iter-event? iter :vy.b/EcsOnAdd)
                                 (add-debug-entity iter)
                                 (vy/with-each iter [entity :vy/entity
                                                     pos vy.c/Position]
                                   (vy/delete world [::vg/debug entity]))))
                             (catch Exception e
                               (println e))))
                         {:vy/query debug-query
                          :vy/events [:on-remove :on-add]
                          :vy/name "enable-debug-observer"
                          :vy.observer/yield-existing true})))

    (= key-code Input$Keys/D)
    (when (vy/has-id world ::vg/debug :vy.t/active)
      (defonce p (p/open))
      (add-tap #'p/submit)
      (->> (vy/query-debug world [:vy.t/debug])
           (mapv (comp :id :vy/entity))
           (filter #(-> (vy/get-c world % vy.c/Hover [:hover])
                        :hover))
           (mapv #(let [id (c-api :get_target world % (vy/->id world ::vg/debug) 0)]
                    (tap> (with-meta (vy/entity-info world id)
                            {:portal.viewer/default :portal.viewer/pprint})))))))
  true)

#_ (start-game)

#_ (-debug [(vy/child-of ::vg/debug)] {:entity-info? true})
#_ (-debug [[::vg/debug :*]] {:entity-info? true})

(defn start-game
  []
  (vg/close-app)
  (Thread/sleep 500)
  (reset! vg/*shaders-cache {})
  (reset! env {})
  (let [world (new-world!)
        game (proxy [Game] []
               (create []
                 (try
                   (let [^Camera camera (OrthographicCamera. screen-width screen-height)
                         pico-8-mono-font (vg/font this
                                                   "fonts/pico8/pico-8-mono-reversed.ttf"
                                                   {:size 48
                                                    ;; For multi-line text.
                                                    :spaceY 50})
                         one-pixel-tex (vg/texture this "images/one-pixel.jpeg")
                         abc-tex (vg/texture this "images/dog.png")
                         anim (vg/animation this "images/dog.png" "images/dog.json")
                         batch (vg/batch this)
                         batch-secondary (vg/batch this)
                         fbo-batch (vg/batch this)
                         fbo (vg/fbo this)]
                     (.setScreen ^Game this main-screen)
                     (.setSystemCursor (Gdx/graphics) Cursor$SystemCursor/None)

                     (doto camera
                       (.. position (set (/ (.viewportWidth ^Camera camera) 2.0)
                                         (/ (.viewportHeight ^Camera camera) 2.0)
                                         0))
                       .update)

                     (let [tp ^Vector3 (Vector3.)
                           handle-mouse (fn handle-mouse
                                          ([x y]
                                           (handle-mouse x y nil nil))
                                          ([x y pointer]
                                           (handle-mouse x y pointer nil))
                                          ([x y pointer {:keys [button dragged]}]
                                           (.unproject ^Camera camera (.set tp x (- ^long screen-height ^int y) 0))
                                           #_(swap! env assoc ::vg/mouse-pos-raw [(.x tp) (.y tp)])
                                           (swap! env assoc
                                                  ::vg/mouse-pos [(.x tp) (.y tp)]
                                                  ::vg/mouse-dragged dragged)))]
                       (.setInputProcessor
                        (Gdx/input)
                        (proxy [InputAdapter] []
                          (mouseMoved [x y]
                            (handle-mouse x y)
                            true)

                          (touchDown [x y pointer button]
                            (handle-mouse x y pointer {:button button})
                            (swap! env assoc ::vg/mouse-state :vg.mouse/down)
                            true)

                          (touchUp [x y pointer button]
                            (handle-mouse x y pointer {:button button})
                            (swap! env assoc ::vg/mouse-state :vg.mouse/up)
                            true)

                          (touchDragged [x y pointer]
                            (handle-mouse x y pointer {:dragged true})
                            true)

                          (keyUp [key-code]
                            (key-up env key-code)))))

                     (swap! env merge
                            {::vg/game this
                             ::vg/resources-watcher (vg/dev-resources-watcher world)

                             ::vg/total-time 0M
                             ::vg/delta-time 0
                             #_ #_::vg/mouse-pos-raw [-1 -1]
                             ::vg/mouse-pos [-1 -1]
                             ::vg/camera camera
                             ::vg/anim anim
                             ::vg/screen-size [screen-width screen-height]

                             ;; These need to be disposed (also audio files).
                             ::vg/batch batch
                             ::vg/batch-secondary batch-secondary
                             ::vg/fbo-batch fbo-batch
                             ::vg/fbo fbo
                             ::vg/one-pixel-tex one-pixel-tex
                             ::vg/abc-tex abc-tex
                             ::vg/font pico-8-mono-font
                             ;; Shader programs are okay as we have a cache for it.
                             ::vg/shaders {:cursor {::vg/shader.frag "shaders/cursor.frag"}
                                           :rect {::vg/shader.frag "shaders/rect.frag"}
                                           :default {}}})
                     (setup env))
                   (catch Exception e
                     (println e)
                     (throw e)))

                 #_ (start-game))

               (dispose []
                 (vg/dispose-resources this)
                 (beholder/stop @(::vg/resources-watcher env)))

               #_(resize [width height]
                   (.update viewport width height true)))]
    (future
      (def -application
        (Lwjgl3Application.
         game
         (doto (Lwjgl3ApplicationConfiguration.)
           (.setTitle "Oh my")
           (.setWindowedMode screen-width screen-height)
           (.useVsync true)
           (.setForegroundFPS desired-fps)))))))

#_ (start-game)
#_ (vg/close-app)

#_ (do @*exceptions)

(comment

  #_(.set org.lwjgl.system.Configuration/GLFW_LIBRARY_NAME "glfw_async")
  #_(.set org.lwjgl.system.Configuration/GLFW_CHECK_THREAD0 true)
  #_(bean com.badlogic.gdx.Game)

  (start-game)

  ())
