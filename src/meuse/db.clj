(ns meuse.db
  (:require [clojure.java.jdbc :as j]
            [clojure.tools.logging :refer [debug info error]]
            [aleph.http :as http]
            [mount.core :refer [defstate]]
            [meuse.config :refer [config]])
  (:import (com.mchange.v2.c3p0 ComboPooledDataSource)))

(defn pool
  [spec]
  (debug "starting database thread pool")
  (let [cpds (doto (ComboPooledDataSource.)
               (.setDriverClass (:classname spec))
               (.setJdbcUrl (str "jdbc:" (:subprotocol spec) ":" (:subname spec)))
               (.setUser (:user spec))
               (.setPassword (:password spec))
               ;; expire excess connections after 30 minutes of inactivity:
               (.setMaxIdleTimeExcessConnections (* 30 60))
               ;; expire connections after 3 hours of inactivity:
               (.setMaxIdleTime (* 3 60 60)))]
    {:datasource cpds}))

(defstate database
  :start (pool (:database config))
  :stop (do (debug "stopping db pool")
            (.close (:datasource database))
            (debug "db pool stopped")))

