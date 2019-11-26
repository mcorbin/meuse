(ns meuse.mirror-test
  (:require [manifold.deferred :as d]
            [meuse.mirror :refer :all]
            [meuse.mocks.store :as mock]
            [clojure.test :refer :all]
            [spy.core :as spy]
            [spy.protocol :as protocol]))

(deftest download-crate-test
  (let [crate-file (.getBytes "foobar")]
    (with-redefs [aleph.http/get (spy/stub
                                  (d/success-deferred {:body crate-file}))]
      (is (= crate-file (download-crate "foo" "1.0.0")))
      (is (spy/called-once-with? aleph.http/get
                                 "https://crates.io/api/v1/crates/foo/1.0.0/download")))))

(deftest download-and-save-test
  (let [store-mock (mock/store-mock {:exists false})
        crate-file (.getBytes "foobar")]
    (with-redefs [aleph.http/get (spy/stub
                                  (d/success-deferred {:body crate-file}))]
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
      (is (spy/called-once-with? aleph.http/get
                                 "https://crates.io/api/v1/crates/foo/1.0.0/download")))))
