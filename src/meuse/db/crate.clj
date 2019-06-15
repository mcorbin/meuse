(ns meuse.db.crate
  "Manages crates in the database."
  (:require [clojure.java.jdbc :as jdbc]
            [clojure.tools.logging :refer [debug info error]]
            [meuse.db.queries.crate :as crate-queries]
            [meuse.db.queries.crate-version :as crate-version-queries]
            [meuse.db.queries.category :as category-queries]
            [meuse.db.queries.user :as user-queries]
            [meuse.db.category :as category]
            [meuse.message :refer [yanked?->msg]])
  (:import java.util.UUID))

(defn get-crate-by-name
  "Takes a crate name and returns the crate if it exists."
  [db-tx crate-name]
  (-> (jdbc/query db-tx (crate-queries/get-crate [:= :c.name crate-name]))
      first
      (clojure.set/rename-keys {:crate_id :crate-id
                                :crate_name :crate-name})))

(defn get-crate-category
  "Get the crate/category relation for a crate and a category."
  [db-tx crate-id category-id]
  (-> (jdbc/query db-tx (category-queries/get-crate-category crate-id category-id))
      first
      (clojure.set/rename-keys {:crate_id :crate-id
                                :category_id :category-id})))

(defn get-crate-join-crates-categories
  "Get the crate/category relation for a crate and a category."
  [db-tx crate-id]
  (->> (jdbc/query db-tx (category-queries/get-crate-join-crates-categories crate-id))
       (map #(clojure.set/rename-keys % {:category_id :category-id
                                         :category_name :category-name
                                         :category_description :category-description}))))

(defn create-crate-category
  "Assigns a crate to a category."
  [db-tx crate-id category-name]
  (if-let [category (category/get-category-by-name db-tx category-name)]
    (when-not (get-crate-category db-tx
                                  crate-id
                                  (:category-id category))
      (jdbc/execute! db-tx (category-queries/create-crate-category
                            crate-id
                            (:category-id category))))
    (throw (ex-info (format "the category %s does not exist"
                            category-name)
                    {:status 404}))))

(defn create-crate-categories
  "Creates categories for a crate."
  [db-tx crate-id categories]
  (doseq [category categories]
    (create-crate-category db-tx crate-id category)))

(defn get-crate-and-version
  "Takes a crate name and version and returns the crate version if it exists."
  [db-tx crate-name crate-version]
  (-> (jdbc/query db-tx (crate-version-queries/get-crate-and-version
                         crate-name
                         crate-version))
      first
      (clojure.set/rename-keys {:crate_id :crate-id
                                :crate_name :crate-name
                                :version_id :version-id
                                :version_version :version-version
                                :version_description :version-description
                                :version_yanked :version-yanked
                                :version_created_at :version-created-at
                                :version_updated_at :version-updated-at
                                :version_document_vectors :version-document-vectors
                                :version_crate_id :version-crate-id})))

(defn create-crate
  "Creates a crate in the database."
  [database metadata user-id]
  (jdbc/with-db-transaction [db-tx database]
    (if-let [crate (get-crate-and-version db-tx
                                          (:name metadata)
                                          (:vers metadata))]
      ;; the crate exists, let's check the version
      (do
        (when (:version-version crate)
          (throw (ex-info (format "release %s for crate %s already exists"
                                  (:name metadata)
                                  (:vers metadata))
                          {:status 200})))
        ;; the user should own the crate
        (when-not (-> (jdbc/query db-tx (user-queries/get-crate-user
                                         (:crate-id crate)
                                         user-id))
                      first)
          (throw (ex-info "the user does not own the crate" {:status 403})))
        ;; insert the new version
        (jdbc/execute! db-tx (crate-version-queries/create-version
                              metadata
                              (:crate-id crate)))
        (create-crate-categories db-tx
                                 (:crate-id crate)
                                 (:categories metadata)))
      ;; the crate does not exist
      (let [crate-id (UUID/randomUUID)
            create-crate (crate-queries/create-crate metadata crate-id)
            create-version (crate-version-queries/create-version metadata
                                                                 crate-id)]
        (jdbc/execute! db-tx create-crate)
        (jdbc/execute! db-tx create-version)
        (create-crate-categories db-tx
                                 crate-id
                                 (:categories metadata))
        ;; the user should own the crate
        (jdbc/execute! db-tx (user-queries/create-crate-user
                              crate-id
                              user-id))))))

(defn update-yank
  "Updates the `yanked` field in the database for a crate version."
  [database crate-name crate-version yanked?]
  (info (yanked?->msg yanked?) "crate" crate-name crate-version)
  (jdbc/with-db-transaction [db-tx database]
    (if-let [crate (get-crate-and-version db-tx crate-name crate-version)]
      (do
        (when-not (:version-version crate)
          (throw
           (ex-info
            (format "cannot %s the crate: the version does not exist"
                    (yanked?->msg yanked?))
            {:status 404
             :crate-name crate-name
             :crate-version crate-version})))
        (when (= yanked? (:version-yanked crate))
          (throw
           (ex-info
            (format "cannot %s the crate: crate state is already %s"
                    (yanked?->msg yanked?)
                    (yanked?->msg yanked?))
            {:status 404
             :crate-name crate-name
             :crate-version crate-version})))
        (jdbc/execute! db-tx (crate-version-queries/update-yanked (:version-id crate) yanked?)))
      (throw (ex-info (format "cannot %s the crate: the crate does not exist"
                              (yanked?->msg yanked?))
                      {:status 400
                       :crate-name crate-name
                       :crate-version crate-version})))))
