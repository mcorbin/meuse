(ns meuse.api.crate.download
  (:require [meuse.api.crate.http :refer (crates-api!)]
            [meuse.api.params :as params]
            [meuse.auth.request :as auth-request]
            [meuse.crate-file :as crate-file]
            [clojure.java.io :as io]
            [clojure.tools.logging :refer [debug info error]])
  (:import java.io.File))

(defmethod crates-api! :download
  [request]
  (params/validate-params request ::download)
  (auth-request/admin-or-tech?-throw request)
  (let [{:keys [crate-name crate-version]} (:route-params request)
        path (crate-file/crate-file-path
              (get-in request [:config :crate :path])
              crate-name
              crate-version)
        file (io/file path)]
    (when-not (.exists file)
      (throw (ex-info (format "the file %s does not exist" path)
                      {:status 400})))
    (when (.isDirectory file)
      (throw (ex-info (format "the file %s is a directory" path)
                      {:status 400})))
    (info "serving crate file" path)
    {:status 200
     :body file}))
