(ns meuse.db.crate-user
  (:require [meuse.db.crate :as crate]
            [meuse.db.queries.crate-user :as crate-user-queries]
            [meuse.db.role :as role]
            [meuse.db.user :as user-db]
            [clojure.java.jdbc :as jdbc]
            [clojure.set :as set]
            [clojure.tools.logging :refer [debug info error]])
  (:import java.util.UUID))

(defn by-id
  "Get the crate/user relation for a crate and an user."
  [db-tx crate-id user-id]
  (-> (jdbc/query db-tx (crate-user-queries/by-crate-and-user crate-id user-id))
      first
      (clojure.set/rename-keys {:crate_id :crate-id
                                :user_id :user-id})))

(defn create
  "Add an user as a owner of a crate"
  [db-tx crate-name user-name]
  (if-let [user (user-db/by-name db-tx user-name)]
    (if-let [crate (crate/by-name db-tx crate-name)]
      (do
        (when (by-id db-tx (:crate-id crate) (:user-id user))
          (throw (ex-info (format "the user %s already owns the crate %s"
                                  user-name
                                  crate-name)
                          {})))
        (jdbc/execute! db-tx (crate-user-queries/create
                              (:crate-id crate)
                              (:user-id user))))
      (throw (ex-info (format "the crate %s does not exist"
                              crate-name)
                      {:status 404})))
    (throw (ex-info (format "the user %s does not exist"
                            user-name)
                    {:status 404}))))

(defn create-crate-users
  "Add multiple users as owner of a crate"
  [database crate-name users]
  (jdbc/with-db-transaction [db-tx database]
    (doseq [user users]
      (create db-tx crate-name user))))

(defn owned-by?
  "Checks if a crate is owned by an user."
  [database crate-name user-id]
  (if-let [crate (crate/by-name database crate-name)]
    (if (-> (jdbc/query database (crate-user-queries/by-crate-and-user
                                  (:crate-id crate)
                                  user-id))
            first)
      true
      (throw (ex-info (format "user does not own the crate %s"
                              crate-name)
                    {:status 403})))
    (throw (ex-info (format "the crate %s does not exist"
                            crate-name)
                    {:status 404}))))

(defn delete
  "Remove an user as a owner of a crate"
  [database crate-name user-name]
  (jdbc/with-db-transaction [db-tx database]
    (if-let [user (user-db/by-name database user-name)]
      (if-let [crate (crate/by-name db-tx crate-name)]
        (do
          (when-not (by-id db-tx (:crate-id crate) (:user-id user))
            (throw (ex-info (format "the user %s does not own the crate %s"
                                    user-name
                                    crate-name)
                            {:status 400})))
          (jdbc/execute! db-tx (crate-user-queries/delete
                                (:crate-id crate)
                                (:user-id user))))
        (throw (ex-info (format "the crate %s does not exist"
                                crate-name)
                        {:status 404})))
      (throw (ex-info (format "the user %s does not exist"
                              user-name)
                      {:status 404})))))

(defn delete-crate-users
  "Remove multiple users as owner of a crate"
  [database crate-name users]
  (jdbc/with-db-transaction [db-tx database]
    (doseq [user users]
      (delete db-tx crate-name user))))