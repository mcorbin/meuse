(ns meuse.api.meuse.category
  (:require [meuse.api.meuse.http :refer [meuse-api!]]
            [meuse.api.params :as params]
            [meuse.auth.request :as auth-request]
            [meuse.db.category :as db-category]
            [clojure.tools.logging :refer [debug info error]]))

(defmethod meuse-api! :new-category
  [request]
  (params/validate-params request ::new)
  (auth-request/admin?-throw request)
  (info "create category" (get-in request [:body :name]))
  (db-category/create (:database request)
                      (get-in request [:body :name])
                      (get-in request [:body :description]))
  {:status 200
   :body {:ok true}})

(defmethod meuse-api! :list-categories
  [request]
  (auth-request/admin-or-tech?-throw request)
  (info "get categories")
  {:status 200
   :body {:categories (->> (db-category/get-categories (:database request))
                           (map #(clojure.set/rename-keys
                                  % {:category-id :id
                                     :category-name :name
                                     :category-description :description})))}})


