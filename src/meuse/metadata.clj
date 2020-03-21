(ns meuse.metadata
  "Functions to interacts with the crate metadata file."
  (:require [meuse.path :as path]
            [cheshire.core :as json]
            [exoscale.ex :as ex]
            [clojure.java.io :as io]
            [clojure.string :as string]))

(defn metadata-dir
  "Takes a crate name and returns the directory on which the metadata
  file should be created."
  [crate-name]
  (condp = (count crate-name)
    1 "1"
    2 "2"
    3 (str "3/" (first crate-name))
    (str (subs crate-name 0 2) "/" (subs crate-name 2 4))))

(defn metadata-file-path
  "Takes a path and a crate name, returns the path to the metadata file."
  [base-path crate-name]
  (let [dir (path/new-path base-path (metadata-dir crate-name))]
    [dir (path/new-path dir crate-name)]))

(defn write-metadata
  "Takes a crate and add the metadata into the git directory."
  [base-path metadata]
  (let [crate-name (:name metadata)
        [dir metadata-path] (metadata-file-path base-path crate-name)]
    (when-not (.exists (io/file dir))
      (when-not (io/make-parents metadata-path)
        (throw (ex/ex-fault "fail to create directory for crate"
                            {:crate {:name crate-name
                                     :directory dir}}))))
    (spit metadata-path
          (str (->> metadata
                    (remove #(nil? (second %)))
                    (into {})
                    json/generate-string)
               "\n")
          :append true)))

(defn replace-yank
  "Takes a crate version, a boolean indicating if the crate should be
  yanked or not, and the content of the metadata file for this crate.
  Returns the file content with the `yanked` field updated."
  [crate-version yanked? file-content]
  (str (->> (string/split file-content #"\n")
            (map
             (fn [line]
               ;; worst way to patch a file ever
               ;; todo: do something more robust
               (if (string/includes? line (str "\"vers\":\"" crate-version "\""))
                 (string/replace line
                                 (re-pattern (str "\"yanked\":" (not yanked?)))
                                 (str "\"yanked\":" yanked?))
                 line)))
            (string/join "\n"))
       "\n"))

(defn update-yank
  "updates the `yanked` field in the metadata file for a crate version"
  [base-path crate-name crate-version yanked?]
  (let [[_ metadata-path] (metadata-file-path base-path crate-name)]
    (->> (slurp metadata-path)
         ((partial replace-yank crate-version yanked?))
         (spit metadata-path))))

(defn versions
  "Returns a list of the versions which exists on the metadata file.
  If the file does not exist, returns an empty vector."
  [base-path crate-name]
  (let [[_ metadata-path] (metadata-file-path base-path crate-name)]
    (if (.exists (io/file metadata-path))
      (->> (string/split (slurp metadata-path) #"\n")
           (map #(json/parse-string % true))
           (map :vers))
      [])))
