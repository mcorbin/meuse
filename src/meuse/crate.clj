(ns meuse.crate
  (:require [byte-streams :as bs]
            [cheshire.core :as json]
            [clojure.java.io :as io]
            [clojure.tools.logging :refer [debug info error]])
  (:import java.util.Arrays))

(defn check-size
  "Takes a request, a byte array and a size.
  Throws an exception if the byte array size is lower than the size."
  [request byte-array size]
  (when (< (alength byte-array) size)
    (throw (ex-info (format "invalid request size %d" (alength byte-array))
                    {}))))

(defn request->crate
  "Takes an HTTP publish request. Converts the payload to a map containing
  the metadata and the crate file."
  [request]
  (let [byte-array (bs/to-byte-array (:body request))
        _ (check-size request byte-array 8)
        metadata-size (+ (bit-shift-left (aget byte-array 3) 24)
                         (bit-shift-left (aget byte-array 2) 16)
                         (bit-shift-left (aget byte-array 1) 8)
                         (aget byte-array 0)
                         4)
        _ (check-size request byte-array metadata-size)
        metadata (Arrays/copyOfRange byte-array 4 metadata-size)
        crate-size (+ (bit-shift-left (aget byte-array (+ metadata-size 3)) 24)
                      (bit-shift-left (aget byte-array (+ metadata-size 2)) 16)
                      (bit-shift-left (aget byte-array (inc metadata-size)) 8)
                      (aget byte-array metadata-size)
                      4)
        _ (check-size request byte-array crate-size)
        crate-file (Arrays/copyOfRange byte-array metadata-size crate-size)]
    {:metadata (json/parse-string (String. metadata) true)
     :crate-file crate-file}))

(defn crate-dir
  "Takes a crate name and returns the directory on which the metadata
  file should be created"
  [crate-name]
  (condp = (count crate-name)
    1 "1"
    2 "2"
    3 (str "3/" (first crate-name))
    (str (subs crate-name 0 2) "/" (subs crate-name 2 4))))

;; todo: creating path with str is not nice
(defn write-metadata
  "Takes a crate and add the metadata into the git directory."
  [path crate]
  (let [crate-name (get-in crate [:metadata :name])
        dir (str path "/" (crate-dir crate-name))
        metadata-path (str dir "/" crate-name)]
    (when-not (.exists (io/file dir))
      (when-not (io/make-parents metadata-path)
        (throw (ex-info "fail to create directory for crate"
                        {:crate {:name crate-name
                                 :directory dir}}))))
    (spit metadata-path
          (-> (:metadata crate)
              json/generate-string
              (str "\n"))
          :append true)))

(defn commit-msg
  "Creates a commit message from a crate"
  [{:keys [metadata]}]
  [(format "%s %s" (:name metadata) (:vers metadata))
   (format "meuse pushed %s %s" (:name metadata) (:vers metadata))])

(defn save-crate-file
  "Takes a crate file and its metadata, saves the crate file on disk."
  [path {:keys [metadata crate-file]}]
  (let [path (str path "/" (:name metadata) "/" (:vers metadata) "/download")]
    (io/make-parents path)
    (spit path
          (String. crate-file java.nio.charset.StandardCharsets/UTF_8))))

