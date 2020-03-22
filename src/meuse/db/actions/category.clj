(ns meuse.db.actions.category
  "Manage categories in the database"
  (:require [meuse.db.queries.category :as queries]
            [exoscale.ex :as ex]
            [next.jdbc :as jdbc]))

(defn by-name
  "Takes a db transaction and a category name, and get this category
  from the database"
  [db-tx category-name]
  (-> (jdbc/execute! db-tx (queries/by-name category-name))
      first))

(defn create
  "Takes a database and a category name, and creates this category
  if it does not already exists."
  [database category-name description]
  (jdbc/with-transaction [db-tx database]
    (if (by-name db-tx category-name)
      (throw (ex/ex-incorrect (format "the category %s already exists"
                                      category-name)))
      (jdbc/execute! db-tx (queries/create category-name description)))))

(defn by-crate-id
  "Get the crate/category relation for a crate and a category."
  [database crate-id]
  (->> (jdbc/execute! database (queries/by-crate-id crate-id))))

(defn get-categories
  "get all categories"
  [database]
  (->> (jdbc/execute! database (queries/get-categories))))

(defn update-category
  "update a category"
  [database category-name fields]
  (jdbc/with-transaction [db-tx database]
    (if-let [category (by-name db-tx category-name)]
      (jdbc/execute! db-tx (queries/update-category (:categories/id category)
                                                    fields))
      (throw (ex/ex-not-found (format "the category %s does not exist"
                                      category-name))))))

(defn count-crates-for-categories
  "count the number of crates in categories"
  [database]
  (->> (jdbc/execute! database
                      (queries/count-crates-for-categories))))
