(ns meuse.db.crate
  (:require [clojure.java.jdbc :as jdbc]
            [clojure.tools.logging :refer [debug info error]]
            [honeysql.core :as sql]
            [honeysql.helpers :as h]
            [meuse.crate :refer [yanked?->msg]])
  (:import java.sql.Timestamp
           java.util.Date
           java.util.UUID))

(defn update-yanked-req
  [version-id yanked?]
  (-> (h/update :crate_versions)
      (h/sset {:yanked yanked?})
      (h/where [:= :id version-id])
      sql/format))

(defn get-crate-version-req
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

(defn create-crate-req
  [metadata crate-id]
  (-> (h/insert-into :crates)
      (h/columns :id :name)
      (h/values [[crate-id
                  (:name metadata)]])
      sql/format))

(defn create-version-req
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

(defn get-crate-version
  [db-tx crate-name crate-version]
  (-> (jdbc/query db-tx (get-crate-version-req crate-name crate-version))
      first
      (clojure.set/rename-keys {:crate_id :crate-id
                                :crate_name :crate-name
                                :version_id :version-id
                                :version_version :version-version
                                :version_description :version-description
                                :version_yanked :version-yanked
                                :version_created_at :version-created-at
                                :version_updated_at :version-updated-at
                                :version_document_vectors :version-document-vectors
                                :version_crate_id :version-crate-id})))

(defn new-crate
  [request {:keys [metadata]}]
  (jdbc/with-db-transaction [db-tx (:database request)]
    (if-let [crate (get-crate-version db-tx
                                      (:name metadata)
                                      (:vers metadata))]
      ;; the crate exists, let's check the version
      (do
        (when (:version-version crate)
          (throw (ex-info (format "release %s for crate %s already exists"
                                  (:name metadata)
                                  (:vers metadata))
                          {})))
        ;; insert the new version
        (jdbc/execute! db-tx (create-version-req metadata (:crate-id crate))))
      ;; the crate does not exist
      (let [crate-id (UUID/randomUUID)
            create-crate (create-crate-req metadata crate-id)
            create-version (create-version-req metadata crate-id)]
        (jdbc/execute! db-tx create-crate)
        (jdbc/execute! db-tx create-version)))))

(defn update-yank
  [request crate-name crate-version yanked?]
  (info (yanked?->msg yanked?) "crate" crate-name crate-version)
  (jdbc/with-db-transaction [db-tx (:database request)]
    (if-let [crate (get-crate-version db-tx crate-name crate-version)]
      (do
        (when-not (:version-version crate)
          (throw
           (ex-info
            (format "cannot %s the crate: the version does not exist"
                    (yanked?->msg yanked?))
            {:crate-name crate-name
             :crate-version crate-version})))
        (jdbc/execute! db-tx (update-yanked-req (:version-id crate) yanked?)))
      (throw (ex-info (format "cannot %s the crate: the crate does not exist"
                              (yanked?->msg yanked?))
                      {:crate-name crate-name
                       :crate-version crate-version})))))
