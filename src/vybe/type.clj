(ns vybe.type
  (:require
   [vybe.jnr :as jnr]
   [malli.core :as m]
   [malli.error :as me]))

(defprotocol IVybeName
  (-vybe-name [_]))

(defprotocol IResolveComponent
  (-resolve-component [_ world id p]))

(defprotocol IComponentData
  (-component-data [struct-type]
    "Get component data from a JNR struct type."))

(defrecord VybeComponent [struct struct-schema fields field->idx comp-sym adapter]
  clojure.lang.IFn
  (invoke [_]
    (jnr/make-instance struct))
  (invoke [_ m]
    (try
      (jnr/make-instance struct (adapter m))
      (catch Exception e
        (when-let [errors (-> (m/explain struct-schema m)
                              me/humanize)]
          (throw (ex-info "Schema error when creating comp instance"
                          {:comp comp-sym
                           :params m
                           :errors errors})))
        (throw e))))
  ;; For pairs.
  (invoke [this target data]
    {[this target] data}))
