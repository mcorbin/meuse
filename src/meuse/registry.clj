(ns meuse.registry
  "Manage the registry configuration file."
  (:require [meuse.path :as path]
            [cheshire.core :as json]
            [exoscale.ex :as ex]
            [clojure.java.io :as io]))

(defn read-registry-config
  "Read the `config.json` file in the crate git registry.
  If the `allowed-registry` key is empty, the current registry url
  will be used."
  [base-path registry-url]
  (let [config-path (path/new-path base-path "config.json")]
    (when-not (.exists (io/file config-path))
      (throw (ex/ex-fault (str "the file " config-path " does not exist"))))
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
        (throw (ex/ex-forbidden (str "the registry " registry " is not allowed."
                                     " Please check https://github.com/rust-lang/rfcs/blob/master/text/2141-alternative-registries.md#registry-index-format-specification"))))))
  true)
