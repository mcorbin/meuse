(ns meuse.db.actions.search
  "Search crates in the database."
  (:require [meuse.db.queries.search :as search-queries]
            [next.jdbc :as jdbc]
            [clojure.string :as string]))

(defn format-query-string
  "Takes a string, format it to be usable for search."
  [query-string]
  (->> (string/split query-string #" ")
       (string/join " | ")))

(defn search
  "Takes a query string, and returns crates (with their versions) matching the
  query."
  [database query-string]
  (->> (jdbc/execute! database
                      (search-queries/search-crates (format-query-string query-string)))))
