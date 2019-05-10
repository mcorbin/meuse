(ns meuse.db.token
  "Manage tokens in the database"
  (:require [clj-time.core :as t]
            [clojure.java.jdbc :as jdbc]
            [clojure.tools.logging :refer [debug info error]]
            [crypto.password.bcrypt :as bcrypt]
            [crypto.random :as random]
            [meuse.db.queries.token :as token-queries]
            [meuse.db.user :as user-db])
  (:import java.util.UUID))

(def token-size 50)

(defn expiration-date
  "Calculates the expiration date of the token"
  [validity]
  (t/plus (t/now) (t/days validity)))

(defn generate-token
  []
  (random/base64 token-size))

(defn create-token
  "Creates a new token for an user. `validity` is the number of days before the
  expiration of the token.
  Returns the generated token."
  [database user-name validity]
  (jdbc/with-db-transaction [db-tx database]
    (if-let [user (user-db/get-user-by-name db-tx user-name)]
      (let [token (generate-token)]
        (jdbc/execute! db-tx (token-queries/create-token
                              (bcrypt/encrypt token)
                              (:user-id user)
                              (expiration-date validity)))
        token)
      (throw (ex-info (format "the user %s does not exist"
                              user-name)
                      {:status 400})))))

(defn get-user-tokens
  "Get the tokens for an user."
  [database user-name]
  (jdbc/with-db-transaction [db-tx database]
    (if-let [user (user-db/get-user-by-name db-tx user-name)]
      (->> (jdbc/query db-tx (token-queries/get-user-tokens
                             (:user-id user)))
           (map #(clojure.set/rename-keys % {:token_id :token-id
                                             :token_token :token-token
                                             :token_created_at :token-created-at
                                             :token_expired_at :token-expired-at
                                             :token_user_id :token-user-id})))
      (throw (ex-info (format "the user %s does not exist"
                              user-name)
                      {:status 400})))))
