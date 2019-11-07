(ns meuse.db.actions.search
  "Search crates in the database."
  (:require [meuse.db.queries.search :as search-queries]
            [clojure.java.jdbc :as jdbc]
            [clojure.string :as string]
            [clojure.tools.logging :refer [debug info error]]))

(defn format-query-string
  "Takes a string, format it to be usable for search."
  [query-string]
  (->> (string/split query-string #" ")
       (string/join " | ")))

(defn search
  "Takes a query string, and returns crates (with their versions) matching the
  query."
  [database query-string]
  (->> (jdbc/query database
                   (search-queries/search-crates (format-query-string query-string)))
       (map #(clojure.set/rename-keys
              %
              {:crate_id :crate-id
               :crate_name :crate-name
               :version_id :version-id
               :version_version :version-version
               :version_description :version-description
               :version_yanked :version-yanked
               :version_created_at :version-created-at
               :version_updated_at :version-updated-at
               :version_document_vectors :version-document-vectors
               :version_crate_id :version-crate-id}))))
