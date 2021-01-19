(ns meuse.interceptor.error
  (:require [meuse.error :as err]
            [meuse.log :as log]
            [meuse.metric :as metric]
            [exoscale.ex :as ex]))

(defn handle-error
  [request e]
  (cond
    (ex/type? e ::err/user) (err/handle-user-error request e)
    (ex/type? e ::err/redirect-login) (err/redirect-login-error request e)
    :else (err/handle-unexpected-error request e)))

(def error
  {:name ::error
   :error (fn [ctx e]
            (assoc ctx :response (handle-error (:request ctx) e)))})

(def last-error
  {:name ::last-error
   :error (fn [ctx e]
            (try
              (metric/increment! :http.fatal.error.total
                                 {})
              (log/error {} e "fatal error")
              (assoc ctx :response {:status 500
                                    :body {:error err/default-msg}})
              (catch Exception e
                (log/error {} e "fatal error in the last handler")
                (assoc ctx :response {:status 500
                                      :body {:error err/default-msg}}))))})
