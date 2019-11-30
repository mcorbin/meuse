(ns meuse.api.meuse.category
  (:require [meuse.api.params :as params]
            [meuse.auth.request :as auth-request]
            [meuse.db.public.category :as public-category]
            [clojure.tools.logging :refer [debug info error]]))

(defn new-category
  [category-db request]
  (params/validate-params request ::new)
  (auth-request/admin?-throw request)
  (info "create category" (get-in request [:body :name]))
  (public-category/create category-db
                          (get-in request [:body :name])
                          (get-in request [:body :description]))
  {:status 200
   :body {:ok true}})

(defn list-categories
  [category-db request]
  (auth-request/admin-or-tech?-throw request)
  (info "get categories")
  {:status 200
   :body {:categories (->> (public-category/get-categories category-db)
                           (map #(clojure.set/rename-keys
                                  % {:categories/id :id
                                     :categories/name :name
                                     :categories/description :description})))}})

(defn update-category
  [category-db request]
  (auth-request/admin?-throw request)
  (params/validate-params request ::update)
  (info "update category")
  (let [category-name (get-in request [:route-params :name])]
    (public-category/update-category category-db
                                     category-name
                                     (:body request))
    {:status 200
     :body {:ok true}}))

