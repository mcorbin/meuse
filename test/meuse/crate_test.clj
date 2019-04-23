(ns meuse.crate-test
  (:require [meuse.crate :refer :all]
            [cheshire.core :as json]
            [clojure.test :refer :all])
  (:import clojure.lang.ExceptionInfo))

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

(deftest yank-commit-msg-test
  (is (= ["foo 1.1.2"
          "meuse yank foo 1.1.2"]
         (yank-commit-msg "foo" "1.1.2" true)))
  (is (= ["foo 1.1.2"
          "meuse unyank foo 1.1.2"]
         (yank-commit-msg "foo" "1.1.2" false))))
