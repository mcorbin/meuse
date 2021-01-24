(ns meuse.config
  "Load the project configuration."
  (:require [exoscale.cloak :as cloak]
            [exoscale.ex :as ex]
            [meuse.log :as log]
            [meuse.spec :as spec]
            [environ.core :refer [env]]
            [mount.core :refer [defstate]]
            [unilog.config :refer [start-logging!]]
            [yummy.config :as yummy]))

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
                     (log/error {} e error-msg)
                     (stop!)))})]
    (when-let [front-secret (get-in config [:frontend :secret])]
      (when (< (count (cloak/unmask front-secret)) spec/frontend-secret-min-size)
        (throw (ex/ex-incorrect (format "The frontend secret is too small (minimum size is %d"
                                        spec/frontend-secret-min-size)))))
    (start-logging! (:logging config))
    (log/info {} "config loaded, logger started !")
    config))

(defstate config
  :start (load-config (env :meuse-configuration)))
