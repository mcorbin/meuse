(ns meuse.crate-file
  "Manipulates the crate file."
  (:require [meuse.path :as path]
            [clojure.java.io :as io]))

(defn crate-file-path
  "Get the path to the crate file on disk."
  [base-path crate-name crate-version]
  (path/new-path base-path crate-name crate-version "download"))

(defn write-crate-file
  "Takes a crate file and its metadata, writes the crate file on disk."
  [base-path {:keys [raw-metadata crate-file]}]
  (let [path (crate-file-path base-path
                              (:name raw-metadata)
                              (:vers raw-metadata))]
    (io/make-parents path)
    (with-open [w (io/output-stream path)]
      (.write w crate-file))))
