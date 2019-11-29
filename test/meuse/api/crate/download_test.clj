(ns meuse.api.crate.download-test
  (:require [meuse.api.crate.download :refer :all]
            [meuse.api.crate.http :refer [crates-api!]]
            [meuse.crate-file :refer [crate-file-store]]
            [meuse.helpers.fixtures :refer :all]
            [meuse.helpers.request :refer [add-auth]]
            [meuse.store.protocol :as store]
            [clojure.test :refer :all])
  (:import clojure.lang.ExceptionInfo))

(use-fixtures :once system-fixture)
(use-fixtures :each tmp-fixture)

(deftest crate-api-download-test
  (testing "success"
    (store/write-file crate-file-store
                      {:name "crate1"
                       :vers "1.1.0"}
                      (.getBytes "file content")))
  (is (= (slurp (:body (crates-api!
                        (add-auth {:action :download
                                   :config {:crate {:path tmp-dir}}
                                   :route-params {:crate-name "crate1"
                                                  :crate-version "1.1.0"}}))))
         "file content"))
  (testing "error"
    (is (thrown-with-msg?
         ExceptionInfo
         #"the file test/resources/tmp/bar/1.0.0/download does not exist"
         (crates-api!
          (add-auth {:action :download
                     :config {:crate {:path tmp-dir}}
                     :route-params {:crate-name "bar"
                                    :crate-version "1.0.0"}})))))
  (testing "invalid parameters"
    (is (thrown-with-msg?
         ExceptionInfo
         #"Wrong input parameters:\n - field crate-name: the value should be a non empty string\n"
         (crates-api! (add-auth {:action :download
                                 :config {:crate {:path tmp-dir}}
                                 :route-params {:crate-name ""
                                                :crate-version "1.0.0"}}))))
    (is (thrown-with-msg?
         ExceptionInfo
         #"Wrong input parameters:\n - field crate-version: the value should be a valid semver string\n"
         (crates-api! (add-auth {:action :download
                                 :config {:crate {:path tmp-dir}}
                                 :route-params {:crate-name "aaaa"
                                                :crate-version "1.1"}}))))))
