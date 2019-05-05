(ns meuse.db.user
  "Manage users in the database."
  (:require [clojure.java.jdbc :as jdbc]
            [clojure.tools.logging :refer [debug info error]]
            [meuse.db.crate :as crate]
            [meuse.db.queries.user :as user-queries]
            [meuse.db.role :as role]
            [meuse.message :refer [yanked?->msg]]
            [clojure.set :as set])
  (:import java.util.UUID))

(defn get-user-by-name
  "Get an user by username."
  [db-tx user-name]
  (-> (jdbc/query db-tx (user-queries/get-user-by-name user-name))
      first
      (clojure.set/rename-keys {:user_id :user-id
                                :user_name :user-name
                                :user_password :user-password
                                :user_description :user-description
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
        (jdbc/execute! db-tx (user-queries/create-user user (:role-id role))))
      (throw (ex-info (format "the role %s does not exist"
                              (:role user))
                      {:status 400})))))

(defn get-crate-user
  "Get the crate/user relation for a crate and an user."
  [db-tx crate-id user-id]
  (-> (jdbc/query db-tx (user-queries/get-crate-user crate-id user-id))
      first
      (clojure.set/rename-keys {:crate_id :crate-id
                                :user_id :user-id})))

(defn create-crate-user
  "Add an user as a owner of a crate"
  [db-tx crate-name user-name]
  (if-let [user (get-user-by-name db-tx user-name)]
    (if-let [crate (crate/get-crate-by-name db-tx crate-name)]
      (do
        (when (get-crate-user db-tx (:crate-id crate) (:user-id user))
          (throw (ex-info (format "the user %s already owns the crate %s"
                                  user-name
                                  crate-name)
                          {})))
        (jdbc/execute! db-tx (user-queries/create-crate-user
                              (:crate-id crate)
                              (:user-id user))))
      (throw (ex-info (format "the crate %s does not exist"
                              crate-name)
                      {})))
    (throw (ex-info (format "the user %s does not exist"
                            user-name)
                    {}))))

(defn create-crate-users
  "Add multiple users as owner of a crate"
  [database crate-name users]
  (jdbc/with-db-transaction [db-tx database]
    (doseq [user users]
      (create-crate-user db-tx crate-name user))))

;; todo: mutualize code with add-owner
(defn delete-crate-user
  "Remove an user as a owner of a crate"
  [database crate-name user-name]
  (jdbc/with-db-transaction [db-tx database]
    (if-let [user (get-user-by-name database user-name)]
      (if-let [crate (crate/get-crate-by-name db-tx crate-name)]
        (do
          (when-not (get-crate-user db-tx (:crate-id crate) (:user-id user))
            (throw (ex-info (format "the user %s does not own the crate %s"
                                    user-name
                                    crate-name)
                            {})))
          (jdbc/execute! db-tx (user-queries/delete-crate-user
                                (:crate-id crate)
                                (:user-id user))))
        (throw (ex-info (format "the crate %s does not exist"
                                crate-name)
                        {})))
      (throw (ex-info (format "the user %s does not exist"
                              user-name)
                      {})))))

(defn delete-crate-users
  "Remove multiple users as owner of a crate"
  [database crate-name users]
  (jdbc/with-db-transaction [db-tx database]
    (doseq [user users]
      (delete-crate-user db-tx crate-name user))))

(defn get-crate-join-crates-users
  "Get the crate users"
  [database crate-name]
  (jdbc/with-db-transaction [db-tx database]
    (if-let [crate (crate/get-crate-by-name db-tx crate-name)]
      (->> (jdbc/query db-tx (user-queries/get-crate-join-crates-users
                              (:crate-id crate)))
           (map #(set/rename-keys % {:crate_id :crate-id
                                     :user_id :user-id
                                     :user_cargo_id :user-cargo-id
                                     :user_name :user-name})))
      (throw (ex-info (format "the crate %s does not exist"
                              crate-name)
                      {})))))
