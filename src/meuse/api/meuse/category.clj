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
  (db-category/create-category (:database request)
                               (get-in request [:body :name])
                               (get-in request [:body :description]))
  {:status 200})


