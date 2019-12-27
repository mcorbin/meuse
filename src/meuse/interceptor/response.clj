(ns meuse.interceptor.response)

(def response
  {:name ::response
   :leave
   (fn [ctx]
     (:response ctx))})
