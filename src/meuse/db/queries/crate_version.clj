(ns meuse.db.queries.crate-version
  (:require [honeysql.core :as sql]
            [honeysql.helpers :as h])
  (:import java.util.Date
           java.util.UUID
           java.sql.Timestamp))

(defn update-yanked
  [version-id yanked?]
  (-> (h/update :crates_versions)
      (h/sset {:yanked yanked?})
      (h/where [:= :id version-id])
      sql/format))

(defn create
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
                   :document_vectors)
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
                              ")"))]])
        sql/format
        (conj (:name metadata))
        (conj (:description metadata)))))
