(ns meuse.semver
  "Functions to work on crate versions"
  (:require [clojure.string :as string]))

(defn valid?
  "Checks if a version is a valid semver version."
  [version]
  (let [splitted (string/split version #"\.")]
    ;; check if the version numbers are integers
    (boolean
     (and (= 3 (count splitted))
          (try
            (mapv #(Integer/parseInt %) splitted)
            (catch Exception e
              false))))))

(defn version->int-vec
  "Takes a semver version, returns a vector of int."
  [version]
  (->> (string/split version #"\.")
       (map #(Integer/parseInt %))))

(defn compare-versions
  "Compares two semver strings."
  [v1 v2]
  (let [v1-vec (version->int-vec v1)
        v2-vec (version->int-vec v2)]
    (cond
      (= v1-vec v2-vec) 0
      (< (first v1-vec) (first v2-vec)) -1
      (> (first v1-vec) (first v2-vec)) 1
      (< (second v1-vec) (second v2-vec)) -1
      (> (second v1-vec) (second v2-vec)) 1
      (< (last v1-vec) (last v2-vec)) -1
      (> (last v1-vec) (last v2-vec)) 1
      :default 0)))
