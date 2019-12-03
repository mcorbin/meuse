(ns meuse.db.actions.role
  "Manage roles in the database"
  (:require [meuse.db.queries.role :as role-queries]
            [meuse.message :refer [yanked?->msg]]
            [exoscale.ex :as ex]
            [next.jdbc :as jdbc]
            [clojure.tools.logging :refer [debug info error]])
  (:import java.util.UUID))

(def admin-role-name "admin")
(def tech-role-name "tech")

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

