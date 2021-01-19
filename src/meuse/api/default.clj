(ns meuse.api.default
  (:require [meuse.log :as log]))

(defn not-found
  [request]
  (log/info (log/req-ctx request)
            "uri not found:" (:request-method request) (:uri request))
  {:status 404
   :body {:errors
          [{:detail "not found"}]}})
