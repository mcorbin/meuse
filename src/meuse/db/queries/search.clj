(ns meuse.db.queries.search
  (:require [honeysql.core :as sql]
            [honeysql.helpers :as h]))

(defn search-crates
  [query]
  (-> (h/select :c.id
                :c.name
                :v.id
                :v.version
                :v.description
                :v.yanked
                :v.created_at
                :v.updated_at
                :v.download_count
                :v.document_vectors
                :v.crate_id)
      (h/from [:crates :c])
      (h/join [:crates_versions :v] [:and
                                     [:= :c.id :v.crate_id]
                                     (sql/raw "document_vectors @@ to_tsquery(?)")])
      sql/format
      (conj query)))
