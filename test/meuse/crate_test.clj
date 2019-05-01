(ns meuse.crate-test
  (:require [meuse.crate :refer :all]
            [meuse.helpers.fixtures :refer :all]
            [cheshire.core :as json]
            [digest :as digest]
            [clojure.java.io :as io]
            [clojure.test :refer :all])
  (:import clojure.lang.ExceptionInfo))

(use-fixtures :each tmp-fixture)

(defn create-publish-request
  [metadata crate-file]
  (let [;; size is 13
        metadata (-> metadata json/generate-string .getBytes)
        metadata-length [(byte (count metadata)) (byte 0) (byte 0) (byte 0)]
        ;; size is 14
        crate-file (.getBytes crate-file)
        crate-file-length [(byte (count crate-file)) (byte 0) (byte 0) (byte 0)]
        request {:body (byte-array (concat metadata-length
                                           metadata
                                           crate-file-length
                                           crate-file))}]
    {:body (byte-array (concat metadata-length
                               metadata
                               crate-file-length
                               crate-file))}))

(deftest check-size-test
  (is (nil? (check-size (byte-array 10) 10)))
  (is (nil? (check-size (byte-array 10) 9)))
  (is (thrown-with-msg?
       ExceptionInfo
       #"^invalid request size"
       (nil? (check-size (byte-array 10) 11)))))

(deftest request->crate-test
  (testing "valid request"
    (let [crate-file "random content"
          metadata {:name "bar"
                    :vers "1.0.1"
                    :yanked false
                    :cksum (digest/sha-256 crate-file)}
          request (create-publish-request metadata crate-file)]
      (is (= {:metadata metadata
              :crate-file (String. (.getBytes crate-file))}
             (-> (request->crate request)
                 (update :crate-file #(String. %)))))))
  (testing "size issue"
    (is (thrown-with-msg?
         ExceptionInfo
         #"^invalid request size"
         (request->crate {:body (byte-array [(byte 20) (byte 0)])})))))
