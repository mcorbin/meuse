(ns meuse.auth.request
  (:require [meuse.auth.header :as header]
            [meuse.auth.token :as auth-token]
            [meuse.db.token :as token-db]
            [meuse.db.user :as user-db]))

;; todo: clean this mess
(defn check-user
  "Takes a request. Verifies if the token is valid and populates the
  request with the password informations."
  [request]
  (if-let [token (header/extract-token request)]
    (if-let [db-token (token-db/get-token-user-role (:database request) token)]
      (if (:user-active db-token)
        (if (auth-token/valid? token db-token)
          (assoc request
                 :auth
                 (select-keys db-token [:role-name
                                        :user-name
                                        :user-id]))
          (throw (ex-info "invalid token" {:status 403})))
        (throw (ex-info "user is not active" {:status 403})))
      (throw (ex-info "token not found" {:status 403})))
    (throw (ex-info "token missing in the header" {:status 403}))))
