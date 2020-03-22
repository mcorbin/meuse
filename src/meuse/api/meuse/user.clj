(ns meuse.api.meuse.user
  (:require [meuse.api.params :as params]
            [meuse.auth.request :as auth-request]
            [meuse.db.public.user :as public-user]
            [meuse.log :as log]
            [exoscale.ex :as ex]))

(defn new-user
  [user-db request]
  (params/validate-params request ::new)
  (auth-request/check-admin request)
  (log/info (log/req-ctx request) "create user" (get-in request [:body :name]))
  (public-user/create user-db
                      (:body request))
  {:status 200
   :body {:ok true}})

(defn delete-user
  [user-db request]
  (params/validate-params request ::delete)
  (auth-request/check-admin request)
  (log/info (log/req-ctx request) "delete user" (get-in request [:route-params :name]))
  (public-user/delete user-db
                      (get-in request [:route-params :name]))
  {:status 200
   :body {:ok true}})

(defn update-user
  [user-db request]
  (params/validate-params request ::update)
  (auth-request/check-authenticated request)
  (let [user-name (get-in request [:route-params :name])
        fields (:body request)]
    (when (and (not (auth-request/admin? request))
               (contains? fields :role))
      (throw (ex/ex-forbidden "only admins can update an user role")))
    (when (and (not (auth-request/admin? request))
               (contains? fields :active))
      (throw (ex/ex-forbidden "only admins can enable or disable an user")))
    (when (and (not (auth-request/admin? request))
               (not= (auth-request/user-name request)
                     user-name))
      (throw (ex/ex-forbidden "bad permissions")))
    (log/info (log/req-ctx request) "update user" user-name)
    (public-user/update-user user-db
                             user-name
                             fields)
    {:status 200
     :body {:ok true}}))

(defn list-users
  [user-db request]
  (auth-request/check-admin request)
  (log/info (log/req-ctx request) "list users")
  {:status 200
   :body {:users (->> (public-user/get-users user-db)
                      (map #(clojure.set/rename-keys
                             %
                             {:users/id :id
                              :users/name :name
                              :users/description :description
                              :users/active :active
                              :roles/name :role})))}})
