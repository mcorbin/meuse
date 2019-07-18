(ns meuse.auth.request
  (:require [meuse.auth.header :as header]
            [meuse.auth.token :as auth-token]
            [meuse.db.token :as token-db]
            [meuse.db.user :as user-db]
            [clojure.set :refer [rename-keys]]
            [clojure.tools.logging :refer [debug info error]]))

;; todo: clean
(defn check-user
  "Takes a request. Verifies if the token is valid and populates the
  request with the user informations."
  [request]
  (if-let [token (header/extract-token request)]
    (if-let [db-token (token-db/get-token-user-role (:database request) token)]
      (if (:user-active db-token)
        (if (auth-token/valid? token db-token)
          (do
            (info "user" (:user-name token) "authenticated")
            (assoc request
                   :auth
                   (-> (select-keys db-token [:role-name
                                              :user-name
                                              :token-user-id])
                       (rename-keys {:token-user-id :user-id}))))
          (throw (ex-info "invalid token" {:status 403})))
        (throw (ex-info "user is not active" {:status 403})))
      (throw (ex-info "token not found" {:status 403})))
    (throw (ex-info "token missing in the header" {:status 403}))))

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

(defn admin?-throw
  "Takes a request, verifies is the user is admin."
  [request]
  (when-not (admin? request)
    (throw (ex-info "bad permissions" {:status 403})))
  true)

(defn tech?-throw
  "Takes a request, verifies is the user is tech."
  [request]
  (when-not (tech? request)
    (throw (ex-info "bad permissions" {:status 403})))
  true)

(defn admin-or-tech?-throw
  "Takes a request, verifies is the user is admin or tech."
  [request]
  (when-not (or (admin? request)
                (tech? request))
    (throw (ex-info "bad permissions" {:status 403})))
  true)
