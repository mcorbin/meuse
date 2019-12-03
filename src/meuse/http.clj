(ns meuse.http
  "HTTP server and handlers"
  (:require meuse.api.crate.download
            [meuse.api.crate.http :as crate-http]
            [meuse.api.default :refer [not-found]]
            [meuse.api.meuse.http :as meuse-http]
            [meuse.api.mirror.http :as mirror-http]
            [meuse.api.public.http :as public-http]
            meuse.api.public.healthz
            meuse.api.public.me
            meuse.api.public.metric
            [meuse.auth.token :as auth-token]
            [meuse.auth.request :as auth-request]
            [meuse.config :refer [config]]
            [meuse.db.public.token :refer [token-db]]
            [meuse.error :as err]
            [meuse.front.base :as base-http]
            [meuse.front.http :as front-http]
            [meuse.inject :as inject]
            [meuse.metric :as metric]
            [meuse.middleware :refer [wrap-json]]
            [meuse.registry :as registry]
            [meuse.request :refer [convert-body-edn]]
            [aleph.http :as http]
            [aleph.netty :as netty]
            [bidi.bidi :refer [match-route*]]
            [hiccup.page :as page]
            [less.awful.ssl :as less-ssl]
            [mount.core :refer [defstate]]
            [exoscale.ex :as ex]
            [ring.middleware.content-type :refer [wrap-content-type]]
            [ring.middleware.resource :refer [wrap-resource]]
            [ring.middleware.keyword-params :refer [wrap-keyword-params]]
            [ring.middleware.params :refer [wrap-params]]
            [clojure.java.jdbc :as jdbc]
            [clojure.tools.logging :refer [debug info error]])
  (:import io.netty.handler.ssl.ClientAuth
           io.netty.handler.ssl.JdkSslContext
           java.io.Closeable
           java.net.InetSocketAddress
           java.util.UUID))

(def routes
  ["/"
   [["api/v1/crates" crate-http/crates-routes]
    ["api/v1/meuse" meuse-http/meuse-routes]
    ["api/v1/mirror" mirror-http/mirror-routes]
    ["front" front-http/front-routes]
    [#"healthz/?" :meuse.api.public.http/healthz]
    [#"health/?" :meuse.api.public.http/healthz]
    [#"me/?" :meuse.api.public.http/me]
    [#"metrics/?" :meuse.api.public.http/metrics]
    [#"status/?" :meuse.api.public.http/healthz]
    [true :meuse.api.public.http/default]]])

(defmulti route! :subsystem)

(defmethod route! :meuse.api.crate.http
  [request]
  (let [request (if (crate-http/skip-auth (:action request))
                  request
                  (auth-request/check-user token-db request))]
    (crate-http/crates-api! request)))

(defmethod route! :meuse.api.mirror.http
  [request]
  (mirror-http/mirror-api!
   ;;(auth-request/check-user token-db request)
   request))

(defmethod route! :meuse.api.public.http
  [request]
  (public-http/public-api! request))

(defmethod route! :meuse.api.meuse.http
  [request]
  (let [request (if (meuse-http/skip-auth (:action request))
                  request
                  (auth-request/check-user token-db request))]
    (meuse-http/meuse-api! (-> (convert-body-edn request)))))

(defn front-route!
  []
  (defmethod route! :meuse.front.http
    [request]
    {:status 200
     :headers {"Content-Type" "text/html; charset=utf-8"}
     :body (base-http/html (front-http/front-api! request))}))

(defmethod route! :default
  [request]
  (not-found request))

(defn get-handler
  "Returns the main handler for the HTTP server."
  [crate-config metadata-config]
  (let [registry-config (registry/read-registry-config
                         (:path metadata-config)
                         (:url metadata-config))]
    (fn handler
      [request]
      (try
        (let [uri (:uri request)
              ;; add a request id
              request (->> (assoc request :request-id (str (UUID/randomUUID)))
                           (match-route* routes uri))
              _ (info "handler is " (:handler request))
              ;; todo: clean this mess
              request (assoc request
                             :config {:crate crate-config
                                      :metadata metadata-config}
                             :subsystem (-> request :handler namespace keyword)
                             :action (-> request :handler name keyword)
                             :registry-config registry-config)]
          (debug "request" (:request-id request)
                 "with subsystem" (:subsystem request)
                 "with action" (:action request))
          (metric/with-time :http.requests ["uri" (:uri request)
                                            "method" (-> request
                                                         :request-method
                                                         name)]
            (ex/try+
             (route! request)
             (catch :meuse.error/user data
              (err/handle-user-error request data))
             (catch Exception e
               (err/handle-unexpected-error request e)))))
        (catch Exception e
          (error e "internal error")
          {:status 500
           :body {:errors [{:detail err/default-msg}]}})))))

(defn start-server
  [http-config crate-config metadata-config frontend-enabled?]
  (debug "starting http server")
  (let [ssl-context (when (:cacert http-config)
                      (JdkSslContext. (less-ssl/ssl-context (:key http-config)
                                                            (:cert http-config)
                                                            (:cacert http-config))
                                      false
                                      ClientAuth/REQUIRE))
        config (cond-> {:epoll true
                        :socket-address (InetSocketAddress.
                                         ^String (:address http-config)
                                         ^Integer (:port http-config))}
                 ssl-context (assoc :ssl-context ssl-context))]
    (inject/inject! frontend-enabled?)
    (when frontend-enabled?
      (front-route!))
    (http/start-server (-> (get-handler crate-config
                                        metadata-config)
                           (wrap-resource "public")
                           wrap-json
                           wrap-keyword-params
                           wrap-params)
                       config)))

(defstate http-server
  :start (start-server (:http config)
                       (:crate config)
                       (:metadata config)
                       (:frontend config))
  :stop (do (debug "stopping http server")
            (.close ^Closeable http-server)
            (Thread/sleep 4)
            (netty/wait-for-close http-server)
            (Thread/sleep 2)
            (debug "http server stopped")))

