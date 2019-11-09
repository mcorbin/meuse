(ns meuse.store.protocol)

(defprotocol ICrateFile
  (write-file [this raw-metadata crate-file])
  (get-file [this crate-name version])
  (versions [this crate-name]))
