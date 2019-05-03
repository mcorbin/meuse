(ns meuse.db.queries.search
  (:require [honeysql.core :as sql]
            [honeysql.helpers :as h]))

(defn search-crates
  [query]
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
      (h/join [:crate_versions :v] [:and
                                    [:= :c.id :v.crate_id]
                                    (sql/raw "document_vectors @@ to_tsquery(?)")])
      sql/format
      (conj query)))
