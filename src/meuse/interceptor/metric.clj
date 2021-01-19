(ns meuse.interceptor.metric
  (:require [meuse.metric :as metric]))

(def response-metrics
  {:name ::response-metrics
   :leave (fn [ctx]
            (metric/http-response ctx)
            ctx)})
