(ns meuse.db.queries.crate-version
  (:require [honeysql.core :as sql]
            [honeysql.helpers :as h]
            [clojure.string :as string])
  (:import java.util.Date
           java.util.UUID
           java.sql.Timestamp))

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
                       :document_vectors)
            (h/values [[(UUID/randomUUID)
                        (:vers metadata)
                        (:description metadata)
                        (:yanked metadata false)
                        now
                        now
                        crate-id
                        (sql/raw tsvector)]])
            sql/format
            (conj (:name metadata)))
      (not (string/blank? (:description metadata))) (conj (:description metadata))
      (seq keywords) (conj (string/join " " keywords))
      (seq categories) (conj (string/join " " categories)))))

(defn last-updated
  [n]
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
      (h/order-by [:v.updated_at :desc])
      (h/limit n)
      sql/format))

(defn count-crates-versions
  []
  (-> (h/select [:%count.* "crates_versions_count"])
      (h/from [:crates_versions :c])
      sql/format))
