(ns meuse.auth.request
  (:require [meuse.auth.header :as header]
            [meuse.auth.token :as auth-token]
            [meuse.db.public.token :as public-token]
            [meuse.log :as log]
            [exoscale.ex :as ex]
            [clojure.set :refer [rename-keys]]))

;; todo: clean
(defn check-user
  "Takes a request. Verifies if the token is valid and populates the
  request with the user informations."
  [token-db request]
  (if-let [token (header/extract-token request)]
    (if-let [db-token (public-token/get-token-user-role token-db token)]
      (if (:users/active db-token)
        (if (auth-token/valid? token db-token)
          (do
            (log/info (log/req-ctx request) "user" (:users/name db-token) "authenticated")
            (public-token/set-last-used token-db (:tokens/id db-token))
            (assoc request
                   :auth
                   (-> (select-keys db-token [:roles/name
                                              :users/name
                                              :tokens/user_id])
                       (rename-keys {:tokens/user_id :user-id
                                     :roles/name :role-name
                                     :users/name :user-name}))))
          (throw (ex/ex-forbidden "invalid token")))
        (throw (ex/ex-forbidden "user is not active")))
      (throw (ex/ex-forbidden "token not found")))
    (throw (ex/ex-forbidden "token missing in the header"))))

(defn user-id
  [request]
  (get-in request [:auth :user-id]))

(defn user-name
  [request]
  (get-in request [:auth :user-name]))

(defn admin?
  [request]
  (= "admin" (get-in request [:auth :role-name])))

(defn tech?
  [request]
  (= "tech" (get-in request [:auth :role-name])))

(defn read-only?
  [request]
  (= "read-only" (get-in request [:auth :role-name])))

(defn check-admin
  "Takes a request, verifies is the user is admin."
  [request]
  (when-not (admin? request)
    (throw (ex/ex-forbidden "bad permissions")))
  true)

(defn check-tech
  "Takes a request, verifies is the user is tech."
  [request]
  (when-not (tech? request)
    (throw (ex/ex-forbidden "bad permissions")))
  true)

(defn check-admin-tech
  "Takes a request, verifies is the user is admin or tech."
  [request]
  (when-not (or (admin? request)
                (tech? request))
    (throw (ex/ex-forbidden "bad permissions")))
  true)

(defn check-authenticated
  "Takes a request, verifies is the user is authenticated"
  [request]
  (when-not (or (admin? request)
                (tech? request)
                (read-only? request))
    (throw (ex/ex-forbidden "bad permissions")))
  true)
