(ns meuse.api.mirror.cache-test
  (:require [meuse.api.mirror.cache :as cache]
            [meuse.helpers.request :refer [add-auth]]
            [meuse.mocks.store :as mock]
            [spy.core :as spy]
            [spy.protocol :as protocol]
            [clj-http.client :as http]
            [clojure.test :refer :all]))

(deftest cache-test
  (testing "fail if not authenticate"
    (let [store-mock (mock/store-mock {:exists true
                                       :get-file "foobar"})]
      (is (thrown-with-msg?
           Exception
           #"bad permissions"
           (= {:body {:ok true}
               :status 200}
              (cache/cache
               store-mock
               {:route-params {:crate-name "foo"
                               :crate-version "1.0.0"}}))))))
  (testing "serve local file"
    (let [store-mock (mock/store-mock {:exists true
                                       :get-file "foobar"})]
      (is (= {:body {:ok true}
              :status 200}
             (cache/cache
              store-mock
              (add-auth {:route-params {:crate-name "foo"
                                        :crate-version "1.0.0"}}
                        "user2"
                        "tech"))))
      (is (spy/called-once-with? (:exists (protocol/spies store-mock))
                                 store-mock
                                 "foo"
                                 "1.0.0"))
      (is (spy/not-called? (:get-file (protocol/spies store-mock))))))
  (testing "cache crate file"
    (let [store-mock (mock/store-mock {:exists false})
          crate-file (.getBytes "foobar")]
      ;; ugly
      (with-redefs [http/get (spy/stub
                                    {:body crate-file})]
        (is (= {:body {:ok true}
                :status 200}
               (cache/cache
                store-mock
                (add-auth {:route-params {:crate-name "foo"
                                          :crate-version "1.0.0"}}
                          "user2"
                          "tech"))))
        (is (spy/called-once-with? (:write-file (protocol/spies store-mock))
                                   store-mock
                                   {:name "foo"
                                    :vers "1.0.0"}
                                   crate-file))
        (is (spy/called-once-with? http/get
                                   "https://crates.io/api/v1/crates/foo/1.0.0/download"
                                   {:as :byte-array}))))))
