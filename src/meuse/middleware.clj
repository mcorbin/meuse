(ns meuse.middleware
  "HTTP middlewares"
  (:require [clojure.tools.logging :refer [debug info error]]
            [manifold.deferred :as d]
            [meuse.auth.header :as h]
            [meuse.auth.token :as auth-token]
            [meuse.db.token :as token-db]
            [cheshire.core :as json]))

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
