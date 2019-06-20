(ns meuse.db.crate-user
  (:require [clojure.java.jdbc :as jdbc]
            [clojure.tools.logging :refer [debug info error]]
            [meuse.db.crate :as crate]
            [meuse.db.user :as user-db]
            [meuse.db.queries.crate-user :as crate-user-queries]
            [meuse.db.role :as role]
            [clojure.set :as set])
  (:import java.util.UUID))

(defn get-crate-user
  "Get the crate/user relation for a crate and an user."
  [db-tx crate-id user-id]
  (-> (jdbc/query db-tx (crate-user-queries/by-crate-and-user crate-id user-id))
      first
      (clojure.set/rename-keys {:crate_id :crate-id
                                :user_id :user-id})))

(defn create-crate-user
  "Add an user as a owner of a crate"
  [db-tx crate-name user-name]
  (if-let [user (user-db/get-user-by-name db-tx user-name)]
    (if-let [crate (crate/get-crate-by-name db-tx crate-name)]
      (do
        (when (get-crate-user db-tx (:crate-id crate) (:user-id user))
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
      (create-crate-user db-tx crate-name user))))

(defn owned-by?
  "Checks if a crate is owned by an user."
  [database crate-name user-id]
  (if-let [crate (crate/get-crate-by-name database crate-name)]
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

(defn delete-crate-user
  "Remove an user as a owner of a crate"
  [database crate-name user-name]
  (jdbc/with-db-transaction [db-tx database]
    (if-let [user (user-db/get-user-by-name database user-name)]
      (if-let [crate (crate/get-crate-by-name db-tx crate-name)]
        (do
          (when-not (get-crate-user db-tx (:crate-id crate) (:user-id user))
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
      (delete-crate-user db-tx crate-name user))))
