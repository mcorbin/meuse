(ns meuse.crate-file-test
  (:require [meuse.crate-file :refer :all]
            [meuse.helpers.fixtures :refer :all]
            [meuse.helpers.files :refer :all]
            [clojure.test :refer :all]))

(use-fixtures :each tmp-fixture)

(deftest crate-file-path-test
  (is (= "/tmp/foo/foobar/1.0.0/download"
         (crate-file-path "/tmp/foo" "foobar" "1.0.0")))
  (is (= "/tmp/foo/foobaz/1.0.3/download"
         (crate-file-path "/tmp/foo" "foobaz" "1.0.3"))))

(deftest save-crate-file-test
  (let [crate {:raw-metadata {:name "test1"
                              :vers "2.3.2"}
               :crate-file (.getBytes "this is the crate file content")}]
    (save-crate-file tmp-dir crate)
    (test-crate-file (str tmp-dir "/test1/2.3.2/download")
                     (:crate-file crate))))
