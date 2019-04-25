(ns meuse.crate-file
  (:require [clojure.java.io :as io]))

(defn crate-file-path
  [base-path crate-name crate-version]
  (str base-path "/" crate-name "/" crate-version "/download"))

(defn save-crate-file
  "takes a crate file and its metadata, saves the crate file on disk"
  [base-path {:keys [metadata crate-file]}]
  (let [path (crate-file-path base-path (:name metadata) (:vers metadata))]
    (io/make-parents path)
    (spit path
          (String. crate-file java.nio.charset.StandardCharsets/UTF_8))))
