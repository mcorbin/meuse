(ns meuse.middleware
  "HTTP middlewares"
  (:require [meuse.auth.header :as h]
            [meuse.auth.token :as auth-token]
            [meuse.db.token :as token-db]
            [cheshire.core :as json]
            [manifold.deferred :as d]
            [clojure.tools.logging :refer [debug info error]]))

(defn wrap-json
  "converts the response body into json and set the content type as
  `application/json`"
  [handler]
  (fn [request]
    (d/chain
     (handler request)
     (fn [response]
       (if (coll? (:body response))
         (-> (update response :body json/generate-string)
             (update :headers assoc :content-type "application/json"))
         response)))))
