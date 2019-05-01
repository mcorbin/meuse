(ns meuse.api.crate.search-test
  (:require [meuse.api.crate.search :refer :all]
            [clojure.test :refer :all])
  (:import java.util.UUID))

(deftest format-search-result-test
  (is (= []
         (format-search-result
          []))
      (= [{:name "foo"
           :max_version "1.0.1"
           :description "a description"}]
         (format-search-result
          [{:version-description "a description",
            :version-version "1.0.1",
            :crate-id (UUID/fromString "6f87931e-2194-4d75-b887-370281c4d5bc")
            :crate-name "foo"}])))
  (is (= [{:name "foo"
           :max_version "1.0.10"
           :description "latest description"}
          {:name "bar"
           :max_version "0.2.0"
           :description "bar latest"}]
         (format-search-result
          [{:version-description "latest description",
            :version-version "1.0.10",
            :crate-id (UUID/fromString "6f87931e-2194-4d75-b887-370281c4d5bc")
            :crate-name "foo"}
           {:version-description "a description",
            :version-version "1.0.1",
            :crate-id (UUID/fromString "6f87931e-2194-4d75-b887-370281c4d5bc")
            :crate-name "foo"}
           {:version-description "a description",
            :version-version "0.1.0",
            :crate-id (UUID/fromString "6f87931e-2194-4d75-b887-370281c4d5bc")
            :crate-name "foo"}
           {:version-description "bar description",
            :version-version "0.1.0",
            :crate-id (UUID/fromString "6f87931e-2194-4d75-b887-370281c4dccc")
            :crate-name "bar"}
           {:version-description "bar latest",
            :version-version "0.2.0",
            :crate-id (UUID/fromString "6f87931e-2194-4d75-b887-370281c4dccc")
            :crate-name "bar"}]))))
