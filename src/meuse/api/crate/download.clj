(ns meuse.api.crate.download
  (:require [meuse.api.crate.http :refer (crates-api!)]
            [meuse.api.params :as params]
            [meuse.auth.request :as auth-request]
            [meuse.store.protocol :as store]
            [clojure.java.io :as io]
            [clojure.tools.logging :refer [debug info error]])
  (:import java.io.File))

(defn download
  [crate-file-store request]
  (params/validate-params request ::download)
  (auth-request/admin-or-tech?-throw request)
  (let [{:keys [crate-name crate-version]} (:route-params request)]
    (info (format "serving crate file for crate %s version %s"
                  crate-name
                  crate-version))
    {:status 200
     :body (store/get-file crate-file-store
                           crate-name
                           crate-version)}))
