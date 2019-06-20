(ns meuse.api.crate.yank
  (:require [meuse.api.crate.http :refer (crates-api!)]
            [meuse.auth.request :as auth-request]
            [meuse.db.crate-user :as crate-user-db]
            [meuse.db.crate-version :as crate-version-db]
            [meuse.api.params :as params]
            [meuse.git :as git]
            [meuse.metadata :as metadata]
            [meuse.message :as msg]
            [clojure.tools.logging :refer [debug info error]]))

(defn update-yank
  [request yanked?]
  (params/validate-params request ::yank)
  (let [{:keys [crate-name crate-version]} (:route-params request)]
    (when-not (auth-request/admin? request)
      (crate-user-db/owned-by? (:database request)
                         crate-name
                         (auth-request/user-id request)))
    (info (msg/yanked?->msg yanked?) "crate" crate-name "version" crate-version)
    (crate-version-db/update-yank (:database request) crate-name crate-version yanked?)
    (metadata/update-yank (get-in request [:config :metadata :path])
                          crate-name
                          crate-version
                          yanked?)
    (git/add (:git request))
    (apply git/commit (:git request) (msg/yank-commit-msg
                                      crate-name
                                      crate-version
                                      yanked?))
    (git/push (:git request))
    {:status 200
     :body {:ok true}}))

(defmethod crates-api! :yank
  [request]
  (update-yank request true))

(defmethod crates-api! :unyank
  [request]
  (update-yank request false))
