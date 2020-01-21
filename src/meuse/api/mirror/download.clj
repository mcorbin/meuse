(ns meuse.api.mirror.download
  (:require [meuse.api.crate.http :refer (crates-api!)]
            [meuse.api.params :as params]
            [meuse.auth.request :as auth-request]
            [meuse.mirror :as mirror]
            [meuse.store.protocol :as store]
            [clojure.java.io :as io]
            [clojure.tools.logging :refer [infof]])
  (:import java.io.File))

(defn download
  [mirror-store request]
  (params/validate-params request :meuse.api.crate.download/download)
   ;;  (auth-request/check-admin-tech request)
  (let [{:keys [crate-name crate-version]} (:route-params request)]
    (infof "mirror: serving crate file for crate %s version %s"
           crate-name
           crate-version)
    (if (store/exists mirror-store crate-name crate-version)
      (do
        (infof "mirror: get crate %s %s from cache"
               crate-name
               crate-version)
        {:status 200
         :body (store/get-file mirror-store
                               crate-name
                               crate-version)})
      {:status 200
       :body (mirror/download-and-save mirror-store
                                       crate-name
                                       crate-version)})))
