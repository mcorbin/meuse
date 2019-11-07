(ns meuse.db
  "The database component"
  (:require [meuse.config :refer [config]]
            [meuse.metric :as metric]
            [aleph.http :as http]
            [mount.core :refer [defstate]]
            [clojure.java.jdbc :as j]
            [clojure.tools.logging :refer [debug info error]])
  (:import com.zaxxer.hikari.HikariConfig
           com.zaxxer.hikari.HikariDataSource))

(def default-pool-size 2)
(def default-ssl-mode "verify-full")

(defn pool
  [{:keys [user password host port name max-pool-size key cert cacert ssl-password ssl-mode]}]
  (debug "starting database connection pool")
  (let [url (format "jdbc:postgresql://%s:%d/%s"
                    host port name)
        config (doto (HikariConfig.)
                 (.setMetricRegistry metric/registry)
                 (.setJdbcUrl url)
                 (.addDataSourceProperty "user" user)
                 (.addDataSourceProperty "password" password)
                 (.setMaximumPoolSize (or max-pool-size default-pool-size)))]
    (when key
      (info "ssl enabled for the database")
      (.addDataSourceProperty config "ssl" true)
      (.addDataSourceProperty config
                              "sslfactory"
                              "org.postgresql.ssl.jdbc4.LibPQFactory")
      (.addDataSourceProperty config "sslcert" cert)
      (.addDataSourceProperty config "sslkey" key)
      (.addDataSourceProperty config "sslrootcert" cacert)
      (.addDataSourceProperty config "sslmode" (or ssl-mode
                                                   default-ssl-mode)))
    {:datasource (HikariDataSource. config)}))

(defstate database
  :start (pool (:database config))
  :stop (do (debug "stopping db pool")
            (.close ^HikariDataSource (:datasource database))
            (debug "db pool stopped")))
