(ns meuse.api.crate.download
  (:require [meuse.api.params :as params]
            [meuse.auth.request :as auth-request]
            [meuse.db.public.crate-version :as public-crate-version]
            [meuse.store.protocol :as store]
            [clojure.java.io :as io]
            [clojure.tools.logging :refer [debug info error]])
  (:import java.io.File))

(defn download
  [crate-version-db crate-file-store request]
  (params/validate-params request ::download)
  ;(auth-request/admin-or-tech?-throw request)
  (let [{:keys [crate-name crate-version]} (:route-params request)
        bin-file (store/get-file crate-file-store
                                 crate-name
                                 crate-version)]
    (info (format "serving crate file for crate %s version %s"
                  crate-name
                  crate-version))
    (public-crate-version/inc-download crate-version-db crate-name crate-version)
    {:status 200
     :body bin-file}))
