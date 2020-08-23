(ns meuse.store.protocol)

(defprotocol ICrateStore
  (exists [this crate-name version])
  (get-file [this crate-name version])
  (versions [this crate-name])
  (write-file [this raw-metadata crate-file])
  (delete-file [this crate-name version]))
