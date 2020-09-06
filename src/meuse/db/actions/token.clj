(ns meuse.db.actions.token
  "Manage tokens in the database"
  (:require [meuse.auth.token :as auth-token]
            [meuse.db.actions.user :as user-db]
            [meuse.db.queries.token :as token-queries]
            [exoscale.ex :as ex]
            [next.jdbc :as jdbc]
            [crypto.password.bcrypt :as bcrypt]))

(defn by-user-and-name
  "Get a token by name for an user."
  [db-tx user-name token-name]
  (if-let [user (user-db/by-name db-tx user-name)]
    (-> (jdbc/execute! db-tx (token-queries/by-user-and-name
                              (:users/id user)
                              token-name))
        first)
    (throw (ex/ex-not-found (format "the user %s does not exist"
                                    user-name)))))

(defn create
  "Creates a new token for an user. `validity` is the number of days before the
  expiration of the token.
  Returns the generated token."
  [database token]
  (jdbc/with-transaction [db-tx database]
    (if-let [user (user-db/by-name db-tx (:user token))]
      (do
        (when (by-user-and-name db-tx (:user token) (:name token))
          (throw (ex/ex-incorrect (format "a token named %s already exists for user %s"
                                          (:name token)
                                          (:user token)))))
        (let [generated-token (auth-token/generate-token)]
          (jdbc/execute! db-tx (token-queries/create
                                (auth-token/extract-identifier generated-token)
                                (bcrypt/encrypt generated-token)
                                (:name token)
                                (:users/id user)
                                (auth-token/expiration-date (:validity token))))
          generated-token))
      (throw (ex/ex-not-found (format "the user %s does not exist"
                                      (:user token)))))))

(defn get-token-user-role
  "Get a token by value.
  Also returns informations about the user and the role."
  [database token]
  (-> (jdbc/execute! database (token-queries/token-join-user-join-role
                               (auth-token/extract-identifier token)))
      first))

(defn by-user
  "Get the tokens for an user."
  [database user-name]
  (jdbc/with-transaction [db-tx database]
    (if-let [user (user-db/by-name db-tx user-name)]
      (->> (jdbc/execute! db-tx (token-queries/by-user
                                 (:users/id user))))
      (throw (ex/ex-not-found (format "the user %s does not exist"
                                      user-name))))))

(defn delete
  "Deletes a token for an user."
  [database user-name token-name]
  (jdbc/with-transaction [db-tx database]
    (if-let [token (by-user-and-name db-tx user-name token-name)]
      (jdbc/execute! db-tx (token-queries/delete
                            (:tokens/id token)))
      (throw (ex/ex-not-found (format "the token %s does not exist for the user %s"
                                      token-name
                                      user-name))))))

(defn set-last-used
  "Set the last used value for a token."
  [database token-id]
  (jdbc/with-transaction [db-tx database]
    (jdbc/execute! db-tx (token-queries/set-last-used token-id))))
