(ns meuse.api.crate.owner
  "Owner Cargo API"
  (:require [meuse.api.crate.http :refer (crates-api!)]
            [meuse.api.params :as params]
            [meuse.auth.request :as auth-request]
            [meuse.db.public.crate-user :as public-crate-user]
            [meuse.db.public.user :as public-user]
            [meuse.request :refer [convert-body-edn]]
            [clojure.tools.logging :refer [debug info error]]
            [clojure.string :as string]))

(defn add-owner
  [crate-user-db request]
  (let [request (convert-body-edn request)
        _ (params/validate-params request ::add)
        crate-name (get-in request [:route-params
                                    :crate-name])
        users (get-in request [:body :users])]
    (when-not (auth-request/admin? request)
      (public-crate-user/owned-by? crate-user-db
                                   crate-name
                                   (auth-request/user-id request)))
    (info "add owners" (string/join ", " users) "to crate" crate-name)
    (public-crate-user/create-crate-users crate-user-db
                                          crate-name
                                          users)
    {:status 200
     :body {:ok true
            :msg (format "added user(s) %s as owner(s) of crate %s"
                         (string/join ", " users)
                         crate-name)}}))

(defn remove-owner
  [crate-user-db request]
  (let [request (convert-body-edn request)
        _ (params/validate-params request ::remove)
        crate-name (get-in request [:route-params
                                    :crate-name])
        users (get-in request [:body :users])]
    (when-not (auth-request/admin? request)
      (public-crate-user/owned-by? crate-user-db
                                   crate-name
                                   (auth-request/user-id request)))
    (info "remove owners" (string/join ", " users) "to crate" crate-name)
    (public-crate-user/delete-crate-users crate-user-db
                                          crate-name
                                          users)
    {:status 200
     :body {:ok true
            :msg (format "removed user(s) %s as owner(s) of crate %s"
                         (string/join ", " users)
                         crate-name)}}))

(defn list-owners
  [user-db request]
  (params/validate-params request ::list)
  (auth-request/admin-or-tech?-throw request)
  (let [crate-name (get-in request [:route-params
                                    :crate-name])
        users (public-user/crate-owners
               user-db
               crate-name)]
    (info "list owners for crate" crate-name)
    {:status 200
     :body {:users (map
                    (fn [u]
                      {:login (:user-name u)
                       :name (:user-name u)
                       :id (rand-int 100)})
                    users)}}))

