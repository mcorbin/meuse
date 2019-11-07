(ns meuse.crate-test
  (:require [meuse.crate :refer :all]
            [meuse.crate-file :as crate-file]
            [meuse.db.actions.crate :as crate-action]
            [meuse.db.public.crate :refer [crate-db]]
            [meuse.git :refer [git]]
            [meuse.metadata :as metadata]
            [meuse.helpers.fixtures :refer :all]
            [cheshire.core :as json]
            [digest :as digest]
            [clojure.java.io :as io]
            [clojure.test :refer :all]
            [spy.assert :as assert]
            [spy.core :as spy])
  (:import clojure.lang.ExceptionInfo))

(use-fixtures :once inject-fixture)
(use-fixtures :each tmp-fixture)

(defn create-publish-request
  [metadata crate-file]
  (let [metadata (-> metadata json/generate-string .getBytes)
        metadata-length [(bit-and (unchecked-byte (count metadata)) 0xFF)
                         (byte 0)
                         (byte 0)
                         (byte 0)]
        crate-file (.getBytes ^String crate-file)
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
                 (update :crate-file #(String. #^bytes %)))))))
  (testing "size issue"
    (is (thrown-with-msg?
         ExceptionInfo
         #"^invalid request size"
         (request->crate {:body (byte-array [(byte 20) (byte 0)])})))))

(deftest verify-versions-test
  (let [f (verify-versions tmp-dir tmp-dir)]
    (testing "metadata/files don't exist"
      (is (= [{:crate "foo"
               :errors ["metadata does not exist for version 1.1.0"
                        "crate binary file does not exist for version 1.1.0"]}]
             (f [] ["foo" [{:version-version "1.1.0"}]]))))
    (testing "files exists"
      (crate-file/write-crate-file tmp-dir {:raw-metadata {:name "foo"
                                                           :vers "1.1.0"}
                                            :crate-file (.getBytes "lol")})
      (metadata/write-metadata tmp-dir {:name "foo"
                                        :vers "1.1.0"})
      (is (= [{:crate "foo"
               :errors []}]
             (f [] ["foo" [{:version-version "1.1.0"}]]))))
    (testing "metadata exists but should not"
      (metadata/write-metadata tmp-dir {:name "foo"
                                        :vers "1.1.3"})
      (is (= [{:crate "foo"
               :errors ["metata exists but not in the database for version 1.1.3"]}]
             (f [] ["foo" [{:version-version "1.1.0"}]]))))
    (testing "crate-file exists but should not"
      (crate-file/write-crate-file tmp-dir {:raw-metadata {:name "foo"
                                                           :vers "1.1.3"}
                                            :crate-file (.getBytes "lol")})
      (is (= [{:crate "foo"
               :errors ["metata exists but not in the database for version 1.1.3"
                        "crate binary file exists but not in the db for version 1.1.3"]}]
             (f [] ["foo" [{:version-version "1.1.0"}]]))))
    (testing "success: pass multiple versions"
      (is (= [{:crate "foo"
               :errors []}]
             (f [] ["foo" [{:version-version "1.1.0"}
                           {:version-version "1.1.3"}]]))))
    (testing "the binary file does not exist for a version"
      (clojure.java.io/delete-file (str tmp-dir "/foo/1.1.3/download"))
      (is (= [{:crate "foo"
               :errors ["missing crate binary file for version 1.1.3"]}]
             (f [] ["foo" [{:version-version "1.1.0"}
                           {:version-version "1.1.3"}]]))))))

(deftest check-test
  (let [request {:git {:lock "foo"}
                 :config {:crate {:path tmp-dir}
                          :metadata {:path tmp-dir}}}]
    (testing "metadata/files don't exist"
      (with-redefs [crate-action/get-crates-and-versions
                    (spy/stub [{:crate-name "foo"
                                :version-version "1.1.0"}])]
        (is (= [{:crate "foo"
                 :errors ["metadata does not exist for version 1.1.0"
                          "crate binary file does not exist for version 1.1.0"]}]
               (check crate-db git request)))))
    (testing "files exists"
      (crate-file/write-crate-file tmp-dir {:raw-metadata {:name "foo"
                                                           :vers "1.1.0"}
                                            :crate-file (.getBytes "lol")})
      (metadata/write-metadata tmp-dir {:name "foo"
                                        :vers "1.1.0"})
      (with-redefs [crate-action/get-crates-and-versions
                    (spy/stub [{:crate-name "foo"
                                :version-version "1.1.0"}])]
        (is (= []
               (check crate-db git request)))))
    (testing "metadata exists but should not"
      (metadata/write-metadata tmp-dir {:name "foo"
                                        :vers "1.1.3"})
      (with-redefs [crate-action/get-crates-and-versions
                    (spy/stub [{:crate-name "foo"
                                :version-version "1.1.0"}])]
        (is (= [{:crate "foo"
                 :errors ["metata exists but not in the database for version 1.1.3"]}]
               (check crate-db git request)))))
    (testing "crate-file exists but should not"
      (crate-file/write-crate-file tmp-dir {:raw-metadata {:name "foo"
                                                           :vers "1.1.3"}
                                            :crate-file (.getBytes "lol")})
      (with-redefs [crate-action/get-crates-and-versions
                    (spy/stub [{:crate-name "foo"
                                :version-version "1.1.0"}])]
        (is (= [{:crate "foo"
                 :errors ["metata exists but not in the database for version 1.1.3"
                          "crate binary file exists but not in the db for version 1.1.3"]}]
               (check crate-db git request)))))
    (testing "success: pass multiple versions"
      (with-redefs [crate-action/get-crates-and-versions
                    (spy/stub [{:crate-name "foo"
                                :version-version "1.1.0"}
                               {:crate-name "foo"
                                :version-version "1.1.3"}])]
        (is (= []
               (check crate-db git request)))))
    (testing "the binary file does not exist for a version"
      (clojure.java.io/delete-file (str tmp-dir "/foo/1.1.3/download"))
      (with-redefs [crate-action/get-crates-and-versions
                    (spy/stub [{:crate-name "foo"
                                :version-version "1.1.0"}
                               {:crate-name "foo"
                                :version-version "1.1.3"}])]
        (is (= [{:crate "foo"
                 :errors ["missing crate binary file for version 1.1.3"]}]
               (check crate-db git request)))))
    (testing "two crates, one in error"
      (crate-file/write-crate-file tmp-dir {:raw-metadata {:name "foo"
                                                           :vers "1.1.3"}
                                            :crate-file (.getBytes "lol")})
      (with-redefs [crate-action/get-crates-and-versions
                    (spy/stub [{:crate-name "foo"
                                :version-version "1.1.0"}
                               {:crate-name "bar"
                                :version-version "3.0.1"}
                               {:crate-name "foo"
                                :version-version "1.1.3"}])]
        (is (= [{:crate "bar"
                 :errors ["metadata does not exist for version 3.0.1"
                          "crate binary file does not exist for version 3.0.1"]}]
               (check crate-db git request)))))
    (testing "two crates, success"
      (crate-file/write-crate-file tmp-dir {:raw-metadata {:name "bar"
                                                           :vers "3.0.1"}
                                            :crate-file (.getBytes "lol")})
      (metadata/write-metadata tmp-dir {:name "bar"
                                        :vers "3.0.1"})
      (with-redefs [crate-action/get-crates-and-versions
                    (spy/stub [{:crate-name "foo"
                                :version-version "1.1.0"}
                               {:crate-name "bar"
                                :version-version "3.0.1"}
                               {:crate-name "foo"
                                :version-version "1.1.3"}])]
        (is (= []
               (check crate-db git request)))))
    (testing "two crates in error"
      (clojure.java.io/delete-file (str tmp-dir "/foo/1.1.3/download"))
      (clojure.java.io/delete-file (str tmp-dir "/bar/3.0.1/download"))
      (with-redefs [crate-action/get-crates-and-versions
                    (spy/stub [{:crate-name "foo"
                                :version-version "1.1.0"}
                               {:crate-name "bar"
                                :version-version "3.0.1"}
                               {:crate-name "foo"
                                :version-version "1.1.3"}])]
        (is (= [{:crate "foo"
                 :errors ["missing crate binary file for version 1.1.3"]}
                {:crate "bar"
                 :errors ["missing crate binary file for version 3.0.1"]}]
               (check crate-db git request)))))))
