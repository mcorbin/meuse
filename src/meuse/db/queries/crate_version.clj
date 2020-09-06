(ns meuse.db.queries.crate-version
  (:require [cheshire.core :as json]
            [honeysql.core :as sql]
            [honeysql.helpers :as h]
            [next.jdbc.prepare :as p]
            [next.jdbc.result-set :as rs]
            [clojure.string :as string])
  (:import java.util.Date
           java.util.UUID
           java.sql.PreparedStatement
           java.sql.Timestamp
           org.postgresql.util.PGobject))

(defn jsonb
  [value]
  (doto (PGobject.)
    (.setType "jsonb")
    (.setValue (json/generate-string value))))

(extend-protocol p/SettableParameter
  clojure.lang.IPersistentMap
  (set-parameter [^clojure.lang.IPersistentMap v ^PreparedStatement s ^long i]
    (.setObject s i (jsonb v))))
(extend-protocol rs/ReadableColumn
  org.postgresql.util.PGobject
  (read-column-by-index [pgobj metadata idx]
    (let [type (.getType pgobj)
          value (.getValue pgobj)]
      (if (#{"jsonb" "json"} type)
        (json/parse-string value true)
        value))))

(defn update-yanked
  [version-id yanked?]
  (-> (h/update :crates_versions)
      (h/sset {:yanked yanked?
               :updated_at (new Timestamp (.getTime (new Date)))})
      (h/where [:= :id version-id])
      sql/format))

(defn create
  [metadata crate-id]
  (let [now (new Timestamp (.getTime (new Date)))
        keywords (:keywords metadata)
        categories (:categories metadata)
        tsvector (cond-> "(to_tsvector(?)"

                   (not (string/blank? (:description metadata)))
                   (str "|| to_tsvector(?)")

                   (seq keywords)
                   (str "|| to_tsvector(?)")

                   (seq categories)
                   (str "|| to_tsvector(?)")

                   true
                   (str ")"))]
    (cond->
     (-> (h/insert-into :crates_versions)
         (h/columns :id
                    :version
                    :description
                    :yanked
                    :created_at
                    :updated_at
                    :crate_id
                    :metadata
                    :download_count
                    :document_vectors)
         (h/values [[(UUID/randomUUID)
                     (:vers metadata)
                     (:description metadata)
                     (:yanked metadata false)
                     now
                     now
                     crate-id
                     (jsonb metadata)
                     0
                     (sql/raw tsvector)]])
         sql/format
         (conj (:name metadata)))
      (not (string/blank? (:description metadata))) (conj (:description metadata))
      (seq keywords) (conj (string/join " " keywords))
      (seq categories) (conj (string/join " " categories)))))

(defn count-crates-versions
  []
  (-> (h/select :%count.*)
      (h/from [:crates_versions :c])
      sql/format))

(defn inc-download
  [crate-version-id]
  ["UPDATE crates_versions SET download_count = download_count + 1 WHERE id = ?"
   crate-version-id])

(defn sum-download-count
  []
  (-> (h/select :%sum.download_count)
      (h/from [:crates_versions :c])
      sql/format))

(defn first-n-order-by
  [n order-by]
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
      (h/order-by order-by)
      (h/limit n)
      sql/format))

(defn last-updated
  [n]
  (first-n-order-by n [:v.updated_at :desc]))

(defn top-n-downloads
  [n]
  (first-n-order-by n [:v.download_count :desc]))

(defn delete
  [id]
  (-> (h/delete-from :crates_versions)
      (h/where [:= :id id])
      (sql/format)))
