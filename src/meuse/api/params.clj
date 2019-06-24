(ns meuse.api.params
  (:require [meuse.error :as error]
            [clojure.spec.alpha :as s]))

(defn validate-params
  "Takes a request and a spec, and validates the parameters."
  [request spec]
  (when-not (s/valid? spec request)
    (throw (ex-info (error/explain->message
                     (s/explain-data spec request))
                    {:explain-str (s/explain-str spec request)
                     :status 400})))
  true)
