(ns meuse.mirror
  "The store for the crates.io mirror."
  (:require [meuse.config :as config]
            [meuse.path :as path]
            meuse.store.filesystem
            [meuse.store.protocol :as store]
            [aleph.http :as http]
            [byte-streams :as bs]
            [mount.core :refer [defstate]]
            [clojure.tools.logging :refer [infof]])
  (:import [meuse.store.filesystem LocalCrateFile]))

(defstate mirror-store
  :start
  (let [crate-config (:crate config/config)
        store (:store crate-config)]
    (condp = store
      "filesystem" (LocalCrateFile. (path/new-path (:path crate-config)
                                                   ".crates.io"))
      (throw (ex-info (str "invalid crate store " store) {})))))

(def crates-io-base-url "https://crates.io/api/v1/crates")

(defn download-crate
  "Download a crate file."
  [crate-name version]
  (let [url (str crates-io-base-url "/" crate-name "/" version "/download")
        response @(http/get url)]
    (-> @(http/get url)
        :body
        (bs/to-byte-array))))

(defn download-and-save
  "Download a crate file, save it in the crate mirror store, and return the
  file content."
  [mirror-store crate-name version]
  (infof "mirror: cache crate %s %s" crate-name version)
  (let [crate-file (download-crate crate-name version)]
    (store/write-file mirror-store
                      {:name crate-name
                       :vers version}
                      crate-file)
    crate-file))
