(ns meuse.store.protocol)

(defprotocol ICrateFile
  (exists [this crate-name version])
  (get-file [this crate-name version])
  (versions [this crate-name])
  (write-file [this raw-metadata crate-file]))
