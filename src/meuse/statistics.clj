(ns meuse.statistics
  (:require [meuse.db.public.crate :as public-crate]
            [meuse.db.public.crate-version :as public-crate-version]
            [meuse.db.public.user :as public-user]
            [meuse.metric :as metric]
            [mount.core :refer [defstate]]))

(defn nb-crates-total
  [crate-db]
  (:count (public-crate/count-crates crate-db)))

(defn nb-crates-versions-total
  [crate-version-db]
  (:count (public-crate-version/count-crates-versions
           crate-version-db)))

(defn nb-download-total
  [crate-version-db]
  (:sum (public-crate-version/sum-download-count
         crate-version-db)))

(defn nb-users-total
  [user-db]
  (:count (public-user/count-users user-db)))

(defn get-stats
  [crate-db crate-version-db user-db]
  (let [nb-crate (nb-crates-total crate-db)
        nb-crate-version (nb-crates-versions-total crate-version-db)
        nb-download (nb-download-total crate-version-db)
        nb-user (nb-users-total user-db)]
    {:crates nb-crate
     :crates-versions nb-crate-version
     :downloads nb-download
     :users nb-user}))

(defstate stats-reporter
  :start (do
           (metric/create-gauge! ::crates_total
                                 {}
                                 (fn []
                                   (nb-crates-total public-crate/crate-db)
                                   ))
           (metric/create-gauge! ::crates_versions_total
                                 {}
                                 (fn []
                                   (nb-crates-versions-total public-crate-version/crate-version-db)))
           (metric/create-gauge! ::downloads_total
                                 {}
                                 (fn []
                                   (nb-download-total public-crate-version/crate-version-db)))
           (metric/create-gauge! ::users_total
                                 {}
                                 (fn []
                                   (nb-users-total public-user/user-db)))))
