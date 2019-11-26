(ns meuse.db.queries.crate-category
  (:require [honeysql.core :as sql]
            [honeysql.helpers :as h]))

(defn get-crate-category
  [where-clause]
  (-> (h/select :c.crate_id
                :c.category_id)
      (h/from [:crates_categories :c])
      (h/where where-clause)
      sql/format))

(defn by-crate-and-category
  [crate-id category-id]
  (get-crate-category [:and
                       [:= :c.crate_id crate-id]
                       [:= :c.category_id category-id]]))

(defn create
  [crate-id category-id]
  (-> (h/insert-into :crates_categories)
      (h/columns :crate_id
                 :category_id)
      (h/values [[crate-id
                  category-id]])
      sql/format))

