(ns meuse.crate-file
  "Manipulates the crate file."
  (:require [clojure.java.io :as io]))

(defn crate-file-path
  "Get the path to the crate file on disk."
  [base-path crate-name crate-version]
  (str base-path "/" crate-name "/" crate-version "/download"))

(defn save-crate-file
  "Takes a crate file and its metadata, saves the crate file on disk."
  [base-path {:keys [metadata crate-file]}]
  (let [path (crate-file-path base-path (:name metadata) (:vers metadata))]
    (io/make-parents path)
    (with-open [w (io/output-stream path)]
      (.write w crate-file))))
