(ns meuse.db.queries.crate
  (:require [honeysql.core :as sql]
            [honeysql.helpers :as h])
  (:import java.sql.Timestamp
           java.util.Date
           java.util.UUID))

(defn update-yanked
  [version-id yanked?]
  (-> (h/update :crates_versions)
      (h/sset {:yanked yanked?})
      (h/where [:= :id version-id])
      sql/format))

(defn get-crate-by-name
  [crate-name]
  (-> (h/select [:c.id "crate_id"]
                [:c.name "crate_name"])
      (h/from [:crates :c])
      (h/where [:= :c.name crate-name])
      sql/format))

(defn get-crate-join-version
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

(defn create-crate
  [metadata crate-id]
  (-> (h/insert-into :crates)
      (h/columns :id :name)
      (h/values [[crate-id
                  (:name metadata)]])
      sql/format))

(defn create-version
  [metadata crate-id]
  (let [now (new Timestamp (.getTime (new Date)))]
    (-> (h/insert-into :crates_versions)
        (h/columns :id
                   :version
                   :description
                   :yanked
                   :created_at
                   :updated_at
                   :crate_id
                   :document_vectors
                   )
        (h/values [[(UUID/randomUUID)
                    (:vers metadata)
                    (:description metadata)
                    (:yanked metadata false)
                    now
                    now
                    crate-id
                    (sql/raw (str
                              "("
                              "to_tsvector(?) || "
                              "to_tsvector(?)"
                              ")")
                             )]])
        sql/format
        (conj (:name metadata))
        (conj (:description metadata)))))
