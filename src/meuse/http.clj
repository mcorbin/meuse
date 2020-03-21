(ns meuse.http
  "HTTP server and handlers"
  (:require [meuse.auth.frontend :as auth-frontend]
            [meuse.config :refer [config]]
            [meuse.db.public.token :as token-db]
            [meuse.db.public.user :as user-db]
            [meuse.inject :as inject]
            [meuse.interceptor.auth :as itc-auth]
            [meuse.interceptor.config :as itc-config]
            [meuse.interceptor.error :as itc-error]
            [meuse.interceptor.id :as itc-id]
            [meuse.interceptor.json :as itc-json]
            [meuse.interceptor.ring :as itc-ring]
            [meuse.interceptor.response :as itc-response]
            [meuse.interceptor.route :as itc-route]
            [aleph.http :as http]
            [aleph.netty :as netty]
            [exoscale.interceptor :as interceptor]
            [less.awful.ssl :as less-ssl]
            [mount.core :refer [defstate]]
            [clojure.tools.logging :refer [debug info error]])
  (:import io.netty.handler.ssl.ClientAuth
           io.netty.handler.ssl.JdkSslContext
           java.io.Closeable
           java.net.InetSocketAddress
           java.util.UUID))

(defn interceptor-handler
  [crate-config metadata-config token-db user-db key-spec]
  (let [interceptors
        [itc-response/response ;;leave
         itc-json/json ;; leave
         itc-error/error ;; error
         itc-id/request-id ;;enter
         itc-ring/cookies ;; enter + leave
         itc-ring/params ;; enter
         itc-ring/keyword-params ;; enter
         (itc-config/config crate-config metadata-config) ;;enter
         itc-route/match-route ;; enter
         itc-route/subsystem  ;; enter
         (itc-auth/auth-request token-db user-db key-spec)  ;; enter
         itc-route/route ;; enter
         ]]
    (fn handler [request]
      (interceptor/execute {:request request} interceptors))))

(defn start-server
  [http-config
   crate-config
   metadata-config
   frontend
   token-db
   user-db]
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
                 ssl-context (assoc :ssl-context ssl-context))
        front-config (assoc frontend :key-spec (auth-frontend/secret-key-spec
                                                (:secret frontend)))]
    (inject/inject! (:enabled frontend) (:key-spec front-config))
    (when (:enabled frontend)
      (itc-route/front-route! front-config))
    (http/start-server (interceptor-handler crate-config
                                            metadata-config
                                            token-db
                                            user-db
                                            (:key-spec front-config))
                       config)))

(defstate http-server
  :start (start-server (:http config)
                       (:crate config)
                       (:metadata config)
                       (:frontend config)
                       token-db/token-db
                       user-db/user-db)
  :stop (do (debug "stopping http server")
            (.close ^Closeable http-server)
            (Thread/sleep 4)
            (netty/wait-for-close http-server)
            (Thread/sleep 2)
            (debug "http server stopped")))

