(ns meuse.api.mirror.cache
  (:require [meuse.api.params :as params]
            [meuse.auth.request :as auth-request]
            [meuse.log :as log]
            [meuse.mirror :as mirror]
            [meuse.store.protocol :as store]))

(defn cache
  [mirror-store request]
  (params/validate-params request :meuse.api.crate.mirror/cache)
  (auth-request/check-admin-tech request)
  (let [{:keys [crate-name crate-version]} (:route-params request)]
    (log/infof (log/req-ctx request)
               "mirror: caching crate %s version %s"
               crate-name
               crate-version)
    (if (store/exists mirror-store crate-name crate-version)
      (do
        (log/infof (log/req-ctx request)
                   "mirror: crate %s %s already exists in the cache"
                   crate-name
                   crate-version)
        {:status 200
         :body {:ok true}})
      (do
        (mirror/download-and-save mirror-store
                                  crate-name
                                  crate-version)
        {:status 200
         :body {:ok true}}))))
