(ns meuse.metadata-test
  (:require [cheshire.core :as json]
            [meuse.helpers.fixtures :refer :all]
            [meuse.helpers.files :refer :all]
            [meuse.metadata :refer :all]
            [clojure.test :refer :all]))

(use-fixtures :each tmp-fixture)

(deftest metadata-dir-test
  (is (= "1" (metadata-dir "a")))
  (is (= "2" (metadata-dir "bb")))
  (is (= "3/a" (metadata-dir "aaa")))
  (is (= "3/b" (metadata-dir "bbb")))
  (is (= "te/st" (metadata-dir "test")))
  (is (= "az/er" (metadata-dir "azerty"))))

(deftest metadata-file-path-test
  (is (= ["/foo/repo/1" "/foo/repo/1/a"] (metadata-file-path "/foo/repo" "a")))
  (is (= ["/foo/repo/2" "/foo/repo/2/bb"] (metadata-file-path "/foo/repo" "bb")))
  (is (= ["/foo/repo/3/a" "/foo/repo/3/a/aze"] (metadata-file-path "/foo/repo" "aze")))
  (is (= ["/foo/repo/to/to" "/foo/repo/to/to/toto"] (metadata-file-path "/foo/repo" "toto"))))

(deftest write-metadata-test
  (let [crate1 {:name "foobar"
                :version "1.0.0"}
        crate2 {:name "foobar"
                :version "1.0.1"}]
    (write-metadata tmp-dir crate1)
    (test-metadata-file (str tmp-dir "/fo/ob/foobar")
                        [crate1])
    (write-metadata tmp-dir crate2)
    (test-metadata-file (str tmp-dir "/fo/ob/foobar")
                        [crate1
                         crate2])))

(deftest replace-yank-test
  (is (= "{\"name\":\"test1\",\"vers\":\"2.3.2\",\"yanked\":false}\n"
         (replace-yank "2.3.2"
                       false
                       "{\"name\":\"test1\",\"vers\":\"2.3.2\",\"yanked\":true}\n")))
  (is (= "{\"name\":\"test1\",\"vers\":\"2.3.2\",\"yanked\":true}\n"
         (replace-yank "2.3.2"
                       true
                       "{\"name\":\"test1\",\"vers\":\"2.3.2\",\"yanked\":false}\n")))
  (is (= (str "{\"name\":\"test1\",\"vers\":\"2.3.1\",\"yanked\":true}\n"
              "{\"name\":\"test1\",\"vers\":\"2.3.2\",\"yanked\":false}\n"
              "{\"name\":\"test1\",\"vers\":\"2.3.3\",\"yanked\":true}\n")
         (replace-yank "2.3.2"
                       false
                       (str "{\"name\":\"test1\",\"vers\":\"2.3.1\",\"yanked\":true}\n"
                            "{\"name\":\"test1\",\"vers\":\"2.3.2\",\"yanked\":true}\n"
                            "{\"name\":\"test1\",\"vers\":\"2.3.3\",\"yanked\":true}\n")))))

(deftest update-yank-test
  (let [crate {:name "test1"
               :vers "2.3.2"
               :yanked false}
        path (str tmp-dir "/te/st/test1")]
    (write-metadata tmp-dir crate)
    (test-metadata-file path [crate])
    (update-yank tmp-dir "test1" "2.3.2" true)
    (test-metadata-file path [(assoc crate :yanked true)])
    (update-yank tmp-dir "test1" "2.3.2" false)
    (test-metadata-file path [crate])))

(deftest versions-test
  (is (= [] (versions tmp-dir "doesnotexist")))
  (write-metadata tmp-dir {:name "test1"
                           :vers "2.3.2"
                           :yanked false})
  (is (= ["2.3.2"]
         (versions tmp-dir "test1")))
  (write-metadata tmp-dir {:name "test1"
                           :vers "2.5.3"
                           :yanked false})
  (write-metadata tmp-dir {:name "test1"
                           :vers "2.5.9"
                           :yanked false})
  (is (= ["2.3.2"
          "2.5.3"
          "2.5.9"]
         (versions tmp-dir "test1"))))
