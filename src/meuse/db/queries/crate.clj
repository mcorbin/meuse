(ns meuse.db.queries.crate
  (:require [honeysql.core :as sql]
            [honeysql.helpers :as h])
  (:import java.util.Date
           java.util.UUID
           java.sql.Timestamp))

(defn get-crate
  [where-clause]
  (-> (h/select [:c.id "crate_id"]
                [:c.name "crate_name"])
      (h/from [:crates :c])
      (h/where where-clause)
      sql/format))

(defn by-name-join-version
  [crate-name crate-version]
  (-> (h/select [:c.id "crate_id"]
                [:c.name "crate_name"]
                [:v.id "version_id"]
                [:v.version "version_version"]
                [:v.description "version_description"]
                [:v.yanked "version_yanked"]
                [:v.created_at "version_created_at"]
                [:v.updated_at "version_updated_at"]
                [:v.document_vectors "version_document_vectors"]
                [:v.crate_id "version_crate_id"])
      (h/from [:crates :c])
      (h/left-join [:crates_versions :v] [:and
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
  (-> (h/select [:c.id "crate_id"]
                [:c.name "crate_name"]
                [:v.id "version_id"]
                [:v.version "version_version"]
                [:v.description "version_description"]
                [:v.yanked "version_yanked"]
                [:v.created_at "version_created_at"]
                [:v.updated_at "version_updated_at"])
      (h/from [:crates :c])
      (h/join [:crates_versions :v]
              [:= :c.id :v.crate_id])
      sql/format))

(defn get-crate-and-versions
  [crate-name]
  (-> (h/select [:c.id "crate_id"]
                [:c.name "crate_name"]
                [:v.id "version_id"]
                [:v.version "version_version"]
                [:v.description "version_description"]
                [:v.yanked "version_yanked"]
                [:v.created_at "version_created_at"]
                [:v.updated_at "version_updated_at"])
      (h/from [:crates :c])
      (h/join [:crates_versions :v]
              [:= :c.id :v.crate_id])
      (h/where [:= :c.name crate-name])
      sql/format))

(defn get-crates-for-category
  [category-id]
  (-> (h/select [:c.id "crate_id"]
                [:c.name "crate_name"]
                [:v.id "version_id"]
                [:v.version "version_version"]
                [:v.description "version_description"]
                [:v.yanked "version_yanked"]
                [:v.created_at "version_created_at"]
                [:v.updated_at "version_updated_at"])
      (h/from [:crates :c])
      (h/join [:crates_versions :v]
              [:= :c.id :v.crate_id]
              [:crates_categories :cat]
              [:= :cat.crate_id :c.id])
      (h/where [:= :cat.category_id category-id])
      sql/format))

