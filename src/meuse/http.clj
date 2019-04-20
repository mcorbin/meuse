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

(defn get-handler
  [database git]
  (fn handler
    [request]
    (let [uri (:uri request)
          ;; add a request id
          request (->>
                   (assoc request :request-id (str (UUID/randomUUID)))
                   (match-route* routes uri))]
      (-> request
          (assoc :database database
                 :git git
                 :subsystem (-> request :handler namespace keyword)
                 :action (-> request :handler name keyword))
          route!))))

(comment (defn get-handler
           [database]
           (fn handler [req]
             (let [req (-> (h/insert-into :test)
                           (h/columns :test)
                           (h/values [["test1"] ["test2"]])
                           sql/format)]
               (jdbc/execute! database req))
             {:status 200
              :headers {"content-type" "text/plain"}
              :body "hello!!"})))

(defn start-server
  [config database git]
  (info "starting http server")
  (http/start-server (-> (get-handler database git)
                         wrap-json
                         wrap-keyword-params) config))

(defstate http-server
  :start (start-server (:http config) database git)
  :stop (do (debug "stopping http server")
            (.close ^Closeable http-server)
            (Thread/sleep 4)
            (netty/wait-for-close http-server)
            (Thread/sleep 2)
            (debug "http server stopped")))

