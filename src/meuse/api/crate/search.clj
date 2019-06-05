(ns meuse.api.crate.search
  "Search API"
  (:require [meuse.api.crate.http :refer (crates-api!)]
            [meuse.auth.request :as auth-request]
            [meuse.db.search :as search-db]
            [meuse.api.params :as params]
            [meuse.semver :as semver]
            [clojure.tools.logging :refer [debug info error]]))

(def default-nb-results "10")

(defn get-crate-max-version
  "Get the max version from a list of crate."
  [[_ result]]
  (-> (sort #(semver/compare-versions (:version-version %1)
                                      (:version-version %2))
            result)
      last))

(defn format-version
  "Updates and selects a crate version for the Cargo API."
  [version]
  (-> (clojure.set/rename-keys version {:crate-name :name
                                        :version-version :max_version
                                        :version-description :description})
      (select-keys [:name :max_version :description])))

(defn format-search-result
  "Takes a search result from the db, format it for the Cargo API."
  [result]
  (->> (group-by :crate-id result)
       (map get-crate-max-version)
       (map format-version)))

(defmethod crates-api! :search
  [request]
  (params/validate-params request ::search)
  (auth-request/admin-or-tech?-throw request)
  (let [{query :q nb-results :per_page} (:params request)
        search-result (->> (search-db/search (:database request) query)
                           format-search-result
                           (take (Integer/parseInt
                                  (or nb-results default-nb-results))))]
    (info "search crate with query" query)
    {:status 200
     :body {:crates search-result
            :meta {:total (count search-result)}}}))

