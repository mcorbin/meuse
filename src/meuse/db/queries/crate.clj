(ns meuse.db.queries.crate
  (:require [honeysql.core :as sql]
            [honeysql.helpers :as h]))

(defn get-crate
  [where-clause]
  (-> (h/select :c.id
                :c.name)
      (h/from [:crates :c])
      (h/where where-clause)
      sql/format))

(defn by-name-join-version
  [crate-name crate-version]
  (-> (h/select :c.id
                :c.name
                :v.id
                :v.version
                :v.description
                :v.yanked
                :v.created_at
                :v.download_count
                :v.updated_at
                :v.document_vectors
                :v.crate_id)
      (h/from [:crates :c])
      (h/join [:crates_versions :v] [:and
                                     [:= :c.id :v.crate_id]
                                     [:= :v.version crate-version]])
      (h/where [:= :c.name crate-name])
      sql/format))

(defn by-name
  [crate-name]
  (get-crate [:= :c.name crate-name]))

(defn create
  [metadata crate-id]
  (-> (h/insert-into :crates)
      (h/columns :id :name)
      (h/values [[crate-id
                  (:name metadata)]])
      sql/format))

(defn get-crates-and-versions
  []
  (-> (h/select :c.id
                :c.name
                :v.id
                :v.version
                :v.description
                :v.download_count
                :v.yanked
                :v.created_at
                :v.updated_at)
      (h/from [:crates :c])
      (h/join [:crates_versions :v]
              [:= :c.id :v.crate_id])
      sql/format))

(defn get-crate-and-versions
  [crate-name]
  (-> (h/select :c.id
                :c.name
                :v.id
                :v.version
                :v.description
                :v.download_count
                :v.yanked
                :v.created_at
                :v.metadata
                :v.updated_at)
      (h/from [:crates :c])
      (h/join [:crates_versions :v]
              [:= :c.id :v.crate_id])
      (h/where [:= :c.name crate-name])
      sql/format))

(defn get-crates-for-category
  [category-id]
  (-> (h/select :c.id
                :c.name
                :v.id
                :v.version
                :v.description
                :v.download_count
                :v.yanked
                :v.created_at
                :v.updated_at)
      (h/from [:crates :c])
      (h/join [:crates_versions :v]
              [:= :c.id :v.crate_id]
              [:crates_categories :cat]
              [:= :cat.crate_id :c.id])
      (h/where [:= :cat.category_id category-id])
      sql/format))

(defn get-crates-range
  [start end prefix]
  (-> (h/select :c.id
                :c.name
                :%count.*)
      (h/from [:crates :c])
      (h/where [:like :c.name (str prefix "%")])
      (h/join [:crates_versions :v]
              [:= :c.id :v.crate_id])
      (h/group :c.id)
      (h/order-by :c.name)
      (h/offset start)
      (h/limit end)
      sql/format))

(defn count-crates
  []
  (-> (h/select :%count.*)
      (h/from [:crates :c])
      sql/format))

(defn count-crates-prefix
  [prefix]
  (-> (h/select :%count.*)
      (h/from [:crates :c])
      (h/where [:like :c.name (str prefix "%")])
      sql/format))
