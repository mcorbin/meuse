(ns meuse.api.meuse.user
  (:require [meuse.api.meuse.http :refer [meuse-api!]]
            [meuse.api.params :as params]
            [meuse.db.user :as db-user]
            [clojure.tools.logging :refer [debug info error]]))

(defmethod meuse-api! :new-user
  [request]
  (params/validate-params request ::new)
  (info "create user" (get-in request [:body :name]))
  (db-user/create-user (:database request)
                       (:body request))
  {:status 200})

(defmethod meuse-api! :delete-user
  [request]
  (params/validate-params request ::delete)
  (info "delete user" (get-in request [:body :name]))
  (db-user/delete-user (:database request)
                       (get-in request [:body :name]))
  {:status 200})


