(ns meuse.registry
  "Manage the registry configuration file."
  (:require [cheshire.core :as json]
            [clojure.java.io :as io]
            [clojure.tools.logging :refer [debug info error]]))

(defn read-registry-config
  "Read the `config.json` file in the crate git registry.
  If the `allowed-registry` key is empty, the current registry url
  will be used."
  [base-path registry-url]
  (let [config-path (.getPath (io/file base-path "config.json"))]
    (when-not (.exists (io/file config-path))
      (throw (ex-info (str "the file " config-path " does not exist") {})))
    (-> (slurp config-path)
        (json/parse-string true)
        (update :allowed-registries #(if (seq %)
                                       %
                                       [registry-url])))))

(defn allowed-registry?
  "Takes a crate medadata and the allowed registries from the Git repository.
  Throws an exception if the registry is not allowed."
  [metadata allowed-registries]
  (when-not (seq allowed-registries)
    ;; should not happen, because by default the current registry should be
    ;; allowed in read-registry-config
    (throw (ex-info (str "no registry allowed")
                    {})))
  (doseq [dep (:deps metadata)]
    (when-let [registry (:registry dep)]
      (when-not ((set allowed-registries) registry)
        (throw (ex-info (str "the registry " registry " is not allowed")
                        {})))))
  true)
