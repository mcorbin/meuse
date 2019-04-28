(ns meuse.crate
  "Crate utility functions"
  (:require [byte-streams :as bs]
            [cheshire.core :as json]
            [clojure.java.io :as io]
            [clojure.tools.logging :refer [debug info error]]
            [clojure.string :as string])
  (:import java.util.Arrays))

(defn check-size
  "Takes a request, a byte array and a size.
  Throws an exception if the byte array size is lower than the size."
  [byte-array size]
  (when (< (alength byte-array) size)
    (throw (ex-info (format "invalid request size %d" (alength byte-array))
                    {}))))

(defn request->crate
  "Takes an HTTP publish request. Converts the payload to a map containing
  the metadata and the crate file."
  [request]
  (let [byte-array (bs/to-byte-array (:body request))
        _ (check-size byte-array 8)
        metadata-size (+ (bit-shift-left (aget byte-array 3) 24)
                         (bit-shift-left (aget byte-array 2) 16)
                         (bit-shift-left (aget byte-array 1) 8)
                         (aget byte-array 0))
        _ (check-size byte-array (+ 4 metadata-size))
        metadata (Arrays/copyOfRange byte-array 4 (+ 4 metadata-size))
        crate-size (+ (bit-shift-left (aget byte-array (+ metadata-size 4 3)) 24)
                      (bit-shift-left (aget byte-array (+ metadata-size 4 2)) 16)
                      (bit-shift-left (aget byte-array (+ metadata-size 4 1)) 8)
                      (aget byte-array (+ metadata-size 4)))
        _ (check-size byte-array (+ metadata-size 4 crate-size))
        crate-file (Arrays/copyOfRange byte-array
                                       (+ 4 metadata-size 4)
                                       (+ 4 metadata-size 4 crate-size))]
    {:metadata (-> (String. metadata)
                   (json/parse-string true)
                   (assoc :yanked false))
     :crate-file crate-file}))
