(ns meuse.db.actions.user
  "Manage users in the database."
  (:require [meuse.auth.password :as password]
            [meuse.db.actions.crate :as crate]
            [meuse.db.actions.role :as role]
            [meuse.db.queries.crate-user :as crate-user-queries]
            [meuse.db.queries.user :as user-queries]
            [exoscale.ex :as ex]
            [next.jdbc :as jdbc]))

(defn check-active!
  "Takes an user from the database. Throws if the user is inactive."
  [user]
  (when-not (:active user)
    (throw (ex/ex-incorrect (format "the user %s is inactive" (:name user)))))
  true)

(defn by-name
  "Get an user by username."
  [database user-name]
  (-> (jdbc/execute! database (user-queries/by-name user-name))
      first))

(defn by-id
  "Get an user by id."
  [database user-id]
  (-> (jdbc/execute! database (user-queries/by-id user-id))
      first))

(defn create
  "Crates an user."
  [database user]
  (jdbc/with-transaction [db-tx database]
    (if-let [role (role/by-name db-tx (:role user))]
      (if-let [user (by-name db-tx (:name user))]
        (throw (ex/ex-incorrect (format "the user %s already exists"
                                        (:name user))))
        (jdbc/execute! db-tx (user-queries/create user (:roles/id role))))
      (throw (ex/ex-incorrect (format "the role %s does not exist"
                                      (:role user)))))))

(defn delete
  "Deletes an user."
  [database user-name]
  (jdbc/with-transaction [db-tx database]
    (if-let [user (by-name db-tx user-name)]
      (do
        (jdbc/execute! db-tx (crate-user-queries/delete-for-user (:users/id user)))
        (jdbc/execute! db-tx (user-queries/delete (:users/id user))))
      (throw (ex/ex-incorrect (format "the user %s does not exist"
                                      user-name))))))

(defn crate-owners
  "Get the owners of a crate, by crate name"
  [database crate-name]
  (jdbc/with-transaction [db-tx database]
    (if-let [crate (crate/by-name db-tx crate-name)]
      (->> (jdbc/execute! db-tx (user-queries/users-join-crates-users
                                 (:crates/id crate))))
      (throw (ex/ex-not-found (format "the crate %s does not exist"
                                      crate-name))))))

(defn update-user
  "Updates an user."
  [database user-name fields]
  (jdbc/with-transaction [db-tx database]
    (if-let [user (by-name db-tx user-name)]
      (let [fields (cond-> fields
                     (:password fields) (update :password password/encrypt)
                     (:role fields) (assoc :role_id
                                           (-> (role/by-name database (:role fields))
                                               :roles/id))
                     :remove-role-name (dissoc :role)
                     :select-keys (select-keys
                                   [:role_id
                                    :description
                                    :password
                                    :active]))]
        (jdbc/execute! db-tx (user-queries/update-user (:users/id user) fields)))
      (throw (ex/ex-not-found (format "the user %s does not exist"
                                      user-name))))))

(defn get-users
  "get all existing users"
  [database]
  (->> (jdbc/execute! database (user-queries/get-users-join-role))))

(defn count-users
  "count the number of users"
  [database]
  (-> (jdbc/execute! database (user-queries/count-users))
      first))
