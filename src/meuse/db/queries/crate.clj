(ns meuse.db.queries.crate
  (:require [honeysql.core :as sql]
            [honeysql.helpers :as h])
  (:import java.sql.Timestamp
           java.util.Date
           java.util.UUID))

(defn get-crate
  [where-clause]
  (-> (h/select [:c.id "crate_id"]
                [:c.name "crate_name"])
      (h/from [:crates :c])
      (h/where where-clause)
      sql/format))

(defn create-crate
  [metadata crate-id]
  (-> (h/insert-into :crates)
      (h/columns :id :name)
      (h/values [[crate-id
                  (:name metadata)]])
      sql/format))
