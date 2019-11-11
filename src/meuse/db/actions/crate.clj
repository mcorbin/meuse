(ns meuse.db.actions.crate
  "Manages crates in the database."
  (:require [meuse.db.actions.category :as category-db]
            [meuse.db.actions.crate-category :as crate-category]
            [meuse.db.queries.crate :as crate-queries]
            [meuse.db.queries.crate-user :as crate-user-queries]
            [meuse.db.queries.crate-version :as crate-version-queries]
            [clojure.java.jdbc :as jdbc]
            [clojure.tools.logging :refer [debug info error]])
  (:import java.util.UUID))

(def db-renaming
  {:crate_id :crate-id
   :crate_name :crate-name
   :version_id :version-id
   :version_version :version-version
   :version_description :version-description
   :version_yanked :version-yanked
   :version_created_at :version-created-at
   :version_updated_at :version-updated-at
   :version_document_vectors :version-document-vectors
   :version_crate_id :version-crate-id})

(defn by-name
  "Takes a crate name and returns the crate if it exists."
  [db-tx crate-name]
  (-> (jdbc/query db-tx (crate-queries/by-name crate-name))
      first
      (clojure.set/rename-keys db-renaming)))

(defn by-name-and-version
  "Takes a crate name and version and returns the crate version if it exists."
  [db-tx crate-name crate-version]
  (-> (jdbc/query db-tx (crate-queries/by-name-join-version
                         crate-name
                         crate-version))
      first
      (clojure.set/rename-keys db-renaming)))

(defn get-crates-and-versions
  "Returns all crates with their versions."
  [database]
  (->> (jdbc/query database (crate-queries/get-crates-and-versions))
       (map #(clojure.set/rename-keys % db-renaming))))

(defn get-crate-and-versions
  "Returns a crate with its versions"
  [database crate-name]
  (let [result (->> (jdbc/query database (crate-queries/get-crate-and-versions
                                          crate-name))
                    (map #(clojure.set/rename-keys % db-renaming)))]
    (if (seq result)
      result
      (throw (ex-info (format "the crate %s does not exist" crate-name)
                      {:type :meuse.error/not-found})))))

(defn get-crates-for-category
  "Returns crates from a category."
  [database category-name]
  (jdbc/with-db-transaction [db-tx database]
    (if-let [category (category-db/by-name db-tx category-name)]
      (->> (jdbc/query database (crate-queries/get-crates-for-category
                                 (:category-id category)))
           (map #(clojure.set/rename-keys % db-renaming)))
      (throw (ex-info (format "the category %s does not exist" category-name)
                      {:type :meuse.error/not-found})))))

(defn create
  "Creates a crate in the database."
  [database metadata user-id]
  (jdbc/with-db-transaction [db-tx database]
    (if-let [crate (by-name-and-version db-tx
                                        (:name metadata)
                                        (:vers metadata))]
      ;; the crate exists, let's check the version
      (do
        (when (:version-version crate)
          (throw (ex-info (format "release %s for crate %s already exists"
                                  (:vers metadata)
                                  (:name metadata))
                          {:type :meuse.error/incorrect})))
        ;; the user should own the crate
        (when-not (-> (jdbc/query db-tx (crate-user-queries/by-crate-and-user
                                         (:crate-id crate)
                                         user-id))
                      first)
          (throw (ex-info "the user does not own the crate"
                          {:type :meuse.error/forbidden})))
        ;; insert the new version
        (jdbc/execute! db-tx (crate-version-queries/create
                              metadata
                              (:crate-id crate)))
        (crate-category/create-categories db-tx
                                          (:crate-id crate)
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
                              user-id))))))
