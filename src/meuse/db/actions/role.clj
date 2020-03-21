(ns meuse.db.actions.role
  "Manage roles in the database"
  (:require [meuse.db.queries.role :as role-queries]
            [exoscale.ex :as ex]
            [next.jdbc :as jdbc]))

(def admin-role-name "admin")
(def tech-role-name "tech")
(def read-only-role-name "read-only")

(defn by-name
  "Get a roles by name."
  [db-tx role-name]
  (let [role (-> (jdbc/execute! db-tx (role-queries/by-name role-name))
                 first)]
    (when-not role
      (throw (ex/ex-not-found (format "the role %s does not exist" role-name))))
    role))

(defn get-admin-role
  "Get the admin role."
  [db-tx]
  (by-name db-tx admin-role-name))

(defn get-tech-role
  "Get the tech role."
  [db-tx]
  (by-name db-tx tech-role-name))

(defn get-read-only-role
  "Get the tech role."
  [db-tx]
  (by-name db-tx tech-role-name))

