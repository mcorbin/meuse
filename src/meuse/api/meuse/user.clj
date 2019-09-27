(ns meuse.api.meuse.user
  (:require [meuse.api.meuse.http :refer [meuse-api!]]
            [meuse.api.params :as params]
            [meuse.auth.request :as auth-request]
            [meuse.db.user :as db-user]
            [clojure.tools.logging :refer [debug info error]]))

(defmethod meuse-api! :new-user
  [request]
  (params/validate-params request ::new)
  (auth-request/admin?-throw request)
  (info "create user" (get-in request [:body :name]))
  (db-user/create (:database request)
                       (:body request))
  {:status 200
   :body {:ok true}})

(defmethod meuse-api! :delete-user
  [request]
  (params/validate-params request ::delete)
  (auth-request/admin?-throw request)
  (info "delete user" (get-in request [:route-params :name]))
  (db-user/delete (:database request)
                  (get-in request [:route-params :name]))
  {:status 200
   :body {:ok true}})

(defmethod meuse-api! :update-user
  [request]
  (params/validate-params request ::update)
  (auth-request/admin-or-tech?-throw request)
  (let [user-name (get-in request [:route-params :name])
        fields (:body request)]
    (when (and (not (auth-request/admin? request))
               (contains? fields :role))
      (throw (ex-info "only admins can update an user role"
                      {:type :meuse.error/forbidden})))
    (when (and (not (auth-request/admin? request))
               (contains? fields :active))
      (throw (ex-info "only admins can enable or disable an user"
                      {:type :meuse.error/forbidden})))
    (when (and (not (auth-request/admin? request))
               (not= (auth-request/user-name request)
                     user-name))
      (throw (ex-info "bad permissions"
                      {:type :meuse.error/forbidden})))
    (info "update user" user-name)
    (db-user/update-user (:database request)
                         user-name
                         fields)
    {:status 200
     :body {:ok true}}))

(defmethod meuse-api! :list-users
  [request]
  (auth-request/admin?-throw request)
  {:status 200
   :body {:users (db-user/get-users (:database request))}})
