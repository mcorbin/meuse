(ns meuse.api.crate.yank
  (:require [meuse.api.params :as params]
            [meuse.auth.request :as auth-request]
            [meuse.db.public.crate-user :as public-crate-user]
            [meuse.db.public.crate-version :as public-crate-version]
            [meuse.git :as git]
            [meuse.log :as log]
            [meuse.metadata :as metadata]
            [meuse.message :as msg]))

(defn update-yank
  [crate-user-db crate-version-db git-object request yanked?]
  (params/validate-params request ::yank)
  (let [{:keys [crate-name crate-version]} (:route-params request)]
    (when-not (auth-request/admin? request)
      (public-crate-user/owned-by? crate-user-db
                                   crate-name
                                   (auth-request/user-id request)))
    (log/info
     (log/req-ctx request)
     (msg/yanked?->msg yanked?) "crate" crate-name "version" crate-version)
    (public-crate-version/update-yank crate-version-db
                                      crate-name
                                      crate-version yanked?)
    (locking (git/get-lock git-object)
      (metadata/update-yank (get-in request [:config :metadata :path])
                            crate-name
                            crate-version
                            yanked?)
      (git/add git-object)
      (apply git/commit git-object (msg/yank-commit-msg
                                    crate-name
                                    crate-version
                                    yanked?))
      (git/push git-object)))
  {:status 200
   :body {:ok true}})

(defn yank
  [crate-user-db crate-version-db git-object request]
  (update-yank crate-user-db crate-version-db git-object request true))

(defn unyank
  [crate-user-db crate-version-db git-object request]
  (update-yank crate-user-db crate-version-db git-object request false))
