(ns meuse.config
  "Load the project configuration."
  (:require [meuse.spec :as spec]
            [environ.core :refer [env]]
            [mount.core :refer [defstate]]
            [unilog.config :refer [start-logging!]]
            [yummy.config :as yummy]
            [clojure.java.io :as io]
            [clojure.tools.logging :refer [debug info error]]))

(defn stop!
  "Stop the application."
  []
  (System/exit 1))

(defn load-config
  "Takes a path and loads the configuration."
  [path]
  (let [config (yummy/load-config
                {:program-name :meuse
                 :path path
                 :spec ::spec/config
                 :die-fn
                 (fn [e msg]
                   (let [error-msg (str "fail to load config: "
                                        msg
                                        "\n"
                                        "config path = "
                                        path)]
                     (error e error-msg)
                     (stop!)))})]
    (start-logging! (:logging config))
    (debug "config loaded, logger started !")
    config))

(defstate config
  :start (load-config (env :meuse-configuration)))
