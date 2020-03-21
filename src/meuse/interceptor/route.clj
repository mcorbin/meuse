(ns meuse.interceptor.route
  (:require meuse.api.crate.download
            [meuse.api.crate.http :as crate-http]
            [meuse.api.default :as default]
            [meuse.api.meuse.http :as meuse-http]
            [meuse.api.mirror.http :as mirror-http]
            [meuse.api.public.http :as public-http]
            meuse.api.public.healthz
            meuse.api.public.me
            meuse.api.public.metric
            [meuse.auth.request :as auth-request]
            [meuse.db.public.token :refer [token-db]]
            [meuse.front.http :as front-http]
            [meuse.metric :as metric]
            [meuse.request :as req]
            [bidi.bidi :refer [match-route*]]
            [clojure.tools.logging :as log]))

(def routes
  ["/"
   [["api/v1/crates" crate-http/crates-routes]
    ["api/v1/meuse" meuse-http/meuse-routes]
    ["api/v1/mirror" mirror-http/mirror-routes]
    ["front" front-http/front-routes]
    [#"static/.*" ::front-http/static]
    [#"healthz/?" :meuse.api.public.http/healthz]
    [#"health/?" :meuse.api.public.http/healthz]
    [#"me/?" :meuse.api.public.http/me]
    [#"metrics/?" :meuse.api.public.http/metrics]
    [#"status/?" :meuse.api.public.http/healthz]
    [true :meuse.api.public.http/default]]])

(def match-route
  {:name ::match-route
   :enter
   (fn [{:keys [request] :as ctx}]
     (let [uri (:uri request)]
       (assoc ctx :request (match-route* routes uri request))))})

(def subsystem
  {:name ::subsystem
   :enter (fn [{:keys [request] :as ctx}]
            (assoc ctx :request
                   (assoc request
                          :subsystem (-> request :handler namespace keyword)
                          :action (-> request :handler name keyword))))})

(defmulti route! :subsystem)

(defmethod route! :meuse.api.crate.http
  [request]
  (let [request (if (crate-http/skip-auth (:action request))
                  request
                  (auth-request/check-user token-db request))]
    (crate-http/crates-api! request)))

(defmethod route! :meuse.api.mirror.http
  [request]
  (mirror-http/mirror-api! request))

(defmethod route! :meuse.api.public.http
  [request]
  (public-http/public-api! request))

(defmethod route! :meuse.api.meuse.http
  [request]
  (meuse-http/meuse-api! (-> (req/convert-body-edn request))))

(defn front-route!
  [{:keys [key-spec]}]
  (defmethod route! :meuse.front.http
    [request]
    (front-http/front-api! request)))

(defmethod route! :default
  [request]
  (default/not-found request))

(def route
  {:name ::route
   :enter
   (fn [{:keys [request] :as ctx}]
     (log/debug "request" (str (:id request))
                "uri" (:uri request)
                "method" (:request-method request))
     (metric/with-time :http.requests ["uri" (:uri request)
                                       "method" (-> request
                                                    :request-method
                                                    name)]
       (assoc ctx :response (route! request))))})
