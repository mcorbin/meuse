(ns meuse.crate-file
  "Manipulates the crate file."
  (:require [meuse.config :as config]
            meuse.store.filesystem
            [mount.core :refer [defstate]])
  (:import [meuse.store.filesystem LocalCrateFile]))

(defstate crate-file-store
  :start
  (let [crate-config (:crate config/config)
        store (:store crate-config)]
    (condp = store
      "filesystem" (LocalCrateFile. (:path crate-config))
      (throw (ex-info (str "invalid crate store " store) {})))))
