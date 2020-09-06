(ns meuse.mirror
  "The store for the crates.io mirror."
  (:require [meuse.config :as config]
            [meuse.crate-file :refer [->CrateStore]]
            [meuse.log :as log]
            [meuse.store.protocol :as store]
            [byte-streams :as bs]
            [mount.core :refer [defstate]]
            [clj-http.client :as http]))

(defstate mirror-store
  :start
  (let [crate-config (:crate config/config)]
    (->CrateStore crate-config true)))

(def crates-io-base-url "https://crates.io/api/v1/crates")

(defn download-crate
  "Download a crate file."
  [crate-name version]
  (let [url (str crates-io-base-url "/" crate-name "/" version "/download")]
    (-> (http/get url {:as :byte-array})
        :body)))

(defn download-and-save
  "Download a crate file, save it in the crate mirror store, and return the
  file content."
  [mirror-store crate-name version]
  (log/infof {} "mirror: cache crate %s %s" crate-name version)
  (let [crate-file (download-crate crate-name version)]
    (store/write-file mirror-store
                      {:name crate-name
                       :vers version}
                      crate-file)
    crate-file))
