(ns meuse.db.category
  "Manage categories in the database"
  (:require [meuse.db.queries :as queries]
            [meuse.db.crate :as crate]
            [clojure.java.jdbc :as jdbc]
            [clojure.tools.logging :refer [debug info error]]))

(defn get-category
  "Takes a db transaction and a category name, and get this category
  in the database"
  [db-tx category-name]
  (-> (jdbc/query db-tx (queries/get-category category-name))
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
    (if-let [category (get-category db-tx category-name)]
      (throw (ex-info (format "the category %s already exists"
                              category-name)
                      {:status 400}))
      (jdbc/execute! db-tx (queries/create-category category-name description)))))

(defn get-crate-category
  "Get the crate/category relation for a crate and a category."
  [database crate-id category-id]
  (-> (jdbc/query database (queries/get-crate-category crate-id category-id))
      first
      (clojure.set/rename-keys {:crate_id :crate-id
                                :category_id :category-id})))

(defn add-crate-category
  "Assign a crate to a category."
  [db-tx crate-name category-name]
  (if-let [category (get-category db-tx category-name)]
    (if-let [crate (crate/get-crate db-tx crate-name)]
      ;; adding an existing category to a crate is a noop
      (when-not (get-crate-category db-tx
                                    (:crate-id crate)
                                    (:category-id category))
        (jdbc/execute! db-tx (queries/add-crate-category
                              (:crate-id crate)
                              (:category-id category))))
      (throw (ex-info (format "the crate %s does not exist"
                              crate-name)
                      {})))
    (throw (ex-info (format "the category %s does not exist"
                            category-name)
                    {}))))
