(ns meuse.semver
  "Functions to work on crate versions"
  (:require [meuse.log :as log]
            [clojure.string :as string]))

(defn is-pos-int?
  [value]
  (let [int-value (Integer/parseInt value)]
    (when (< int-value 0)
      (throw (ex-info "Negative int value" {:value value})))))

(defn extract-number
  [value]
  (reduce #(if (or (= \- %2)
                   (= \+ %2))
             (reduced %1)
             (str %1 %2))
          ""
          value))

(defn valid?
  [value]
  (try
    (let [splitted (string/split value #"\." 3)]
      (when-not (= 3 (count splitted))
        (throw (ex-info "Invalid semver version" {:value value})))
      (let [patch(->> (last splitted)
                      extract-number)]
        (mapv is-pos-int? [(first splitted) (second splitted) patch])))
    (catch Exception e
      (log/error {} e "Invalid semver version")
      false)))

(defn version->int-vec
  "Takes a semver version, returns a vector of int."
  [version]
  (let [splitted (string/split version #"\." 3)
        patch (extract-number (last splitted))]
    (map #(Integer/parseInt %) [(first splitted) (second splitted) patch])))

(defn string-number-size
  [int-vector]
  (+ 2 (count (reduce str int-vector))))

(defn compare-versions
  "Compares two semver strings."
  [v1 v2]
  (let [v1-vec (version->int-vec v1)
        v2-vec (version->int-vec v2)
        first-comp (cond
                     (= v1-vec v2-vec) 0
                     (< (first v1-vec) (first v2-vec)) -1
                     (> (first v1-vec) (first v2-vec)) 1
                     (< (second v1-vec) (second v2-vec)) -1
                     (> (second v1-vec) (second v2-vec)) 1
                     (< (last v1-vec) (last v2-vec)) -1
                     (> (last v1-vec) (last v2-vec)) 1
                     :else 0)]
    (if (and (zero? first-comp)
             (not= (count v1) (string-number-size v1-vec)))
      (.compareTo (subs v1 (string-number-size v1-vec))
                  (subs v2 (string-number-size v1-vec)))
      first-comp)))
