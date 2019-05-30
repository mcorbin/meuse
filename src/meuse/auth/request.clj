(ns meuse.auth.request
  (:require [meuse.auth.header :as header]
            [meuse.auth.token :as auth-token]
            [meuse.db.token :as token-db]
            [meuse.db.user :as user-db]))

(comment
  (defn check-user
    [database request]
    (if-let [token (header/extract-token request)]
      (if-let [db-token (token-db/get-token database token)]
        (if (auth-token/valid? token db-token)
          
          (throw (ex-info "invalid token" {:status 403}))
          )
        (throw (ex-info "token not found" {:status 403})))
      (throw (ex-info "token missing in the header" {:status 403})))))
