(ns meuse.db.actions.crate-user
  (:require [meuse.db.actions.crate :as crate]
            [meuse.db.actions.role :as role]
            [meuse.db.actions.user :as user-db]
            [meuse.db.queries.crate-user :as crate-user-queries]
            [next.jdbc :as jdbc]
            [clojure.set :as set]
            [clojure.tools.logging :refer [debug info error]])
  (:import java.util.UUID))

(defn by-id
  "Get the crate/user relation for a crate and an user."
  [db-tx crate-id user-id]
  (-> (jdbc/execute! db-tx (crate-user-queries/by-crate-and-user crate-id user-id))
      first))

(defn create
  "Add an user as a owner of a crate"
  [db-tx crate-name user-name]
  (if-let [user (user-db/by-name db-tx user-name)]
    (if-let [crate (crate/by-name db-tx crate-name)]
      (do
        (when (by-id db-tx (:crates/id crate) (:users/id user))
          (throw (ex-info (format "the user %s already owns the crate %s"
                                  user-name
                                  crate-name)
                          {:type :meuse.error/incorrect})))
        (jdbc/execute! db-tx (crate-user-queries/create
                              (:crates/id crate)
                              (:users/id user))))
      (throw (ex-info (format "the crate %s does not exist"
                              crate-name)
                      {:type :meuse.error/not-found})))
    (throw (ex-info (format "the user %s does not exist"
                            user-name)
                    {:type :meuse.error/not-found}))))

(defn create-crate-users
  "Add multiple users as owner of a crate"
  [database crate-name users]
  (jdbc/with-transaction [db-tx database]
    (doseq [user users]
      (create db-tx crate-name user))))

(defn owned-by?
  "Checks if a crate is owned by an user."
  [database crate-name user-id]
  (if-let [crate (crate/by-name database crate-name)]
    (if (-> (jdbc/execute! database (crate-user-queries/by-crate-and-user
                                     (:crates/id crate)
                                     user-id))
            first)
      true
      (throw (ex-info (format "user does not own the crate %s"
                              crate-name)
                      {:type :meuse.error/forbidden})))
    (throw (ex-info (format "the crate %s does not exist"
                            crate-name)
                    {:type :meuse.error/not-found}))))

(defn delete
  "Remove an user as a owner of a crate"
  [database crate-name user-name]
  (jdbc/with-transaction [db-tx database]
    (if-let [user (user-db/by-name database user-name)]
      (if-let [crate (crate/by-name db-tx crate-name)]
        (do
          (when-not (by-id db-tx (:crates/id crate) (:users/id user))
            (throw (ex-info (format "the user %s does not own the crate %s"
                                    user-name
                                    crate-name)
                            {:type :meuse.error/incorrect})))
          (jdbc/execute! db-tx (crate-user-queries/delete
                                (:crates/id crate)
                                (:users/id user))))
        (throw (ex-info (format "the crate %s does not exist"
                                crate-name)
                        {:type :meuse.error/not-found})))
      (throw (ex-info (format "the user %s does not exist"
                              user-name)
                      {:type :meuse.error/not-found})))))

(defn delete-crate-users
  "Remove multiple users as owner of a crate"
  [database crate-name users]
  (jdbc/with-transaction [db-tx database]
    (doseq [user users]
      (delete db-tx crate-name user))))
