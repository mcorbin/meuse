(ns meuse.crate
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
(defn metadata-file-path
  [base-path crate-name]
  (let [dir (str base-path "/" (crate-dir crate-name))]
    [dir (str dir "/" crate-name)]))

(defn write-metadata
  "Takes a crate and add the metadata into the git directory."
  [base-path crate]
  (let [crate-name (get-in crate [:metadata :name])
        [dir metadata-path] (metadata-file-path base-path crate-name)]
    (when-not (.exists (io/file dir))
      (when-not (io/make-parents metadata-path)
        (throw (ex-info "fail to create directory for crate"
                        {:crate {:name crate-name
                                 :directory dir}}))))
    (spit metadata-path
          (->> (remove #(nil? (second %)) (:metadata crate))
               (into {})
               json/generate-string
               (str "\n"))
          :append true)))

(defn yanked?->msg
  [yanked?]
  (if yanked?
    "yank"
    "unyank"))

(defn publish-commit-msg
  "Creates a commit message from a crate"
  [{:keys [metadata]}]
  [(format "%s %s" (:name metadata) (:vers metadata))
   (format "meuse pushed %s %s" (:name metadata) (:vers metadata))])

(defn yank-commit-msg
  "creates a commit message from a crate"
  [crate-name crate-version yanked?]
  [(format "%s %s" crate-name crate-version)
   (format "meuse %s %s %s"
           (yanked?->msg yanked?)
           crate-name
           crate-version)])

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

(defn update-yank
  "updates the `yanked` field in the metadata file for a crate version"
  [base-path crate-name crate-version yanked?]
  (let [[_ metadata-path] (metadata-file-path base-path crate-name)]
    (->> (string/split (slurp metadata-path) #"\n")
         (map
          (fn [line]
            ;; worst way to patch a file ever
            ;; todo: do something more robust
            (if (string/includes? line (str "\"vers\":\""crate-version"\""))
              (string/replace line
                              (re-pattern (str "\"yanked\":" (not yanked?)))
                              (str "\"yanked\":" yanked?))
              line)))
         (string/join "\n")
         (spit metadata-path))))
