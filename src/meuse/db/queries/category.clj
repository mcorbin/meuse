(ns meuse.db.queries.category
  (:require [honeysql.core :as sql]
            [honeysql.helpers :as h])
  (:import java.util.UUID))

(defn get-category-by-name
  [category-name]
  (-> (h/select [:c.id "category_id"]
                [:c.name "category_name"]
                [:c.description "category_description"])
      (h/from [:categories :c])
      (h/where [:= :c.name category-name])
      sql/format))

(defn get-crate-category
  [crate-id category-id]
  (-> (h/select [:c.crate_id "crate_id"]
                [:c.category_id "category_id"])
      (h/from [:crates_categories :c])
      (h/where [:and
                [:= :c.crate_id crate-id]
                [:= :c.category_id category-id]])
      sql/format))

(defn get-crate-join-crates-categories
  [crate-id]
  (-> (h/select [:c.id "category_id"]
                [:c.name "category_name"]
                [:c.description "category_description"])
      (h/from [:categories :c])
      (h/left-join [:crates_categories :cc]
                   [:and
                    [:= :cc.category_id :c.id]
                    [:= :cc.crate_id crate-id]])
      sql/format))

(defn create-category
  [category-name description]
  (-> (h/insert-into :categories)
      (h/columns :id
                 :name
                 :description)
      (h/values [[(UUID/randomUUID)
                  category-name
                  description]])
      sql/format))

(defn create-crate-category
  [crate-id category-id]
  (-> (h/insert-into :crates_categories)
      (h/columns :crate_id
                 :category_id)
      (h/values [[crate-id
                  category-id]])
      sql/format))

