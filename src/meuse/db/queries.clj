(ns meuse.db.queries
  (:require [honeysql.core :as sql]
            [honeysql.helpers :as h])
  (:import java.sql.Timestamp
           java.util.Date
           java.util.UUID))

(defn update-yanked
  [version-id yanked?]
  (-> (h/update :crate_versions)
      (h/sset {:yanked yanked?})
      (h/where [:= :id version-id])
      sql/format))

(defn get-crate-version
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
      (h/left-join [:crate_versions :v] [:and
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
    (-> (h/insert-into :crate_versions)
        (h/columns :id
                   :version
                   :description
                   :yanked
                   :created_at
                   :updated_at
                   :document_vectors
                   :crate_id)
        (h/values [[(UUID/randomUUID)
                    (:vers metadata)
                    (:description metadata)
                    (:yanked metadata false)
                    now
                    now
                    (sql/raw (format
                              (str
                               "("
                               "to_tsvector('%s') || "
                               "to_tsvector('%s')"
                               ")")
                              (:name metadata)
                              (:description metadata)
                              ))
                    crate-id]])
        sql/format)))

