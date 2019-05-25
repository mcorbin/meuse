(ns meuse.api.meuse.token
  (:require [meuse.api.meuse.http :refer [meuse-api!]]
            [meuse.api.params :as params]
            [meuse.db.token :as token-db]
            [clojure.tools.logging :refer [debug info error]]))

(defmethod meuse-api! :delete-token
  [request]
  (params/validate-params request ::delete)
  (let [token-name (get-in request [:body :name])
        user-name (get-in request [:body :user-name])]
    (info (format "deleting token %s for user %s" token-name user-name))
    (token-db/delete-token (:database request)
                           user-name
                           token-name)
    {:status 200}))


