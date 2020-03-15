(ns meuse.interceptor.error
  (:require [meuse.error :as err]
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
