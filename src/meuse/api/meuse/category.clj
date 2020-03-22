(ns meuse.api.meuse.category
  (:require [meuse.api.params :as params]
            [meuse.auth.request :as auth-request]
            [meuse.db.public.category :as public-category]
            [meuse.log :as log]
            [clojure.set :as set]))

(defn new-category
  [category-db request]
  (params/validate-params request ::new)
  (auth-request/check-admin request)
  (log/info (log/req-ctx request) "create category" (get-in request [:body :name]))
  (public-category/create category-db
                          (get-in request [:body :name])
                          (get-in request [:body :description]))
  {:status 200
   :body {:ok true}})

(defn list-categories
  [category-db request]
  (auth-request/check-authenticated request)
  (log/info (log/req-ctx request) "get categories")
  {:status 200
   :body {:categories (->> (public-category/get-categories category-db)
                           (map #(set/rename-keys
                                  % {:categories/id :id
                                     :categories/name :name
                                     :categories/description :description})))}})

(defn update-category
  [category-db request]
  (auth-request/check-admin request)
  (params/validate-params request ::update)
  (log/info (log/req-ctx request) "update category")
  (let [category-name (get-in request [:route-params :name])]
    (public-category/update-category category-db
                                     category-name
                                     (:body request))
    {:status 200
     :body {:ok true}}))

