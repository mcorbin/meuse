(ns meuse.request
  "Utility functions for requests."
  (:require [byte-streams :as bs]
            [cheshire.core :as json]
            [clojure.tools.logging :refer [debug info error]]))

(defn convert-body-edn
  "Takes a request, tries to convert the body in edn."
  [request]
  (if (:body request)
    (try
      (update request :body
              (fn [body]
                (-> (bs/convert body String)
                    (json/parse-string true))))
      (catch Exception e
        (error e "fail to convert the request body to json")
        (throw (ex-info "fail to convert the request body to json"
                        {:type :meuse.error/incorrect}))))
    request))
