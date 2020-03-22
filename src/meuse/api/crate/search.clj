(ns meuse.api.crate.search
  "Search API"
  (:require [meuse.api.params :as params]
            [meuse.db.public.search :as public-search]
            [meuse.log :as log]
            [meuse.semver :as semver]
            [clojure.set :as set]))

(def default-nb-results "10")

(defn get-crate-max-version
  "Get the max version from a list of crate."
  [[_ result]]
  (-> (sort #(semver/compare-versions (:crates_versions/version %1)
                                      (:crates_versions/version %2))
            result)
      last))

(defn format-version
  "Updates and selects a crate version for the Cargo API."
  [version]
  (-> (set/rename-keys version {:crates/name :name
                                :crates_versions/version :max_version
                                :crates_versions/description :description})
      (select-keys [:name :max_version :description])
      (update :description #(if % % ""))))

(defn format-search-result
  "Takes a search result from the db, format it for the Cargo API."
  [result]
  (->> (group-by :crates/id result)
       (map get-crate-max-version)
       (map format-version)))

(defn search
  [search-db request]
  (params/validate-params request ::search)
  ;; todo: re enable ?
  ;(auth-request/check-admin-tech request)
  (let [{query :q nb-results :per_page} (:params request)
        search-result (->> (public-search/search search-db query)
                           format-search-result
                           (take (Integer/parseInt
                                  (or nb-results default-nb-results))))]
    (log/info (log/req-ctx request)
              "search crates with query" query)
    {:status 200
     :body {:crates search-result
            :meta {:total (count search-result)}}}))

