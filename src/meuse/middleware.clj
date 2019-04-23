(ns meuse.middleware
  (:require [manifold.deferred :as d]
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
