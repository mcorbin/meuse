(ns meuse.helpers.files
  (:require [meuse.metadata :refer :all]
            [cheshire.core :as json]
            [clojure.string :as string]
            [clojure.test :refer :all]))

(defn test-metadata-file
  [path expected]
  (is (= (slurp path)
         (str (->> (map json/generate-string expected)
                   (string/join "\n")) "\n"))))

(defn test-crate-file
  [path file-bytes]
  (is (= (slurp path)
         (String. file-bytes java.nio.charset.StandardCharsets/UTF_8))))
