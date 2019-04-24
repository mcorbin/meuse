(ns meuse.db.crate
  (:require [clojure.java.jdbc :as jdbc]
            [clojure.tools.logging :refer [debug info error]]
            [meuse.db.queries :as queries]
            [meuse.crate :refer [yanked?->msg]])
  (:import java.util.UUID))

(defn get-crate-version
  [db-tx crate-name crate-version]
  (-> (jdbc/query db-tx (queries/get-crate-version crate-name crate-version))
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

(defn new-crate
  [request {:keys [metadata]}]
  (jdbc/with-db-transaction [db-tx (:database request)]
    (if-let [crate (get-crate-version db-tx
                                      (:name metadata)
                                      (:vers metadata))]
      ;; the crate exists, let's check the version
      (do
        (when (:version-version crate)
          (throw (ex-info (format "release %s for crate %s already exists"
                                  (:name metadata)
                                  (:vers metadata))
                          {})))
        ;; insert the new version
        (jdbc/execute! db-tx (queries/create-version metadata (:crate-id crate))))
      ;; the crate does not exist
      (let [crate-id (UUID/randomUUID)
            create-crate (queries/create-crate metadata crate-id)
            create-version (queries/create-version metadata crate-id)]
        (jdbc/execute! db-tx create-crate)
        (jdbc/execute! db-tx create-version)))))

(defn update-yank
  [request crate-name crate-version yanked?]
  (info (yanked?->msg yanked?) "crate" crate-name crate-version)
  (jdbc/with-db-transaction [db-tx (:database request)]
    (if-let [crate (get-crate-version db-tx crate-name crate-version)]
      (do
        (when-not (:version-version crate)
          (throw
           (ex-info
            (format "cannot %s the crate: the version does not exist"
                    (yanked?->msg yanked?))
            {:crate-name crate-name
             :crate-version crate-version})))
        (jdbc/execute! db-tx (queries/update-yanked (:version-id crate) yanked?)))
      (throw (ex-info (format "cannot %s the crate: the crate does not exist"
                              (yanked?->msg yanked?))
                      {:crate-name crate-name
                       :crate-version crate-version})))))
