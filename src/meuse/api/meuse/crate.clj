(ns meuse.api.meuse.crate
  (:require [meuse.api.meuse.http :refer [meuse-api!]]
            [meuse.api.params :as params]
            [meuse.auth.password :as auth-password]
            [meuse.auth.request :as auth-request]
            [meuse.db.public.category :as public-category]
            [meuse.db.public.crate :as public-crate]
            [meuse.crate :refer [check]]
            [clojure.set :as set]
            [clojure.tools.logging :refer [debug info error]]))

(defn format-crate
  [[crate-id crate-versions]]
  (let [versions (->> (map  #(set/rename-keys % {:crates_versions/version :version
                                                 :crates_versions/description :description
                                                 :crates_versions/yanked :yanked
                                                 :crates_versions/created_at :created-at
                                                 :crates_versions/updated_at :updated-at})
                            crate-versions)
                      (map #(select-keys %
                                         [:version :description :yanked
                                          :created-at :updated-at])))]
    {:id crate-id
     :name (-> crate-versions first :crates/name)
     :versions versions}))

(defn format-crates
  "Takes a list of crates and versions.
  Groups the versions by crates, and returns the result"
  [crates-versions]
  (->> (group-by :crates/id crates-versions)
       (map format-crate)))

(defn list-crates
  [crate-db request]
  (params/validate-params request ::list)
  (auth-request/admin-or-tech?-throw request)
  (info "list crates")
  (let [crates (if-let [category (get-in request [:params :category])]
                 (public-crate/get-crates-for-category crate-db
                                                       category)
                 (public-crate/get-crates-and-versions crate-db))]
    {:status 200
     :body {:crates (format-crates crates)}}))

(defn get-crate
  [category-db crate-db request]
  (params/validate-params request ::get)
  (auth-request/admin-or-tech?-throw request)
  (let [crate-name (get-in request [:route-params :name])
        _ (info "get crate" crate-name)
        database (:database request)
        crate (-> (public-crate/get-crate-and-versions crate-db
                                                       crate-name)
                  format-crates
                  first)
        categories (->> (public-category/by-crate-id category-db (:id crate))
                        (map #(clojure.set/rename-keys
                               %
                               {:categories/id :id
                                :categories/name :name
                                :categories/description :description})))]
    {:status 200
     :body (assoc crate :categories categories)}))

(defn check-crates
  [crate-db git-object crate-file-store request]
  (auth-request/admin-or-tech?-throw request)
  (info "check crates")
  {:status 200
   :body (check crate-db git-object crate-file-store request)})
