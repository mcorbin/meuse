(ns meuse.db.crate-category
  (:require [clojure.java.jdbc :as jdbc]
            [clojure.tools.logging :refer [debug info error]]
            [meuse.db.queries.crate-category :as crate-category-queries]
            [meuse.db.category :as category]))

(defn by-crate-and-category
  "Get the crate/category relation for a crate and a category."
  [db-tx crate-id category-id]
  (-> (jdbc/query db-tx (crate-category-queries/by-crate-and-category
                         crate-id
                         category-id))
      first
      (clojure.set/rename-keys {:crate_id :crate-id
                                :category_id :category-id})))

(defn create
  "Assigns a crate to a category."
  [db-tx crate-id category-name]
  (if-let [category (category/by-name db-tx category-name)]
    ;; do nothing is the crate already belongs to the category
    (when-not (by-crate-and-category db-tx
                                     crate-id
                                     (:category-id category))
      (jdbc/execute! db-tx (crate-category-queries/create
                            crate-id
                            (:category-id category))))
    (throw (ex-info (format "the category %s does not exist"
                            category-name)
                    {:status 404}))))

(defn create-categories
  "Creates categories for a crate."
  [db-tx crate-id categories]
  (doseq [category categories]
    (create db-tx crate-id category)))

