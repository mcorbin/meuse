(ns meuse.db.user
  "Manage users in the database."
  (:require [clojure.java.jdbc :as jdbc]
            [clojure.tools.logging :refer [debug info error]]
            [meuse.db.crate :as crate]
            [meuse.db.queries.user :as user-queries]
            [meuse.db.queries.crate-user :as crate-user-queries]
            [meuse.db.role :as role]
            [meuse.message :refer [yanked?->msg]]
            [clojure.set :as set])
  (:import java.util.UUID))

(defn check-active!
  "Takes an user from the database. Throws if the user is inactive."
  [user]
  (when-not (:active user)
    (throw (ex-info (format "the user %s is inactive" (:name user))
                    {:status 400})))
  true)

(defn get-user-by-name
  "Get an user by username."
  [db-tx user-name]
  (-> (jdbc/query db-tx (user-queries/get-user-by-name user-name))
      first
      (clojure.set/rename-keys {:user_id :user-id
                                :user_name :user-name
                                :user_password :user-password
                                :user_description :user-description
                                :user_active :user-active
                                :user_role_id :user-role-id})))

(defn create-user
  "Crates an user."
  [database user]
  (jdbc/with-db-transaction [db-tx database]
    (if-let [role (role/get-role-by-name db-tx (:role user))]
      (if-let [user (get-user-by-name db-tx (:name user))]
        (throw (ex-info (format "the user %s already exists"
                                (:name user))
                        {:status 400}))
        (jdbc/execute! db-tx (user-queries/create user (:role-id role))))
      (throw (ex-info (format "the role %s does not exist"
                              (:role user))
                      {:status 400})))))

(defn delete-user
  "Deletes an user."
  [database user-name]
  (jdbc/with-db-transaction [db-tx database]
    (if-let [user (get-user-by-name db-tx user-name)]
      (do
        (jdbc/execute! db-tx (crate-user-queries/delete-for-user (:user-id user)))
        (jdbc/execute! db-tx (user-queries/delete (:user-id user))))
      (throw (ex-info (format "the user %s does not exist"
                              user-name)
                      {:status 400})))))

(defn get-crate-join-crates-users
  "Get the crate users"
  [database crate-name]
  (jdbc/with-db-transaction [db-tx database]
    (if-let [crate (crate/by-name db-tx crate-name)]
      (->> (jdbc/query db-tx (user-queries/users-join-crates-users
                              (:crate-id crate)))
           (map #(set/rename-keys % {:crate_id :crate-id
                                     :user_id :user-id
                                     :user_cargo_id :user-cargo-id
                                     :user_name :user-name})))
      (throw (ex-info (format "the crate %s does not exist"
                              crate-name)
                      {:status 404})))))
