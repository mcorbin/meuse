(ns meuse.api.crate.search
  "Search API"
  (:require [meuse.api.crate.http :refer (crates-api!)]
            [meuse.db.search :as search-db]
            [clojure.tools.logging :refer [debug info error]]))

(def default-nb-results 10)

(defn get-crate-max-version
  [[_ result]]
  (-> (sort-by :version-version result)
      last))

(defn format-version
  [version]
  (-> (clojure.set/rename-keys version {:crate-name :name
                                        :version-version :max_version
                                        :version-description :description})
      (select-keys [:name :max_version :description])))

(defn format-search-result
  [result]
  (->> (group-by :crate-id result)
       (map get-crate-max-version)
       (map format-version)))

(defmethod crates-api! :search
  [request]
  (let [{query :q nb-results :per_page} (:params request)
        _ (info (:params request))
        search-result (search-db/search (:database request) query)]
    {:status 200
     :body {:crates (take (Integer/parseInt nb-results)
                          (format-search-result search-result))
            :meta {:total (count search-result)}}}))

