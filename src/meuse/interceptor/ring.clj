(ns meuse.interceptor.ring
  (:require [ring.middleware.content-type :as content-type]
            [ring.middleware.resource :as resource]
            [ring.middleware.keyword-params :as keyword-params]
            [ring.middleware.params :as params]))

(def keyword-params
  {:name ::keyword-params
   :enter (fn [ctx] (update ctx :request keyword-params/keyword-params-request))})

(def params
  {:name ::params
   :enter (fn [ctx] (update ctx :request params/params-request))})
