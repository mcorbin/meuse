(ns meuse.api.params
  (:require [meuse.error :as error]
            [clojure.spec.alpha :as s]))

(defn validate-params
  "Takes a request and a spec, and validates the parameters."
  [request spec]
  (when-not (s/valid? spec request)
    (throw (ex-info (error/explain->message
                     ;; remove the db from the req to avoid json issue
                     (s/explain-data spec (dissoc request :database)))
                    {:explain-str (s/explain-str spec request)
                     :status 400})))
  true)
