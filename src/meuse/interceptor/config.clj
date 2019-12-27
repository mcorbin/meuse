(ns meuse.interceptor.config
  (:require [meuse.registry :as registry]))

;; TODO: these config should be injected and should not be
;; part of the request
(defn config
  [crate-config metadata-config]
  (let [registry-config (registry/read-registry-config
                         (:path metadata-config)
                         (:url metadata-config))]
    {:name ::config
     :enter (fn [{:keys [request] :as ctx}]
              (assoc ctx :request
                     (assoc request
                            :config
                            {:crate crate-config
                             :metadata metadata-config}
                            :registry-config registry-config)))}))
