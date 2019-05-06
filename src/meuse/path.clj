(ns meuse.path
  (:require [clojure.java.io :as io])
  (:import java.io.File))

(defn new-path
  "Creates a path."
  [base-path & args]
  (.getPath ^File (apply io/file base-path args)))
