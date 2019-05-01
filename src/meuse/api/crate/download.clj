(ns meuse.api.crate.download
  (:require [meuse.crate-file :as crate-file]
            [meuse.api.crate.http :refer (crates-api!)]
            [clojure.tools.logging :refer [debug info error]]
            [clojure.java.io :as io]))

(defmethod crates-api! :download
  [request]
  (let [{:keys [crate-name crate-version]} (:route-params request)
        path (crate-file/crate-file-path
              (get-in request [:config :crate :path])
              crate-name
              crate-version)]
    (debug "serving crate file" path)
    {:status 200
     :body (io/file path)}))
