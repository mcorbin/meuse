(ns meuse.api.meuse.token
  (:require [meuse.api.meuse.http :refer [meuse-api!]]
            [meuse.api.params :as params]
            [meuse.auth.password :as auth-password]
            [meuse.auth.request :as auth-request]
            [meuse.db.public.token :as public-token]
            [meuse.db.public.user :as public-user]
            [clojure.tools.logging :refer [debug info error]]))

(defn delete-token
  [token-db request]
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
              {:type :meuse.error/forbidden})))
    (info (format "deleting token %s for user %s" token-name user-name))
    (public-token/delete token-db
                         user-name
                         token-name)
    {:status 200
     :body {:ok true}}))

(defn create-token
  [user-db token-db request]
  (params/validate-params request ::create)
  (let [{:keys [name user password] :as body} (:body request)]
    (if-let [db-user (public-user/by-name user-db user)]
      (do (when-not (:user-active db-user)
            (throw (ex-info "user is not active"
                            {:type :meuse.error/forbidden})))
          (auth-password/check password (:user-password db-user))
          (info (format "creating token %s for user %s"
                        name
                        user))
          {:status 200
           :body {:token (public-token/create token-db
                                              (select-keys
                                               (:body request)
                                               [:name :user :validity]))}})
      (throw (ex-info (format "the user %s does not exist" user)
                      {:type :meuse.error/not-found})))))

(defn list-tokens
  [token-db request]
  (params/validate-params request ::list)
  (let [request-user (get-in request [:params :user])]
    (if request-user
      (auth-request/admin?-throw request)
      (auth-request/admin-or-tech?-throw request))
    (info "list tokens")
    (let [user-name (or request-user (get-in request [:auth :user-name]))
          tokens (->> (public-token/by-user token-db user-name)
                      (map #(select-keys % [:token-id
                                            :token-name
                                            :token-created-at
                                            :token-expired-at]))
                      (map #(clojure.set/rename-keys
                             %
                             {:token-id :id
                              :token-name :name
                              :token-created-at :created-at
                              :token-expired-at :expired-at})))]
      {:status 200
       :body {:tokens tokens}})))





