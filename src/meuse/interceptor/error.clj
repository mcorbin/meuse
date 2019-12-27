(ns meuse.interceptor.error
  (:require [meuse.error :as err]
            [exoscale.ex :as ex]))

(defn handle-error
  [request e]
  (if (ex/type? e ::err/user)
    (err/handle-user-error request e)
    (err/handle-unexpected-error request e)))

(def error
  {:name ::error
   :error (fn [ctx e]
            (assoc ctx :response (handle-error (:request ctx) e)))})
