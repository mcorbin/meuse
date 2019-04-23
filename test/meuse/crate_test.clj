(ns meuse.crate-test
  (:require [meuse.crate :refer :all]
            [cheshire.core :as json]
            [clojure.java.io :as io]
            [clojure.test :refer :all])
  (:import clojure.lang.ExceptionInfo
           org.apache.commons.io.FileUtils))

(def tmp-dir "test/resources/tmp/")

(defn tmp-fixture
  [f]
  (FileUtils/deleteDirectory (io/file tmp-dir))
  (f))

(use-fixtures :each tmp-fixture)

(deftest check-size-test
  (is (nil? (check-size (byte-array 10) 10)))
  (is (nil? (check-size (byte-array 10) 9)))
  (is (thrown? ExceptionInfo (nil? (check-size (byte-array 10) 11)))))

(deftest request->crate-test
  (testing "valid request"
    (let [;; size is 13
          metadata (-> {:foo "bar"} json/generate-string .getBytes)
          metadata-length [(byte 13) (byte 0) (byte 0) (byte 0)]
          ;; size is 14
          crate-file (.getBytes "random content")
          crate-file-length [(byte 14) (byte 0) (byte 0) (byte 0)]
          request {:body (byte-array (concat metadata-length
                                             metadata
                                             crate-file-length
                                             crate-file))}]
      (is (= {:metadata {:foo "bar" :yanked false}
              :crate-file (String. crate-file)}
             (-> (request->crate request)
                 (update :crate-file #(String. %)))))))
  (testing "size issue"
    (is (thrown? ExceptionInfo (request->crate {:body
                                               (byte-array
                                                [(byte 20) (byte 0)])})))))

(deftest crate-dir-test
  (is (= "1" (crate-dir "a")))
  (is (= "2" (crate-dir "bb")))
  (is (= "3/a" (crate-dir "aaa")))
  (is (= "3/b" (crate-dir "bbb")))
  (is (= "te/st" (crate-dir "test")))
  (is (= "az/er" (crate-dir "azerty"))))

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

(deftest yanked?->msg-test
  (is (= "yank" (yanked?->msg true)))
  (is (= "unyank" (yanked?->msg false))))

(deftest publush-commit-msg-test
  (is (= ["foo 1.1.2"
          "meuse published foo 1.1.2"]
         (publish-commit-msg {:metadata {:name "foo"
                                         :vers "1.1.2"}}))))

(deftest yank-commit-msg-test
  (is (= ["foo 1.1.2"
          "meuse yank foo 1.1.2"]
         (yank-commit-msg "foo" "1.1.2" true)))
  (is (= ["foo 1.1.2"
          "meuse unyank foo 1.1.2"]
         (yank-commit-msg "foo" "1.1.2" false))))

(deftest crate-file-path-test
  (is (= "/tmp/foo/foobar/1.0.0/download"
         (crate-file-path "/tmp/foo" "foobar" "1.0.0")))
  (is (= "/tmp/foo/foobaz/1.0.3/download"
         (crate-file-path "/tmp/foo" "foobaz" "1.0.3"))))

(deftest save-crate-file-test
  (let [crate {:metadata {:name "test1"
                          :vers "2.3.2"}
               :crate-file (.getBytes "this is the crate file content")}]
    (save-crate-file tmp-dir crate)
    (is (= (slurp (str tmp-dir "/test1/2.3.2/download"))
           (String. (:crate-file crate) java.nio.charset.StandardCharsets/UTF_8)))))

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
