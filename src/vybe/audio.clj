(ns vybe.audio
  (:import
   (com.badlogic.gdx Gdx Audio)))

(defn sound
  [res-path]
  (.newSound ^Audio (Gdx/audio) (.classpath Gdx/files res-path)))
