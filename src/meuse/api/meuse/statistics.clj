(ns meuse.api.meuse.statistics
  (:require [meuse.auth.request :as auth-request]
            [meuse.db.public.crate :as public-crate]
            [meuse.db.public.crate-version :as public-crate-version]
            [meuse.db.public.user :as public-user]
            [meuse.statistics :as statistics]))

(defn get-stats
  [crate-db crate-version-db user-db request]
  (auth-request/check-authenticated request)
  {:status 200
   :body (statistics/get-stats crate-db crate-version-db user-db)})
