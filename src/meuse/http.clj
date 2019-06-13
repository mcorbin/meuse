(ns meuse.http
  "HTTP server and handlers"
  (:require [clojure.java.jdbc :as jdbc]
            [clojure.tools.logging :refer [debug info error]]
            [aleph.http :as http]
            [aleph.netty :as netty]
            [bidi.bidi :refer [match-route*]]
            [mount.core :refer [defstate]]
            [ring.middleware.params :refer [wrap-params]]
            [ring.middleware.keyword-params :refer [wrap-keyword-params]]
            [ring.middleware.content-type :refer [wrap-content-type]]
            [meuse.api.crate.http :refer [crates-routes crates-api!]]
            [meuse.api.default :refer [not-found]]
            meuse.api.crate.new
            meuse.api.crate.owner
            meuse.api.crate.yank
            meuse.api.crate.download
            meuse.api.crate.search
            [meuse.api.meuse.http :as meuse-http]
            meuse.api.meuse.category
            meuse.api.meuse.token
            [meuse.api.default :refer [default-api!]]
            [meuse.auth.token :as auth-token]
            [meuse.auth.request :as auth-request]
            [meuse.config :refer [config]]
            [meuse.db :refer [database]]
            [meuse.git :refer [git]]
            [meuse.middleware :refer [wrap-json]]
            [meuse.registry :as registry]
            [meuse.request :refer [convert-body-edn]])
  (:import java.io.Closeable
           java.util.UUID))

(def default-error-msg "internal error.")

(def routes
  ["/"
   [["api/v1/crates" crates-routes]
    ["api/v1/meuse" meuse-http/meuse-routes]
    [#"me/?" :meuse.api.default/me]
    [true :meuse.api.default/not-found]]])

(defmulti route! :subsystem)

(defmethod route! :meuse.api.crate.http
  [request]
  (crates-api! (auth-request/check-user request)))

(defmethod route! :meuse.api.meuse.http
  [request]
  (let [request (if (meuse-http/skip-auth (:action request))
                  request
                  (auth-request/check-user request))])
  (meuse-http/meuse-api! (-> (convert-body-edn request))))

(defmethod route! :meuse.api.default
  [request]
  (default-api! request))

(defmethod route! :default
  [request]
  not-found)

(defn handle-req-errors
  "Handles HTTP exceptions."
  [request ^Exception e]
  (let [data (ex-data e)
        message (if (:status data)
                  (.getMessage e)
                  default-error-msg)
        ;; cargo expects a status 200 OK even for errors x_x
        status (if (= (:subsystem request) :meuse.api.crate.http)
                 200
                 (:status data))
        request-id (:request-id request)]
    (error request-id e "http error" (pr-str data))
    {:status (or status 500)
     :body {:errors [{:detail message}]}}))

(defn get-handler
  "Returns the main handler for the HTTP server."
  [crate-config metadata-config database git]
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
              ;; todo: clean this mess
              request (assoc request
                             :database database
                             :git git
                             :config {:crate crate-config
                                      :metadata metadata-config}
                             :subsystem (-> request :handler namespace keyword)
                             :action (-> request :handler name keyword)
                             :registry-config registry-config)]
          (debug "request " (:request-id request)
                 "with subsystem" (:subsystem request)
                 "with action" (:action request))
          (try (route! request)
               (catch Exception e
                 (handle-req-errors request e))))
        (catch Exception e
          {:status 500
           :body {:errors [{:detail default-error-msg}]}})))))

(defn start-server
  [http-config crate-config metadata-config database git]
  (debug "starting http server")
  (http/start-server (-> (get-handler crate-config
                                      metadata-config
                                      database
                                      git)
                         wrap-json
                         wrap-keyword-params
                         wrap-params)
                     http-config))

(defstate http-server
  :start (start-server (:http config)
                       (:crate config)
                       (:metadata config)
                       database
                       git)
  :stop (do (debug "stopping http server")
            (.close ^Closeable http-server)
            (Thread/sleep 4)
            (netty/wait-for-close http-server)
            (Thread/sleep 2)
            (debug "http server stopped")))

