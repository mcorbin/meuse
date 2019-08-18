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
        tsvector (cond-> "(to_tsvector(?) || to_tsvector(?)"
                   (seq keywords) (str "|| to_tsvector(?)")
                   (seq categories) (str "|| to_tsvector(?)")
                   true (str ")"))]
    (clojure.tools.logging/info "tsvector" tsvector "keywords" keywords "categories" categories)
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
            (conj (:name metadata))
            (conj (:description metadata "")))
      (seq keywords) (conj (string/join " " keywords))
      (seq categories) (conj (string/join " " categories)))))
