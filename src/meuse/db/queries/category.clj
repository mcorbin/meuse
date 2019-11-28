(ns meuse.db.queries.category
  (:require [honeysql.core :as sql]
            [honeysql.helpers :as h])
  (:import java.util.UUID))

(defn get-category
  [where-clause]
  (-> (h/select :c.id
                :c.name
                :c.description)
      (h/from [:categories :c])
      (h/where where-clause)
      sql/format))

(defn get-categories
  []
  (-> (h/select :c.id
                :c.name
                :c.description)
      (h/from [:categories :c])
      sql/format))

(defn by-name
  [category-name]
  (get-category [:= :c/name category-name]))

(defn by-crate-id
  [crate-id]
  (-> (h/select :c.id
                :c.name
                :c.description)
      (h/from [:categories :c])
      (h/left-join [:crates_categories :cc]
                   [:and
                    [:= :cc.category_id :c.id]
                    [:= :cc.crate_id crate-id]])
      sql/format))

(defn create
  [category-name description]
  (-> (h/insert-into :categories)
      (h/columns :id
                 :name
                 :description)
      (h/values [[(UUID/randomUUID)
                  category-name
                  description]])
      sql/format))

(defn update-category
  [category-id fields]
  (-> (h/update :categories)
      (h/sset fields)
      (h/where [:= :id category-id])
      sql/format))

(defn count-crates-for-categories
  []
  (-> (h/select :%count.*
                :category_id)
      (h/from [:crates_categories :cc])
      (h/group :cc.category_id)
      sql/format))
