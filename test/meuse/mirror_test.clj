(ns meuse.mirror-test
  (:require [meuse.mirror :refer :all]
            [meuse.mocks.store :as mock]
            [clj-http.client :as http]
            [clojure.test :refer :all]
            [spy.core :as spy]
            [spy.protocol :as protocol]))

(deftest download-crate-test
  (let [crate-file (.getBytes "foobar")]
    (with-redefs [http/get (spy/stub
                            {:body crate-file})]
      (is (= crate-file (download-crate "foo" "1.0.0")))
      (is (spy/called-once-with? http/get
                                 "https://crates.io/api/v1/crates/foo/1.0.0/download"
                                 {:as :byte-array})))))

(deftest download-and-save-test
  (let [store-mock (mock/store-mock {:exists false})
        crate-file (.getBytes "foobar")]
    (with-redefs [http/get (spy/stub
                            {:body crate-file})]
      (is (= crate-file
             (download-and-save
              store-mock
              "foo"
              "1.0.0")))
      (is (spy/called-once-with? (:write-file (protocol/spies store-mock))
                                 store-mock
                                 {:name "foo"
                                  :vers "1.0.0"}
                                 crate-file))
      (is (spy/called-once-with? http/get
                                 "https://crates.io/api/v1/crates/foo/1.0.0/download"
                                 {:as :byte-array})))))
