(ns user
  (:require [mount.core :as mount]
            meuse.core
            [clojure.tools.namespace.repl :as tn]))

(defn stop!
  []
  (mount/stop))

(defn start!
  []
  (meuse.core/start!))

(defn refresh!
  []
  (stop!)
  (tn/refresh :after 'meuse.core/start!))

(defn go
  []
  (start!))
