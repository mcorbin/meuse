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
  [database token]
  (jdbc/with-db-transaction [db-tx database]
    (if-let [user (user-db/get-user-by-name db-tx (:user token))]
      (let [token-value (generate-token)]
        (jdbc/execute! db-tx (token-queries/create-token
                              (bcrypt/encrypt token-value)
                              (:name token)
                              (:user-id user)
                              (expiration-date (:validity token))))
        token-value)
      (throw (ex-info (format "the user %s does not exist"
                              (:user token))
                      {:status 400})))))

(defn get-user-token
  "Get a token by name for an user."
  [db-tx user-name token-name]
  (if-let [user (user-db/get-user-by-name db-tx user-name)]
    (-> (jdbc/query db-tx (token-queries/get-user-token
                           (:user-id user)
                           token-name))
        first
        (clojure.set/rename-keys {:token_id :token-id
                                  :token_name :token-name
                                  :token_token :token-token
                                  :token_created_at :token-created-at
                                  :token_expired_at :token-expired-at
                                  :token_user_id :token-user-id}))
    (throw (ex-info (format "the user %s does not exist"
                            user-name)
                    {:status 404}))))

(defn get-user-tokens
  "Get the tokens for an user."
  [database user-name]
  (jdbc/with-db-transaction [db-tx database]
    (if-let [user (user-db/get-user-by-name db-tx user-name)]
      (->> (jdbc/query db-tx (token-queries/get-user-tokens
                             (:user-id user)))
           (map #(clojure.set/rename-keys % {:token_id :token-id
                                             :token_name :token-name
                                             :token_token :token-token
                                             :token_created_at :token-created-at
                                             :token_expired_at :token-expired-at
                                             :token_user_id :token-user-id})))
      (throw (ex-info (format "the user %s does not exist"
                              user-name)
                      {:status 404})))))

(defn delete-token
  "Deletes a token for an user."
  [database user-name token-name]
  (jdbc/with-db-transaction [db-tx database]
    (if-let [token (get-user-token db-tx user-name token-name)]
      (jdbc/execute! db-tx (token-queries/delete-token
                            (:token-id token)))
      (throw (ex-info (format "the token %s does not exist for the user %s"
                              token-name
                              user-name)
                      {:status 404})))))
