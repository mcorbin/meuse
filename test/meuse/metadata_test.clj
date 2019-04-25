(ns meuse.metadata-test
  (:require [cheshire.core :as json]
            [meuse.fixtures :refer :all]
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
  (let [crate1 {:metadata {:name "foobar"
                           :version "1.0.0"}}
        crate2 {:metadata {:name "foobar"
                           :version "1.0.1"}}]
    (write-metadata tmp-dir crate1)
    (is (= (slurp (str tmp-dir "/fo/ob/foobar"))
           (str (json/generate-string (:metadata crate1)) "\n")))
    (write-metadata tmp-dir crate2)
    (is (= (slurp (str tmp-dir "/fo/ob/foobar"))
           (str (json/generate-string (:metadata crate1)) "\n"
                (json/generate-string (:metadata crate2)) "\n")))))

(deftest update-yank-test
  (let [crate {:metadata {:name "test1"
                          :vers "2.3.2"
                          :yanked false}}
        check (fn [s] (= s (-> (slurp (str tmp-dir "/te/st/test1"))
                               (json/parse-string true))))]
    (write-metadata tmp-dir crate)
    (is (check (:metadata crate)))
    (update-yank tmp-dir "test1" "2.3.2" true)
    (is (check (assoc (:metadata crate) :yanked true)))
    (update-yank tmp-dir "test1" "2.3.2" false)
    (is (check (:metadata crate)))))

