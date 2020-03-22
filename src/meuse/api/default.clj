(ns meuse.api.default
  (:require [meuse.log :as log]
            [meuse.metric :as metric]))

(defn not-found
  [request]
  (log/info (log/req-ctx request)
            "uri not found:" (:request-method request) (:uri request))
  (metric/http-errors request 404)
  {:status 404
   :body {:errors
          [{:detail "not found"}]}})
