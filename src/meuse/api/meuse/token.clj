(ns meuse.api.meuse.token
  (:require [meuse.api.meuse.http :refer [meuse-api!]]
            [meuse.api.params :as params]
            [meuse.db.token :as token-db]
            [clojure.tools.logging :refer [debug info error]]))

(defmethod meuse-api! :delete-token
  [request]
  (params/validate-params request ::delete)
  (let [token-name (get-in request [:body :name])
        user-name (get-in request [:body :user])]
    (info (format "deleting token %s for user %s" token-name user-name))
    (token-db/delete-token (:database request)
                           user-name
                           token-name)
    {:status 200}))

(defmethod meuse-api! :create-token
  [request]
  (params/validate-params request ::create)
  (let [{:keys [name user] :as body} (:body request)]
    (info (format "creating token %s for user %s"
                  name
                  user))
    (token-db/create-token (:database request)
                           (:body request))
    {:status 200}))


