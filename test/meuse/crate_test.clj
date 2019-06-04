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
  (let [metadata (-> metadata json/generate-string .getBytes)
        metadata-length [(bit-and (unchecked-byte (count metadata)) 0xFF)
                         (byte 0)
                         (byte 0)
                         (byte 0)]
        crate-file (.getBytes crate-file)
        crate-file-length [(byte (count crate-file)) (byte 0) (byte 0) (byte 0)]]
    {:auth {:user-name "user2"
            :role-name "tech"}
     :body (byte-array (concat metadata-length
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
                    :foo "bar"
                    :deps [{:version_req "0.1.0"
                             :foo "bar"
                             :explicit_name_in_toml "toto"}]
                    :cksum (digest/sha-256 crate-file)}
          request (create-publish-request metadata crate-file)]
      (is (= {:raw-metadata metadata
              :git-metadata {:name "bar"
                             :vers "1.0.1"
                             :yanked false
                             :deps [{:req "0.1.0"
                                     :package "toto"}]
                    :cksum (digest/sha-256 crate-file)}
              :crate-file (String. (.getBytes crate-file))}
             (-> (request->crate request)
                 (update :crate-file #(String. %)))))))
  (testing "size issue"
    (is (thrown-with-msg?
         ExceptionInfo
         #"^invalid request size"
         (request->crate {:body (byte-array [(byte 20) (byte 0)])})))))
