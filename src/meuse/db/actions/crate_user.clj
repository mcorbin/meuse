(ns meuse.db.actions.crate-user
  (:require [meuse.db.actions.crate :as crate]
            [meuse.db.actions.user :as user-db]
            [meuse.db.queries.crate-user :as crate-user-queries]
            [exoscale.ex :as ex]
            [next.jdbc :as jdbc]))

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
          (throw (ex/ex-incorrect (format "the user %s already owns the crate %s"
                                          user-name
                                          crate-name))))
        (jdbc/execute! db-tx (crate-user-queries/create
                              (:crates/id crate)
                              (:users/id user))))
      (throw (ex/ex-not-found (format "the crate %s does not exist"
                                      crate-name))))
    (throw (ex/ex-not-found (format "the user %s does not exist"
                                    user-name)))))

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
      (throw (ex/ex-forbidden (format "user does not own the crate %s"
                                      crate-name))))
    (throw (ex/ex-not-found (format "the crate %s does not exist"
                                    crate-name)))))

(defn delete
  "Remove an user as a owner of a crate"
  [database crate-name user-name]
  (jdbc/with-transaction [db-tx database]
    (if-let [user (user-db/by-name database user-name)]
      (if-let [crate (crate/by-name db-tx crate-name)]
        (do
          (when-not (by-id db-tx (:crates/id crate) (:users/id user))
            (throw (ex/ex-incorrect (format "the user %s does not own the crate %s"
                                            user-name
                                            crate-name))))
          (jdbc/execute! db-tx (crate-user-queries/delete
                                (:crates/id crate)
                                (:users/id user))))
        (throw (ex/ex-not-found (format "the crate %s does not exist"
                                        crate-name))))
      (throw (ex/ex-not-found (format "the user %s does not exist"
                                      user-name))))))

(defn delete-crate-users
  "Remove multiple users as owner of a crate"
  [database crate-name users]
  (jdbc/with-transaction [db-tx database]
    (doseq [user users]
      (delete db-tx crate-name user))))
