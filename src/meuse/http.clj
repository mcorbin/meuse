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
            [meuse.interceptor.metric :as itc-metric]
            [meuse.log :as log]
            meuse.statistics
            [exoscale.interceptor :as interceptor]
            [less.awful.ssl :as less-ssl]
            [mount.core :refer [defstate]]
            [ring.adapter.jetty :as jetty]))

(defn interceptor-handler
  [crate-config metadata-config token-db user-db key-spec public-frontend]
  (let [interceptors
        [itc-error/last-error ;; leave
         itc-response/response ;;leave
         itc-metric/response-metrics ;; leave
         itc-json/json ;; leave
         itc-error/error ;; error
         itc-id/request-id ;;enter
         itc-ring/cookies ;; enter + leave
         itc-ring/params ;; enter
         itc-ring/keyword-params ;; enter
         (itc-config/config crate-config metadata-config) ;;enter
         itc-route/match-route ;; enter
         itc-route/subsystem  ;; enter
         (itc-auth/auth-request token-db user-db key-spec public-frontend)  ;; enter
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
  (log/debug {} "starting http server")
  (let [ssl-context (when (:cacert http-config)
                      (less-ssl/ssl-context (:key http-config)
                                            (:cert http-config)
                                            (:cacert http-config)))
        config (cond-> {:join? false
                        :host (:address http-config)}
                 (not ssl-context) (assoc :port (:port http-config))
                 ssl-context
                 (assoc :ssl? true
                        :http? false
                        :ssl-port (:port http-config)
                        :ssl-context ssl-context
                        :client-auth :need))
        key-spec (when-not (:public frontend)
                   (auth-frontend/secret-key-spec
                    (:secret frontend)))
        front-config (assoc frontend :key-spec key-spec)]
    (inject/inject! (:enabled frontend)
                    (:key-spec front-config)
                    (:public front-config))
    (when (:enabled frontend)
      (itc-route/front-route! front-config))
    (jetty/run-jetty (interceptor-handler crate-config
                                          metadata-config
                                          token-db
                                          user-db
                                          (:key-spec front-config)
                                          (:public front-config))
                     config)))

(defstate http-server
  :start (start-server (:http config)
                       (:crate config)
                       (:metadata config)
                       (:frontend config)
                       token-db/token-db
                       user-db/user-db)
  :stop (do (log/debug {} "stopping http server")
            (.stop http-server)
            (log/debug {} "http server stopped")))

