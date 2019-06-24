(ns meuse.api.crate.owner
  "Owner Cargo API"
  (:require [meuse.api.crate.http :refer (crates-api!)]
            [meuse.api.params :as params]
            [meuse.auth.request :as auth-request]
            [meuse.db.crate-user :as crate-user-db]
            [meuse.db.user :as user-db]
            [meuse.request :refer [convert-body-edn]]
            [clojure.tools.logging :refer [debug info error]]
            [clojure.string :as string]))

(defmethod crates-api! :add-owner
  [request]
  (let [request (convert-body-edn request)
        _ (params/validate-params request ::add)
        crate-name (get-in request [:route-params
                                    :crate-name])
        users (get-in request [:body :users])]
    (when-not (auth-request/admin? request)
      (crate-user-db/owned-by? (:database request)
                         crate-name
                         (auth-request/user-id request)))
    (info "add owners" (string/join ", " users) "to crate" crate-name)
    (crate-user-db/create-crate-users (:database request)
                                crate-name
                                users)
    {:status 200
     :body {:ok true
            :msg (format "added user(s) %s as owner(s) of crate %s"
                         (string/join ", " users)
                         crate-name)}}))

(defmethod crates-api! :remove-owner
  [request]
  (let [request (convert-body-edn request)
        _ (params/validate-params request ::remove)
        crate-name (get-in request [:route-params
                                    :crate-name])
        users (get-in request [:body :users])]
    (when-not (auth-request/admin? request)
      (crate-user-db/owned-by? (:database request)
                         crate-name
                         (auth-request/user-id request)))
    (info "remove owners" (string/join ", " users) "to crate" crate-name)
    (crate-user-db/delete-crate-users (:database request)
                                crate-name
                                users)
    {:status 200
     :body {:ok true
            :msg (format "removed user(s) %s as owner(s) of crate %s"
                         (string/join ", " users)
                         crate-name)}}))

(defmethod crates-api! :list-owners
  [request]
  (params/validate-params request ::list)
  (let [crate-name (get-in request [:route-params
                                    :crate-name])
        users (user-db/crate-owners
               (:database request)
               crate-name)]
    (info "list owners for crate" crate-name)
    {:status 200
     :body {:users (map
                    (fn [u]
                      {:login (:user-name u)
                       :name (:user-name u)
                       :id (rand-int 100)})
                    users)}}))

