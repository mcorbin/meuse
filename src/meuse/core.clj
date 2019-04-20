(ns meuse.core
  (:require [clojure.tools.logging :refer [info error]]
            [mount.core :as mount]
            [signal.handler :refer [with-handler]]
            [meuse.http :as http])
  (:gen-class))

(defn stop!
  []
  (mount/stop))

(defn start!
  "Start the application using mount."
  []
  (mount/start)
  :ready)

(defn -main
  "I don't do a whole lot ... yet."
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

