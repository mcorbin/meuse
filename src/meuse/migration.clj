(ns meuse.migration
  (:require [ragtime.core :as ragtime]
            [ragtime.jdbc :as jdbc]))

(def migrations-table "database_migrations")
(def migrations-path "migrations")

(defn migrate!
  [database]
  (let [database {:datasource database}]
    (let [store (jdbc/sql-database database
                                   {:migrations-table migrations-table})
          migrations (jdbc/load-resources migrations-path)
          index (ragtime/into-index migrations)]
      (ragtime/migrate-all store index migrations))))
