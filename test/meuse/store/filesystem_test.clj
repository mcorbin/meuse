(ns meuse.store.filesystem-test
  (:require [meuse.store.filesystem :refer :all]
            [meuse.helpers.fixtures :refer :all]
            [meuse.helpers.files :refer :all]
            [clojure.test :refer :all]))

(use-fixtures :each tmp-fixture)

(deftest crate-file-path-test
  (is (= "/tmp/foo/foobar/1.0.0/download"
         (crate-file-path "/tmp/foo" "foobar" "1.0.0")))
  (is (= "/tmp/foo/foobar/1.0.0/download"
         (crate-file-path "/tmp/foo/" "foobar" "1.0.0")))
  (is (= "/tmp/foo/foobaz/1.0.3/download"
         (crate-file-path "/tmp/foo" "foobaz" "1.0.3"))))

(deftest write-crate-file-test
  (let [crate {:raw-metadata {:name "test1"
                              :vers "2.3.2"}
               :crate-file (.getBytes "this is the crate file content")}]
    (write tmp-dir (:raw-metadata crate) (:crate-file crate))
    (test-crate-file (str tmp-dir "/test1/2.3.2/download")
                     (:crate-file crate))))

(deftest versions-test

  (write tmp-dir
         {:name "test1"
          :vers "2.3.2"}
         (.getBytes "roflmap"))
  (is (= {"2.3.2" true}
         (filesystem-versions tmp-dir "test1")))
  (write tmp-dir
         {:name "test1"
          :vers "2.3.4"}
         (.getBytes "roflmap"))
  (is (= {"2.3.2" true
          "2.3.4" true}
         (filesystem-versions tmp-dir "test1")))
  (clojure.java.io/delete-file (str tmp-dir "/test1/2.3.4/download"))
  (is (= {"2.3.2" true
          "2.3.4" false}
         (filesystem-versions tmp-dir "test1"))))
