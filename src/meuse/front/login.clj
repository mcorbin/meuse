(ns meuse.front.login
  (:require [exoscale.ex :as ex]
            [meuse.auth.frontend :as auth-frontend]
            [meuse.auth.password :as auth-password]
            [meuse.db.public.user :as public-user]))

(defn check-password
  [user-db request]
  ;; todo: spec params
  (let [username (get-in request [:params :username])
        password (get-in request [:params :password])
        db-user (public-user/by-name user-db username)]
    (when-not db-user
      (throw (ex/ex-forbidden "forbidden" {})))
    (auth-password/check password (:users/password db-user))
    db-user))

(defn login!
  [request user-db key-spec]
  (let [db-user (check-password user-db request)]
    {:status 302
     :headers {"Location" "/front/"}
     :cookies {"session-token" {:value (auth-frontend/generate-token
                                        (:users/id db-user)
                                        key-spec)}}}))
