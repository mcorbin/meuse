(ns meuse.store.filesystem
"Manipulates the crate files in the filesystem."
  (:require [meuse.config :as config]
            [meuse.path :as path]
            [meuse.store.protocol :refer [ICrateFile]]
            [mount.core :refer [defstate]]
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

(defn write
  [base-path raw-metadata crate-file]
  (let [path (crate-file-path base-path
                              (:name raw-metadata)
                              (:vers raw-metadata))]
    (io/make-parents path)
    (with-open [w (io/output-stream path)]
      (.write w crate-file))))

(defrecord LocalCrateFile [base-path]
  ICrateFile
  (write-file [this raw-metadata crate-file]
    (write base-path raw-metadata crate-file))

  (get-file [this crate-name version]
    (let [path (crate-file-path
                base-path
                crate-name
                version)
          file (io/file path)]
      (when-not (.exists file)
      (throw (ex-info (format "the file %s does not exist" path)
                      {:type :meuse.error/incorrect})))
      (when (.isDirectory file)
        (throw (ex-info (format "the file %s is a directory" path)
                        {:type :meuse.error/incorrect})))
      file))
  (versions [this crate-name]
    (filesystem-versions base-path crate-name)))
