(ns meuse.registry-test
  (:require [cheshire.core :as json]
            [meuse.registry :refer :all]
            [meuse.helpers.fixtures :refer :all]
            [clojure.java.io :as io]
            [clojure.test :refer :all])
  (:import clojure.lang.ExceptionInfo))

(use-fixtures :each tmp-fixture)

(def registry-config
  {:dl "https://my-crates-server.com/api/v1/crates/{crate}/{version}/download"
   :api "https://my-crates-server.com/"
   :allowed-registries ["https://github.com/rust-lang/crates.io-index"
                        "https://my-intranet:8080/index"]})

(deftest read-registry-config-test
  (let [config-path (.getPath (io/file tmp-dir "config.json"))]
    (is (thrown-with-msg?
         ExceptionInfo
         (re-pattern (format "the file %s does not exist" config-path))
         (read-registry-config tmp-dir "default"))))
  (testing "non empty allowed-registries"
    (spit (str tmp-dir "config.json") (json/generate-string registry-config))
    (is (= (read-registry-config tmp-dir "default")
           {:dl "https://my-crates-server.com/api/v1/crates/{crate}/{version}/download"
            :api "https://my-crates-server.com/"
            :allowed-registries ["https://github.com/rust-lang/crates.io-index"
                                 "https://my-intranet:8080/index"]})))
  (testing "empty allowed-registries"
    (spit (str tmp-dir "config.json") (json/generate-string
                                       (dissoc registry-config :allowed-registries)))
    (is (= (read-registry-config tmp-dir "default")
           {:dl "https://my-crates-server.com/api/v1/crates/{crate}/{version}/download"
            :api "https://my-crates-server.com/"
            :allowed-registries ["default"]}))))

(deftest allowed-registry?-test
  (is (allowed-registry? {:deps [{:registry
                                  "https://github.com/rust-lang/crates.io-index"}]}
                         (:allowed-registries registry-config)))
  (is (allowed-registry? {:deps [{:registry
                                  "https://my-intranet:8080/index"}]}
                         (:allowed-registries registry-config)))
  (is (allowed-registry? {}
                         (:allowed-registries registry-config)))
  (is (thrown-with-msg?
       ExceptionInfo
       #"no registry allowed"
       (allowed-registry? {:deps [{:registry
                                   "foo"}]}
                          nil)))
  (is (thrown-with-msg?
       ExceptionInfo
       #"the registry foo is not allowed"
       (allowed-registry? {:deps [{:registry
                                   "foo"}]}
                          (:allowed-registries registry-config)))))
