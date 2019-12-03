(ns meuse.crate-file
  "Manipulates the crate file."
  (:require [meuse.config :as config]
            meuse.store.filesystem
            meuse.store.s3
            [meuse.path :as path]
            [exoscale.ex :as ex]
            [mount.core :refer [defstate]])
  (:import [meuse.store.filesystem LocalCrateFile]
           [meuse.store.s3 S3CrateStore]))

(defn ->CrateStore
  "Creates a crate store from a configuration."
  ([config] (->CrateStore config false))
  ([config mirror?]
   (let [store (:store config)]
     (condp = store
       "filesystem" (LocalCrateFile. (if mirror?
                                       (path/new-path (:path config)
                                                      ".crates.io")
                                       (:path config)))
       "s3" (S3CrateStore. (select-keys config [:access-key
                                                :secret-key
                                                :endpoint])
                           (:bucket config)
                           (when mirror? ".crates.io"))
       (throw (ex/ex-incorrect (str "invalid crate store " store)))))))

(defstate crate-file-store
  :start
  (let [crate-config (:crate config/config)]
    (->CrateStore crate-config false)))
