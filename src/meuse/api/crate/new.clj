(ns meuse.api.crate.new
  (:require [meuse.api.crate.http :refer [crates-api!]]
            [meuse.api.params :as params]
            [meuse.auth.request :as auth-request]
            [meuse.crate :as crate]
            [meuse.db.public.crate :as public-crate]
            [meuse.git :as git]
            [meuse.log :as log]
            [meuse.metadata :as metadata]
            [meuse.message :as msg]
            [meuse.registry :as registry]
            [meuse.store.protocol :as store]
            [clojure.java.io :as io]))

(defn new
  [crate-db git-object crate-file-store request]
  (params/validate-params request ::new)
  (let [{:keys [git-metadata raw-metadata crate-file] :as crate}
        (crate/request->crate request)]
    (params/validate-params crate ::crate)
    (auth-request/check-admin-tech request)
    ;; check if the dependencies registry is allowed
    (registry/allowed-registry? raw-metadata
                                (get-in request
                                        [:registry-config
                                         :allowed-registries]))
    (log/info (log/req-ctx request) "publishing crate" (:name raw-metadata)
              "version" (:vers raw-metadata))
    ;; create the crate in the db
    (locking (git/get-lock git-object)
      (public-crate/create crate-db
                           raw-metadata
                           (auth-request/user-id request))
      ;; write the metadata file
      (metadata/write-metadata (get-in request [:config :metadata :path])
                               git-metadata)
      ;; write the crate binary file
      (store/write-file crate-file-store raw-metadata crate-file)
      ;; git add
      (git/add git-object)
      ;; git commit
      (apply git/commit git-object (msg/publish-commit-msg raw-metadata))
      ;; git push
      (git/push git-object))
    {:status 200
     :body {:warning {:invalid_categories []
                      :invalid_badges []
                      :other []}}}))

(defmethod crates-api! :andouillette
  [request]
  {:status 200
   :body (-> "static/andouillette.png" io/resource io/file)})
