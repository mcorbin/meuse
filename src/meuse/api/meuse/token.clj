(ns meuse.api.meuse.token
  (:require [meuse.api.meuse.http :refer [meuse-api!]]
            [meuse.api.params :as params]
            [meuse.auth.password :as auth-password]
            [meuse.db.token :as token-db]
            [meuse.db.user :as user-db]
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
  (let [{:keys [name user password] :as body} (:body request)]
    (if-let [db-user (user-db/get-user-by-name (:database request) user)]
      (do (auth-password/check password (:user-password db-user))
          (info (format "creating token %s for user %s"
                        name
                        user))
          {:status 200
           :body {:token (token-db/create-token (:database request)
                                                (select-keys
                                                 (:body request)
                                                 [:name :user :validity]))}})
      (throw (ex-info (format "the user %s does not exist" user) {:status 400})))))

