(ns meuse.db.category
  "Manage categories in the database"
  (:require [meuse.db.queries.category :as queries]
            [clojure.java.jdbc :as jdbc]
            [clojure.tools.logging :refer [debug info error]]))

(defn get-category-by-name
  "Takes a db transaction and a category name, and get this category
  from the database"
  [db-tx category-name]
  (-> (jdbc/query db-tx (queries/get-category
                         [:= :c.name category-name]))
      first
      (clojure.set/rename-keys {:category_id :category-id
                                :category_name :category-name
                                :category_description :category-description})))

(defn create-category
  "Takes a database and a category name, and creates this category
  if it does not already exists."
  [database category-name description]
  (info "create category" category-name)
  (jdbc/with-db-transaction [db-tx database]
    (if-let [category (get-category-by-name db-tx category-name)]
      (throw (ex-info (format "the category %s already exists"
                              category-name)
                      {:status 400}))
      (jdbc/execute! db-tx (queries/create-category category-name description)))))
