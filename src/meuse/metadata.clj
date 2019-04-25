(ns meuse.metadata
  (:require [cheshire.core :as json]
            [clojure.java.io :as io]
            [clojure.tools.logging :refer [debug info error]]
            [clojure.string :as string]))

(defn metadata-dir
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
  (let [dir (str base-path "/" (metadata-dir crate-name))]
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
          (str (->> (:metadata crate)
                    (remove #(nil? (second %)))
                    (into {})
                    json/generate-string)
               "\n")
          :append true)))

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
