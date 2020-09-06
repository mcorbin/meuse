(ns meuse.interceptor.json
  (:require [cheshire.core :as json]))

(def json
  {:name ::json
   :leave (fn [ctx]
            (if (coll? (get-in ctx [:response :body]))
              (-> (update-in ctx [:response :body] json/generate-string)
                  (update-in [:response :headers] assoc "content-type"
                             "application/json"))
              ctx))})
