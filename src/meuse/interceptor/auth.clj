(ns meuse.interceptor.auth
  (:require [meuse.api.crate.http :as crate-http]
            [meuse.api.meuse.http :as meuse-http]
            [meuse.api.mirror.http :as mirror-http]
            [meuse.auth.request :as auth-request]
            [meuse.auth.frontend :as auth-frontend]))

(defmulti check-auth! (fn [request _] (:subsystem request)))

(defmethod check-auth! :meuse.api.crate.http
  [request {:keys [token-db]}]
  (if (crate-http/skip-auth (:action request))
    request
    (auth-request/check-user token-db request)))

(defmethod check-auth! :meuse.api.mirror.http
  [request {:keys [token-db]}]
  (if (mirror-http/skip-auth (:action request))
    request
    (auth-request/check-user token-db request)))

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
  [request {:keys [_ user-db key-spec public-frontend]}]
  (cond
    (front-actions-no-cookie (:action request))
    request

    :else
    (do (if public-frontend
          ;; users can disable frontend authentication through the
          ;; public flag in the frontend configuration
          request
          (auth-frontend/verify-cookie user-db key-spec request)))))

(defn auth-request
  [token-db user-db key-spec public-frontend]
  {:name ::auth-request
   :enter
   (fn [{:keys [request] :as ctx}]
     (assoc ctx :request (check-auth! request
                                      {:token-db token-db
                                       :user-db user-db
                                       :key-spec key-spec
                                       :public-frontend public-frontend})))})
