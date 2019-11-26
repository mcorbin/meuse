(ns meuse.api.meuse.user
  (:require [meuse.api.meuse.http :refer [meuse-api!]]
            [meuse.api.params :as params]
            [meuse.auth.request :as auth-request]
            [meuse.db.public.user :as public-user]
            [clojure.tools.logging :refer [debug info error]]))

(defn new-user
  [user-db request]
  (params/validate-params request ::new)
  (auth-request/admin?-throw request)
  (info "create user" (get-in request [:body :name]))
  (public-user/create user-db
                      (:body request))
  {:status 200
   :body {:ok true}})

(defn delete-user
  [user-db request]
  (params/validate-params request ::delete)
  (auth-request/admin?-throw request)
  (info "delete user" (get-in request [:route-params :name]))
  (public-user/delete user-db
                      (get-in request [:route-params :name]))
  {:status 200
   :body {:ok true}})

(defn update-user
  [user-db request]
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
    (public-user/update-user user-db
                             user-name
                             fields)
    {:status 200
     :body {:ok true}}))

(defn list-users
  [user-db request]
  (auth-request/admin?-throw request)
  (info "list users")
  {:status 200
   :body {:users (->> (public-user/get-users user-db)
                      (map #(clojure.set/rename-keys
                             %
                             {:users/id :id
                              :users/name :name
                              :users/description :description
                              :users/active :active
                              :roles/name :role})))}})
