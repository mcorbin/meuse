(ns meuse.core
  (:require [meuse.auth.password :as password]
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
  (when (and (seq args)
             (= (first args) "password"))
    (if-let [clear-password (second args)]
      (do (info "your password is:" (password/encrypt clear-password))
          (System/exit 0))
      (do (error "missing parameter for the password subcommand")
          (System/exit 1))))
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

