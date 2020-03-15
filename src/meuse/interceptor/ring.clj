(ns meuse.interceptor.ring
  (:require [ring.middleware.content-type :as content-type]
            [ring.middleware.cookies :as cookies]
            [ring.middleware.params :as params]
            [ring.middleware.keyword-params :as keyword-params]
            [ring.middleware.resource :as resource]))

(def keyword-params
  {:name ::keyword-params
   :enter (fn [ctx] (update ctx :request keyword-params/keyword-params-request))})

(def params
  {:name ::params
   :enter (fn [ctx] (update ctx :request params/params-request))})

(def cookies
  {:name ::cookies
   :enter (fn [ctx] (update ctx :request #(cookies/cookies-request % {})))
   :leave (fn [ctx] (update ctx :response #(cookies/cookies-response % {})))})
