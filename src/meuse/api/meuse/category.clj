(ns meuse.api.meuse.category
  (:require [meuse.api.meuse.http :refer [meuse-api!]]
            [meuse.api.params :as params]
            [meuse.db.category :as db-category]))

(defmethod meuse-api! :new-category
  [request]
  (params/validate-params request ::new)
  (db-category/create-category (:database request)
                               (get-in request [:body :name])
                               (get-in request [:body :description]))
  {:status 200})


