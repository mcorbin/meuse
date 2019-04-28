(ns meuse.db.user
  (:require [clojure.java.jdbc :as jdbc]
            [clojure.tools.logging :refer [debug info error]]
            [meuse.db.crate :as crate]
            [meuse.db.queries :as queries]
            [meuse.db.role :as role]
            [meuse.message :refer [yanked?->msg]])
  (:import java.util.UUID))

(defn get-user-by-name
  "Get an user by username."
  [db-tx user-name]
  (-> (jdbc/query db-tx (queries/get-user-by-name user-name))
      first
      (clojure.set/rename-keys {:user_id :user-id
                                :user_name :user-name
                                :user_password :user-password
                                :user_description :user-description
                                :user_role_id :user-role-id})))

(defn create
  "Crates an user."
  [database user]
  (jdbc/with-db-transaction [db-tx database]
    (if-let [role (role/get-role-by-name db-tx (:role user))]
      (if-let [user (get-user-by-name db-tx (:name user))]
        (throw (ex-info (format "the user %s already exists"
                                (:name user))
                        {:status 400}))
        (jdbc/execute! db-tx (queries/create-user user (:role-id role))))
      (throw (ex-info (format "the role %s does not exist"
                              (:role user))
                      {:status 400})))))

(defn get-crate-user
  "Get the crate/user relation for a crate and an user."
  [db-tx crate-id user-id]
  (-> (jdbc/query db-tx (queries/get-crate-user crate-id user-id))
      first
      (clojure.set/rename-keys {:crate_id :crate-id
                                :user_id :user-id})))

(defn add-owner
  "Add an user as a owner of a crate"
  [database crate-name user-name]
  (jdbc/with-db-transaction [db-tx database]
    (if-let [user (get-user-by-name database user-name)]
      (if-let [crate (crate/get-crate db-tx crate-name)]
        (do
          (when (get-crate-user db-tx (:crate-id crate) (:user-id user))
            (throw (ex-info (format "the user %s already owns the crate %s"
                                    user-name
                                    crate-name)
                            {})))
          (jdbc/execute! db-tx (queries/add-crate-user
                                (:crate-id crate)
                                (:user-id user))))
        (throw (ex-info (format "the crate %s does not exist"
                                crate-name)
                        {})))
      (throw (ex-info (format "the user %s does not exist"
                              user-name)
                      {})))))

;; todo: mutualize code with add-owner
(defn remove-owner
  "Remove an user as a owner of a crate"
  [database crate-name user-name]
  (jdbc/with-db-transaction [db-tx database]
    (if-let [user (get-user-by-name database user-name)]
      (if-let [crate (crate/get-crate db-tx crate-name)]
        (do
          (when-not (get-crate-user db-tx (:crate-id crate) (:user-id user))
            (throw (ex-info (format "the user %s does not own the crate %s"
                                    user-name
                                    crate-name)
                            {})))
          (jdbc/execute! db-tx (queries/delete-crate-user
                                (:crate-id crate)
                                (:user-id user))))
        (throw (ex-info (format "the crate %s does not exist"
                                crate-name)
                        {})))
      (throw (ex-info (format "the user %s does not exist"
                              user-name)
                      {})))))
