(ns meuse.db.queries.role
  (:require [honeysql.core :as sql]
            [honeysql.helpers :as h]))

(defn get-role
  [where-clause]
  (-> (h/select [:r.id "role_id"]
                [:r.name "role_name"])
      (h/from [:roles :r])
      (h/where where-clause)
      sql/format))

(defn by-name
  [role-name]
  (get-role [:= :r.name role-name]))

