(ns meuse.api.crate.download
  (:require [meuse.api.params :as params]
            [meuse.db.public.crate-version :as public-crate-version]
            [meuse.log :as log]
            [meuse.store.protocol :as store]))

(defn download
  [crate-version-db crate-file-store request]
  (params/validate-params request ::download)
  ;(auth-request/check-admin-tech request)
  (let [{:keys [crate-name crate-version]} (:route-params request)
        bin-file (store/get-file crate-file-store
                                 crate-name
                                 crate-version)]
    (log/info (log/req-ctx request)
              (format "serving crate file for crate %s version %s"
                      crate-name
                      crate-version))
    (public-crate-version/inc-download crate-version-db crate-name crate-version)
    {:status 200
     :body bin-file}))
