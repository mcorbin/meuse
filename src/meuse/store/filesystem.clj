(ns meuse.store.filesystem
  "Manipulates the crate files in the filesystem."
  (:require [meuse.path :as path]
            [meuse.store.protocol :refer [ICrateStore]]
            [exoscale.ex :as ex]
            [clojure.java.io :as io]))

(defn crate-file-path
  "Get the path to the crate file on disk."
  [base-path crate-name crate-version]
  (path/new-path base-path crate-name crate-version "download"))

(defn filesystem-versions
  "For each version detected in the filesystem, detects if the `download`
  file exists.
  Returns a map whose keys are version and values a boolean indicating if
  the file exists."
  [base-path crate-name]
  (let [dir (io/file (path/new-path base-path crate-name))
        versions (.list dir)]
    (reduce
     (fn [state version]
       (assoc state version (.exists
                             (io/file (crate-file-path base-path
                                                       crate-name
                                                       version)))))
     {}
     versions)))

;; todo: refactor: should take name and version and not raw-metadata
(defn write
  [base-path raw-metadata crate-file]
  (let [path (crate-file-path base-path
                              (:name raw-metadata)
                              (:vers raw-metadata))]
    (io/make-parents path)
    (with-open [w (io/output-stream path)]
      (.write w crate-file))))

(defrecord LocalCrateFile [base-path]
  ICrateStore
  (exists [this crate-name version]
    (let [path (crate-file-path base-path
                                crate-name
                                version)]
      (.exists (io/file path))))
  (get-file [this crate-name version]
    (let [path (crate-file-path
                base-path
                crate-name
                version)
          file (io/file path)]
      (when-not (.exists file)
        (throw (ex/ex-incorrect (format "the file %s does not exist" path))))
      (when (.isDirectory file)
        (throw (ex/ex-incorrect (format "the file %s is a directory" path))))
      file))
  (versions [this crate-name]
    (filesystem-versions base-path crate-name))
  (write-file [this raw-metadata crate-file]
    (write base-path raw-metadata crate-file))
  (delete-file [this crate-name version]
    (io/delete-file (crate-file-path base-path
                                     crate-name
                                     version)
                    true)))
