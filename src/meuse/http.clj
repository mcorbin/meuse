(ns meuse.http
  (:require [clojure.java.jdbc :as jdbc]
            [clojure.tools.logging :refer [debug info error]]
            [aleph.http :as http]
            [aleph.netty :as netty]
            [bidi.bidi :refer [match-route*]]
            [honeysql.core :as sql]
            [honeysql.helpers :as h]
            [mount.core :refer [defstate]]
            [ring.middleware.keyword-params :refer [wrap-keyword-params]]
            [ring.middleware.content-type :refer [wrap-content-type]]
            [meuse.api.crates.http :refer [crates-routes crates-api!]]
            [meuse.api.default :refer [default-api!]]
            [meuse.config :refer [config]]
            [meuse.db :refer [database]]
            [meuse.git :refer [git]]
            [meuse.middleware :refer [wrap-json]])
  (:import java.io.Closeable
           java.util.UUID))

(def default-error-msg "internal error.")

(def routes
  ["/"
   [["api/v1/crates" crates-routes]
    [true :meuse.api.default/not-found]]])

(defmulti route! :subsystem)

(defmethod route! :meuse.api.crates.http
  [request]
  (crates-api! request))

(defmethod route! :meuse.api.default
  [request]
  (default-api! request))

(defmethod route! :default
  [request]
  {:status 400})

(defn handle-req-errors
  [request ^Exception e]
  (let [data (ex-data e)
        ;; cargo expects a status 200 OK even for errors x_x
        status (if (= (:subsystem request) :meuse.api.crates.http)
                 200
                 (:status data))
        request-id (:request-id request)
        message (if status
                  (.getMessage e)
                  default-error-msg)]
    (error request-id e "http error" (pr-str data))
    {:status (or status 500)
     :body {:errors [{:detail message}]}}))

(defn get-handler
  [crate-config database git]
  (fn handler
    [request]
    (let [uri (:uri request)
          ;; add a request id
          request (->> (assoc request :request-id (str (UUID/randomUUID)))
                       (match-route* routes uri))
          ;; todo: clean this mess
          request (assoc request
                         :database database
                         :git git
                         :crate-config crate-config
                         :subsystem (-> request :handler namespace keyword)
                         :action (-> request :handler name keyword))]
      (try
        (route! request)
        (catch Exception e
          (handle-req-errors request e))))))

(defn start-server
  [config crate-config database git]
  (debug "starting http server")
  (http/start-server (-> (get-handler crate-config database git)
                         wrap-json
                         wrap-keyword-params) config))

(defstate http-server
  :start (start-server (:http config) (:crate config) database git)
  :stop (do (debug "stopping http server")
            (.close ^Closeable http-server)
            (Thread/sleep 4)
            (netty/wait-for-close http-server)
            (Thread/sleep 2)
            (debug "http server stopped")))

