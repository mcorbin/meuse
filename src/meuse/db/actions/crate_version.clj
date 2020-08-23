(ns meuse.db.actions.crate-version
  (:require [meuse.db.actions.crate :as crate-db]
            [meuse.db.queries.crate-version :as crate-version-queries]
            [meuse.log :as log]
            [meuse.message :refer [yanked?->msg]]
            [exoscale.ex :as ex]
            [next.jdbc :as jdbc]))

(defn update-yank
  "Updates the `yanked` field in the database for a crate version."
  [database crate-name crate-version yanked?]
  (log/info {} (yanked?->msg yanked?) "crate" crate-name crate-version)
  (jdbc/with-transaction [db-tx database]
    (if (crate-db/by-name db-tx crate-name)
      (if-let [crate-version (crate-db/by-name-and-version db-tx crate-name crate-version)]
        (do
          (when (= yanked? (:crates_versions/yanked crate-version))
            (throw
             (ex/ex-incorrect
              (format "cannot %s the crate: crate state is already %s"
                      (yanked?->msg yanked?)
                      (yanked?->msg yanked?))
              {:crate-name crate-name
               :crate-version crate-version})))
          (jdbc/execute! db-tx (crate-version-queries/update-yanked
                                (:crates_versions/id crate-version)
                                yanked?)))
        (throw
             (ex/ex-not-found
              (format "cannot %s the crate: the version does not exist"
                      (yanked?->msg yanked?))
              {:crate-name crate-name
               :crate-version crate-version})))
      (throw (ex/ex-not-found (format "cannot %s the crate: the crate does not exist"
                                      (yanked?->msg yanked?))
                              {:crate-name crate-name
                               :crate-version crate-version})))))

(defn last-updated
  "Get the last n updated versions"
  [database n]
  (->> (jdbc/execute! database (crate-version-queries/last-updated n))))

(defn count-crates-versions
  "Count crate versions"
  [database]
  (-> (jdbc/execute! database (crate-version-queries/count-crates-versions))
      first))

(defn inc-download
  "Increment the download count for a crate name and version"
  [database crate-name version]
  (if-let [crate (crate-db/by-name-and-version database crate-name version)]
    (-> (jdbc/execute! database (crate-version-queries/inc-download
                                 (:crates_versions/id crate))))
    (throw (ex/ex-not-found (format "crate %s version %s not found"
                                    crate-name version)))))

(defn sum-download-count
  "Returns the number of total download."
  [database]
  (-> (jdbc/execute! database (crate-version-queries/sum-download-count))
      first
      (update :sum #(or % 0))))

(defn top-n-downloads
  "Returns crate versions which has been the most downloaded."
  [database n]
  (jdbc/execute! database (crate-version-queries/top-n-downloads n)))

(defn delete
  "Deletes a crate version in the database."
  [database crate-name version]
  (jdbc/with-transaction [db-tx database]
    (when-let [crate-version (crate-db/by-name-and-version db-tx crate-name version)]
      (jdbc/execute! db-tx (crate-version-queries/delete (:crates_versions/id crate-version))))))
