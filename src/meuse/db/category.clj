(ns meuse.db.category
  "Manage categories in the database"
  (:require [meuse.db.queries.category :as queries]
            [clojure.java.jdbc :as jdbc]
            [clojure.tools.logging :refer [debug info error]]))

(defn by-name
  "Takes a db transaction and a category name, and get this category
  from the database"
  [db-tx category-name]
  (-> (jdbc/query db-tx (queries/by-name category-name))
      first
      (clojure.set/rename-keys {:category_id :category-id
                                :category_name :category-name
                                :category_description :category-description})))

(defn create
  "Takes a database and a category name, and creates this category
  if it does not already exists."
  [database category-name description]
  (info "create category" category-name)
  (jdbc/with-db-transaction [db-tx database]
    (if-let [category (by-name db-tx category-name)]
      (throw (ex-info (format "the category %s already exists"
                              category-name)
                      {:type :meuse.error/incorrect}))
      (jdbc/execute! db-tx (queries/create category-name description)))))

(defn by-crate-id
  "Get the crate/category relation for a crate and a category."
  [db-tx crate-id]
  (->> (jdbc/query db-tx (queries/by-crate-id crate-id))
       (map #(clojure.set/rename-keys % {:category_id :category-id
                                         :category_name :category-name
                                         :category_description :category-description}))))

(defn get-categories
  "get all categories"
  [database]
  (->> (jdbc/query database (queries/get-categories))
       (map #(clojure.set/rename-keys % {:category_id :category-id
                                         :category_name :category-name
                                         :category_description :category-description}))))

(defn update-category
  "update a category"
  [database category-name fields]
  (jdbc/with-db-transaction [db-tx database]
    (if-let [category (by-name db-tx category-name)]
      (jdbc/execute! db-tx (queries/update-category (:category-id category)
                                                    fields))
      (throw (ex-info (format "the category %s does not exist"
                              category-name)
                      {:type :meuse.error/not-found})))))
