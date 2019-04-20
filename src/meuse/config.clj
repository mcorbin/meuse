(ns meuse.config
  (:require [clojure.tools.logging :refer [debug info error]]
            [environ.core :refer [env]]
            [mount.core :refer [defstate]]
            [unilog.config :refer [start-logging!]]
            [yummy.config :as yummy]
            [meuse.spec :as spec]))

(def default-db-config
  {:classname "org.postgresql.Driver"
   :subprotocol "postgresql"})

(defn load-config
  [path]
  (let [config (-> (yummy/load-config {:program-name :meuse
                                       :path path
                                       :spec ::spec/config
                                       :die-fn
                                       (fn [e msg]
                                         (error e
                                                (str "fail to load config: "
                                                     msg
                                                     "\n"
                                                     "config path = "
                                                     path))
                                         (System/exit 1))})
                   (update :database #(merge default-db-config %)))]
    (start-logging! (:logging config))
    (debug "config loaded, logger started !")
    config))

(defstate config
  :start (load-config (env :meuse-configuration)))
