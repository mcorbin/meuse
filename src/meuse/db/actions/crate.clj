(ns meuse.db.actions.crate
  "Manages crates in the database."
  (:require [meuse.db.actions.category :as category-db]
            [meuse.db.actions.crate-category :as crate-category]
            [meuse.db.queries.crate :as crate-queries]
            [meuse.db.queries.crate-user :as crate-user-queries]
            [meuse.db.queries.crate-version :as crate-version-queries]
            [exoscale.ex :as ex]
            [next.jdbc :as jdbc])
  (:import java.util.UUID))

(defn by-name
  "Takes a crate name and returns the crate if it exists."
  [db-tx crate-name]
  (-> (jdbc/execute! db-tx (crate-queries/by-name crate-name))
      first))

(defn by-name-and-version
  "Takes a crate name and version and returns the crate version if it exists."
  [db-tx crate-name crate-version]
  (-> (jdbc/execute! db-tx (crate-queries/by-name-join-version
                            crate-name
                            crate-version))
      first))

(defn get-crates-and-versions
  "Returns all crates with their versions."
  [database]
  (->> (jdbc/execute! database (crate-queries/get-crates-and-versions))))

(defn get-crate-and-versions
  "Returns a crate with its versions"
  [database crate-name]
  (let [result (->> (jdbc/execute! database (crate-queries/get-crate-and-versions
                                             crate-name)))]
    (if (seq result)
      result
      (throw (ex/ex-not-found (format "the crate %s does not exist" crate-name))))))

(defn get-crates-for-category
  "Returns crates from a category."
  [database category-name]
  (jdbc/with-transaction [db-tx database]
    (if-let [category (category-db/by-name db-tx category-name)]
      (->> (jdbc/execute! db-tx (crate-queries/get-crates-for-category
                                 (:categories/id category))))
      (throw (ex/ex-not-found
              (format "the category %s does not exist" category-name))))))

(defn create
  "Creates a crate in the database."
  [database metadata user-id]
  (jdbc/with-transaction [db-tx database]
    (let [crate (by-name db-tx (:name metadata))
          crate-version (by-name-and-version db-tx
                                             (:name metadata)
                                             (:vers metadata))]
      (if crate
          ;; the crate exists, let's check the version
          (do
            (when crate-version
              (throw (ex/ex-incorrect (format "release %s for crate %s already exists"
                                              (:vers metadata)
                                              (:name metadata)))))
            ;; the user should own the crate
            (when-not (-> (jdbc/execute! db-tx (crate-user-queries/by-crate-and-user
                                                (:crates/id crate)
                                                user-id))
                          first)
              (throw (ex/ex-forbidden "the user does not own the crate")))
            ;; insert the new version
            (jdbc/execute! db-tx (crate-version-queries/create
                                  metadata
                                  (:crates/id crate)))
            (crate-category/create-categories db-tx
                                              (:crates/id crate)
                                              (:categories metadata)))
        ;; the crate does not exist
        (let [crate-id (UUID/randomUUID)
              created-crate (crate-queries/create metadata crate-id)
              created-version (crate-version-queries/create metadata
                                                            crate-id)]
          (jdbc/execute! db-tx created-crate)
          (jdbc/execute! db-tx created-version)
          (crate-category/create-categories db-tx
                                            crate-id
                                            (:categories metadata))
          ;; the user should own the crate
          (jdbc/execute! db-tx (crate-user-queries/create
                                crate-id
                                user-id)))))))

(defn get-crates-range
  "Get crates with the number of versions per crates. The crates will start
  by the prefix.
  Returns the rows between start and end (the crates are sorted by name)."
  [database start end prefix]
  (->> (jdbc/execute! database (crate-queries/get-crates-range start end prefix))))

(defn count-crates
  "Count the number of crates"
  [database]
  (-> (jdbc/execute! database (crate-queries/count-crates))
      first))

(defn count-crates-prefix
  "Count the number of crates starting by a prefix"
  [database prefix]
  (-> (jdbc/execute! database (crate-queries/count-crates-prefix prefix))
      first))
