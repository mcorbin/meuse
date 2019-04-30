(ns meuse.api.crate.download
  (:require [meuse.crate-file :as crate-file]
            [meuse.api.crate.http :refer (crates-api!)]
            [clojure.java.io :as io]))

(defmethod crates-api! :download
  [request]
  (let [{:keys [crate-name crate-version]} (:route-params request)]
    {:status 200
     :body (-> (crate-file/crate-file-path
                (get-in request [:config :crate :path])
                crate-name
                crate-version)
               io/file)}))
