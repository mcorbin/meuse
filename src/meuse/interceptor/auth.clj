(ns meuse.interceptor.auth
  (:require [meuse.api.crate.http :as crate-http]
            [meuse.api.meuse.http :as meuse-http]
            [meuse.auth.request :as auth-request]
            [meuse.auth.frontend :as auth-frontend]))

(defmulti check-auth! (fn [request _] (:subsystem request)))

(defmethod check-auth! :meuse.api.crate.http
  [request {:keys [token-db]}]
  (if (crate-http/skip-auth (:action request))
    request
    (auth-request/check-user token-db request)))

(defmethod check-auth! :meuse.api.mirror.http
  [request _]
  request)

(defmethod check-auth! :meuse.api.public.http
  [request _]
  request)

(defmethod check-auth! :meuse.api.meuse.http
  [request {:keys [token-db]}]
  (if (meuse-http/skip-auth (:action request))
    request
    (auth-request/check-user token-db request)))

(def front-actions-no-cookie #{:loginpage :login :static :logout})

(defmethod check-auth! :meuse.front.http
  [request {:keys [token-db user-db key-spec]}]
  (cond
    (front-actions-no-cookie (:action request))
    request

    :else
    (do (auth-frontend/valid-cookie? user-db key-spec request)
        request)))

(defn auth-request
  [token-db user-db key-spec]
  {:name ::auth-request
   :enter
   (fn [{:keys [request] :as ctx}]
     (assoc ctx :request (check-auth! request
                                      {:token-db token-db
                                       :user-db user-db
                                       :key-spec key-spec})))})
