(ns meuse.mocks.store
  (:require [meuse.store.protocol :refer [ICrateStore]]
            [spy.core :as spy]
            [spy.protocol :as protocol]))

(defn store-mock
  "Crates a mock for the crate file store."
  [{:keys [exists get-file versions write-file]}]
  (protocol/spy ICrateStore
                (reify ICrateStore
                  (exists [this crate-name version]
                    exists)
                  (get-file [this crate-name version]
                    get-file)
                  (versions [this crate-name]
                    versions)
                  (write-file [this raw-metadata crate-file]
                    write-file))))
