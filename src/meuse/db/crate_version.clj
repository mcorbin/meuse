(ns meuse.db.crate-version
  (:require [clojure.java.jdbc :as jdbc]
            [clojure.tools.logging :refer [debug info error]]
            [meuse.db.crate :as crate-db]
            [meuse.db.queries.crate-version :as crate-version-queries]
            [meuse.message :refer [yanked?->msg]])
  (:import java.util.UUID))

(defn update-yank
  "Updates the `yanked` field in the database for a crate version."
  [database crate-name crate-version yanked?]
  (info (yanked?->msg yanked?) "crate" crate-name crate-version)
  (jdbc/with-db-transaction [db-tx database]
    (if-let [crate (crate-db/get-crate-and-version db-tx crate-name crate-version)]
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
