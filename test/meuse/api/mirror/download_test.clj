(ns meuse.api.mirror.download-test
  (:require [meuse.api.mirror.download :refer :all]
            [meuse.mocks.store :as mock]
            [spy.core :as spy]
            [spy.protocol :as protocol]
            [clj-http.client :as http]
            [clojure.test :refer :all]))

(deftest download-test
  (testing "serve local file"
    (let [store-mock (mock/store-mock {:exists true
                                       :get-file "foobar"})]
      (is (= {:status 200
              :body "foobar"}
             (download
              store-mock
              {:route-params {:crate-name "foo"
                              :crate-version "1.0.0"}})))
      (is (spy/called-once-with? (:get-file (protocol/spies store-mock))
                                 store-mock
                                 "foo"
                                 "1.0.0"))))
  (testing "cache crate file"
    (let [store-mock (mock/store-mock {:exists false})
          crate-file (.getBytes "foobar")]
      ;; ugly
      (with-redefs [http/get (spy/stub
                                    {:body crate-file})]
        (is (= {:status 200
                :body crate-file}
               (download
                store-mock
                {:route-params {:crate-name "foo"
                                :crate-version "1.0.0"}})))
        (is (spy/called-once-with? (:write-file (protocol/spies store-mock))
                                   store-mock
                                   {:name "foo"
                                    :vers "1.0.0"}
                                   crate-file))
        (is (spy/called-once-with? http/get
                                   "https://crates.io/api/v1/crates/foo/1.0.0/download"
                                   {:as :byte-array}))))))
