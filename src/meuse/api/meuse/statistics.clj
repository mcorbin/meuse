(ns meuse.api.meuse.statistics
  (:require [meuse.auth.request :as auth-request]
            [meuse.db.public.crate :as public-crate]
            [meuse.db.public.crate-version :as public-crate-version]
            [meuse.db.public.user :as public-user]))

(defn get-stats
  [crate-db crate-version-db user-db request]
  (auth-request/admin-or-tech?-throw request)
  (let [nb-crate (:count (public-crate/count-crates crate-db))
        nb-crate-version (:count (public-crate-version/count-crates-versions
                                  crate-version-db))
        nb-download (:sum (public-crate-version/sum-download-count
                           crate-version-db))
        nb-user (:count (public-user/count-users user-db))]
    {:status 200
     :body {:crates nb-crate
            :crates-versions nb-crate-version
            :downloads nb-download
            :users nb-user}}))
