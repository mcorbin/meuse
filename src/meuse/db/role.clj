(ns meuse.db.role
  "Manage roles in the database"
  (:require [clojure.java.jdbc :as jdbc]
            [clojure.tools.logging :refer [debug info error]]
            [meuse.db.queries.user :as user-queries]
            [meuse.message :refer [yanked?->msg]])
  (:import java.util.UUID))

(def admin-role-name "admin")
(def tech-role-name "tech")

(defn get-role-by-name
  "Get a roles by name."
  [db-tx role-name]
  (-> (jdbc/query db-tx (user-queries/get-role-by-name role-name))
      first
      (clojure.set/rename-keys {:role_id :role-id
                                :role_name :role-name})))

(defn get-admin-role
  "Get the admin role."
  [db-tx]
  (get-role-by-name db-tx admin-role-name))

(defn get-tech-role
  "Get the tech role."
  [db-tx]
  (get-role-by-name db-tx tech-role-name))

