(ns meuse.core
  (:require [meuse.auth.password :as password]
            [mount.core :as mount]
            meuse.http
            [meuse.log :as log]
            [signal.handler :refer [with-handler]])
  (:gen-class))

(defn stop!
  "Stops the application"
  []
  (mount/stop))

(defn start!
  "Start the application using mount."
  []
  (mount/start)
  (log/info {} "Ecoute, on t'connait pas, mais laisse nous t'dire que tu t'prepares des nuits blanches, des migraines... des \"nervous breakdown\" comme on dit de nos jours.")
  :ready)

(defn -main
  "Starts the application"
  [& args]
  (when (and (seq args)
             (= (first args) "password"))
    (if-let [clear-password (second args)]
      (do (log/info {} "your password is:" (password/encrypt clear-password))
          (System/exit 0))
      (do (log/error {} "missing parameter for the password subcommand")
          (System/exit 1))))
  (with-handler :term
    (log/info {} "SIGTERM, stopping")
    (stop!)
    (log/info {} "the system is stopped")
    (System/exit 0))

  (with-handler :int
    (log/info {} "SIGINT, stopping")
    (stop!)
    (log/info {} "the system is stopped")
    (System/exit 0))
  (start!))
