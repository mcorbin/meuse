(ns meuse.api.crate.search-test
  (:require [meuse.api.crate.search :refer :all]
            [meuse.api.crate.http :refer [crates-api!]]
            [meuse.db :refer [database]]
            [meuse.helpers.fixtures :refer :all]
            [meuse.helpers.request :refer [add-auth]]
            [clojure.test :refer :all])
  (:import java.util.UUID
           clojure.lang.ExceptionInfo))

(use-fixtures :once db-fixture)
(use-fixtures :each table-fixture)

(deftest get-crate-max-version-test
  (is (= {:version-version "10.0.1"}
         (get-crate-max-version [:foo [{:version-version "10.0.1"}]])))
  (is (= {:version-version "10.0.3"}
         (get-crate-max-version [:foo [{:version-version "10.0.2"}
                                       {:version-version "10.0.1"}
                                       {:version-version "10.0.3"}]])))
  (is (= {:version-version "10.0.10"}
         (get-crate-max-version [:foo [{:version-version "10.0.2"}
                                       {:version-version "10.0.10"}
                                       {:version-version "10.0.3"}]])))
  (is (= {:version-version "14.5.1"}
         (get-crate-max-version [:foo [{:version-version "11.0.2"}
                                       {:version-version "14.5.1"}
                                       {:version-version "13.9.10"}]]))))

(deftest format-version-test
  (is (= {:name "foo"
          :max_version "1.1.1"
          :description "crate description"}
         (format-version {:crate-name "foo"
                          :version-version "1.1.1"
                          :version-description "crate description"
                          :version_yanked false}))))

(deftest format-search-result-test
  (is (= [{:name "foo"
            :max_version "1.3.1"
           :description "crate description"}
          {:name "bar"
            :max_version "3.4.1"
            :description "another description"}]
       (format-search-result [{:crate-name "foo"
                               :version-version "1.1.1"
                               :version-description "crate description"
                               :version_yanked false}
                              {:crate-name "foo"
                               :version-version "1.3.1"
                               :version-description "crate description"
                               :version_yanked false}
                              {:crate-name "foo"
                               :version-version "1.2.1"
                               :version-description "crate description"
                               :version_yanked false}
                              {:crate-name "bar"
                               :version-version "3.3.1"
                               :version-description "another description"
                               :version_yanked false}
                              {:crate-name "bar"
                               :version-version "3.4.1"
                               :version-description "another description"
                               :version_yanked false}]))))

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

(deftest crate-api-search-test
  (testing "success"
    (let [request (add-auth {:action :search
                             :database database
                             :params {:q "foobar"}})
          result (crates-api! request)]
      (is (= {:status 200
              :body {:meta {:total 1}
                     :crates [{:name "crate1"
                               :max_version "1.1.5"
                               :description "the crate1 description, this crate is for foobar"}]}}
             result)))
    (let [request (add-auth {:action :search
                             :database database
                             :params {:q "description"}})
          result (crates-api! request)]
      (is (= {:status 200
              :body {:meta {:total 2}
                     :crates [{:name "crate1"
                               :max_version "1.1.5"
                               :description "the crate1 description, this crate is for foobar"}
                              {:name "crate2"
                               :max_version "1.3.0"
                               :description "the crate2 description, this crate is for barbaz"}]}}
             result)))
    (let [request (add-auth {:action :search
                             :database database
                             :params {:q "description" :per_page "1"}})
          result (crates-api! request)]
      (is (= {:status 200
              :body {:meta {:total 1}
                     :crates [{:name "crate1"
                               :max_version "1.1.5"
                               :description "the crate1 description, this crate is for foobar"}]}}
             result))))
  (testing "invalid params"
    (is (thrown-with-msg?
         ExceptionInfo
         #"Wrong input parameters:\n - field q: the value should be a non empty string\n"
         (crates-api! (add-auth {:action :search
                                 :params {:q ""}})))))
  (testing "invalid params"
    (is (thrown-with-msg?
         ExceptionInfo
         #"Wrong input parameters:\n - field q missing in params\n"
         (crates-api! (add-auth {:action :search
                                 :params {}})))))
  (testing "invalid params"
    (is (thrown-with-msg?
         ExceptionInfo
         #"Wrong input parameters:\n - field per_page is incorrect\n"
         (crates-api! (add-auth {:action :search
                                 :params {:q "foo" :per_page "aaa"}}))))))
