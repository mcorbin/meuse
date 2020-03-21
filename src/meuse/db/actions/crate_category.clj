(ns meuse.db.actions.crate-category
  (:require [meuse.db.actions.category :as category]
            [meuse.db.queries.crate-category :as crate-category-queries]
            [exoscale.ex :as ex]
            [next.jdbc :as jdbc]))

(defn by-crate-and-category
  "Get the crate/category relation for a crate and a category."
  [db-tx crate-id category-id]
  (-> (jdbc/execute! db-tx (crate-category-queries/by-crate-and-category
                            crate-id
                            category-id))
      first))

(defn create
  "Assigns a crate to a category."
  [db-tx crate-id category-name]
  (if-let [category (category/by-name db-tx category-name)]
    ;; do nothing is the crate already belongs to the category
    (when-not (by-crate-and-category db-tx
                                     crate-id
                                     (:categories/id category))
      (jdbc/execute! db-tx (crate-category-queries/create
                            crate-id
                            (:categories/id category))))
    (throw (ex/ex-not-found (format "the category %s does not exist"
                                    category-name)))))

(defn create-categories
  "Creates categories for a crate."
  [db-tx crate-id categories]
  (doseq [category categories]
    (create db-tx crate-id category)))
