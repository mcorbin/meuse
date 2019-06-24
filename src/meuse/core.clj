(ns meuse.core
  (:require [meuse.http :as http]
            [mount.core :as mount]
            [signal.handler :refer [with-handler]]
            [clojure.tools.logging :refer [info error]])
  (:gen-class))

(defn stop!
  "Stops the application"
  []
  (mount/stop))

(defn start!
  "Start the application using mount."
  []
  (mount/start)
  (info "Ecoute, on t'connaît pas, mais laisse nous t'dire que tu t'prépares des nuits blanches, des migraines... des «nervous breakdown» comme on dit de nos jours.")
  :ready)

(defn -main
  "Starts the application"
  [& args]
  (with-handler :term
      (info "SIGTERM, stopping")
      (stop!)
      (info "the system is stopped")
      (System/exit 0))

    (with-handler :int
      (info "SIGINT, stopping")
      (stop!)
      (info "the system is stopped")
      (System/exit 0))
  (start!))

