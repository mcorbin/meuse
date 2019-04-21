(ns meuse.db.crate
  (:require [clojure.java.jdbc :as jdbc]
            [clojure.tools.logging :refer [debug info error]]
            [honeysql.core :as sql]
            [honeysql.helpers :as h])
  (:import java.sql.Timestamp
           java.util.Date
           java.util.UUID))

(defn get-crate
  [metadata]
  (-> (h/select :c.* :v.*)
      (h/from [:crates :c])
      (h/left-join [:crate_versions :v] [:and
                                         [:= :c.id :v.crate_id]
                                         [:= :v.version (:vers metadata)]])
      (h/where [:= :c.name (:name metadata)])
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

(defn new-crate
  [request {:keys [metadata]}]
  (jdbc/with-db-transaction [db-tx (:database request)]
    (if-let [crate (-> (jdbc/query db-tx (get-crate metadata)) first)]
      ;; the crate exists, let's check the version
      (do
        (when (:version crate)
          (throw (ex-info (format "release %s for crate %s already exists"
                                  (:name metadata)
                                  (:vers metadata))
                          {})))
        ;; insert the new version
        (jdbc/execute! db-tx (create-version-req metadata (:id crate))))
      ;; the crate does not exist
      (let [crate-id (UUID/randomUUID)
            create-crate (create-crate-req metadata crate-id)
            create-version (create-version-req metadata crate-id)]
        (jdbc/execute! db-tx create-crate)
        (jdbc/execute! db-tx create-version)))))

(comment
  (-> (h/insert-into :crate_versions)
      (h/columns :id
                 :version
                 :description
                 :yanked
                 :created_at
                 :crate_name)
      (h/values [["foo" "abar" (sql/call "foo")]])
      sql/format)
  )
