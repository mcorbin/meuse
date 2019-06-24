(ns meuse.api.meuse.token
  (:require [meuse.api.meuse.http :refer [meuse-api!]]
            [meuse.api.params :as params]
            [meuse.auth.password :as auth-password]
            [meuse.auth.request :as auth-request]
            [meuse.db.token :as token-db]
            [meuse.db.user :as user-db]
            [clojure.tools.logging :refer [debug info error]]))

(defmethod meuse-api! :delete-token
  [request]
  (params/validate-params request ::delete)
  (let [token-name (get-in request [:body :name])
        user-name (get-in request [:body :user])
        auth-user-name (get-in request [:auth :user-name])]
    ;; the user or an admin can delete a token
    (when-not (or (= (get-in request [:auth :user-name])
                     user-name)
                  (auth-request/admin? request))
      (throw (ex-info
              (format "user %s cannot delete token for %s" auth-user-name user-name)
              {:status 403})))
    (info (format "deleting token %s for user %s" token-name user-name))
    (token-db/delete (:database request)
                     user-name
                     token-name)
    {:status 200
     :body {:ok true}}))

(defmethod meuse-api! :create-token
  [request]
  (params/validate-params request ::create)
  (let [{:keys [name user password] :as body} (:body request)]
    (if-let [db-user (user-db/by-name (:database request) user)]
      (do (auth-password/check password (:user-password db-user))
          (info (format "creating token %s for user %s"
                        name
                        user))
          {:status 200
           :body {:token (token-db/create (:database request)
                                          (select-keys
                                           (:body request)
                                           [:name :user :validity]))}})
      (throw (ex-info (format "the user %s does not exist" user) {:status 400})))))


