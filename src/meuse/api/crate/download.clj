(ns meuse.api.crate.download
  (:require [meuse.crate-file :as crate-file]
            [meuse.api.crate.http :refer (crates-api!)]
            [meuse.api.params :as params]
            [clojure.tools.logging :refer [debug info error]]
            [clojure.java.io :as io])
  (:import java.io.File))

(defmethod crates-api! :download
  [request]
  (params/validate-params request ::api)
  (let [{:keys [crate-name crate-version]} (:route-params request)
        path (crate-file/crate-file-path
              (get-in request [:config :crate :path])
              crate-name
              crate-version)
        file (io/file path)]
    (when-not (.exists file)
      (throw (ex-info (format "the file %s does not exist" path)
                      {})))
    (when (.isDirectory file)
      (throw (ex-info (format "the file %s is a directory" path)
                      {})))
    (debug "serving crate file" path)
    {:status 200
     :body file}))
