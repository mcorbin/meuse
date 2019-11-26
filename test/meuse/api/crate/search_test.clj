(ns meuse.api.crate.search-test
  (:require [meuse.api.crate.search :refer :all]
            [meuse.api.crate.http :refer [crates-api!]]
            [meuse.db :refer [database]]
            [meuse.helpers.fixtures :refer :all]
            [meuse.helpers.request :refer [add-auth]]
            [clojure.test :refer :all])
  (:import java.util.UUID
           clojure.lang.ExceptionInfo))

(use-fixtures :once system-fixture)
(use-fixtures :each table-fixture)

(deftest get-crate-max-version-test
  (is (= {:crates_versions/version "10.0.1"}
         (get-crate-max-version [:foo [{:crates_versions/version "10.0.1"}]])))
  (is (= {:crates_versions/version "10.0.3"}
         (get-crate-max-version [:foo [{:crates_versions/version "10.0.2"}
                                       {:crates_versions/version "10.0.1"}
                                       {:crates_versions/version "10.0.3"}]])))
  (is (= {:crates_versions/version "10.0.10"}
         (get-crate-max-version [:foo [{:crates_versions/version "10.0.2"}
                                       {:crates_versions/version "10.0.10"}
                                       {:crates_versions/version "10.0.3"}]])))
  (is (= {:crates_versions/version "14.5.1"}
         (get-crate-max-version [:foo [{:crates_versions/version "11.0.2"}
                                       {:crates_versions/version "14.5.1"}
                                       {:crates_versions/version "13.9.10"}]]))))

(deftest format-version-test
  (is (= {:name "foo"
          :max_version "1.1.1"
          :description "crate description"}
         (format-version {:crates/name "foo"
                          :crates_versions/version "1.1.1"
                          :crates_versions/description "crate description"
                          :crates_versions/yanked false}))))

(deftest format-search-result-test
  (is (= []
         (format-search-result
          []))
      (= [{:name "foo"
           :max_version "1.0.1"
           :description "a description"}]
         (format-search-result
          [{:crates_versions/description "a description",
            :crates_versions/version "1.0.1",
            :crates/id (UUID/fromString "6f87931e-2194-4d75-b887-370281c4d5bc")
            :crates/name "foo"}])))
  (is (= [{:name "foo"
           :max_version "1.0.10"
           :description "latest description"}
          {:name "bar"
           :max_version "0.2.0"
           :description "bar latest"}]
         (format-search-result
          [{:crates_versions/description "latest description",
            :crates_versions/version "1.0.10",
            :crates/id (UUID/fromString "6f87931e-2194-4d75-b887-370281c4d5bc")
            :crates/name "foo"}
           {:crates_versions/description "a description",
            :crates_versions/version "1.0.1",
            :crates/id (UUID/fromString "6f87931e-2194-4d75-b887-370281c4d5bc")
            :crates/name "foo"}
           {:crates_versions/description "a description",
            :crates_versions/version "0.1.0",
            :crates/id (UUID/fromString "6f87931e-2194-4d75-b887-370281c4d5bc")
            :crates/name "foo"}
           {:crates_versions/description "bar description",
            :crates_versions/version "0.1.0",
            :crates/id (UUID/fromString "6f87931e-2194-4d75-b887-370281c4dccc")
            :crates/name "bar"}
           {:crates_versions/description "bar latest",
            :crates_versions/version "0.2.0",
            :crates/id (UUID/fromString "6f87931e-2194-4d75-b887-370281c4dccc")
            :crates/name "bar"}]))))

(deftest crate-api-search-test
  (testing "success"
    (let [request (add-auth {:action :search
                             :params {:q "foobar"}})
          result (crates-api! request)]
      (is (= {:status 200
              :body {:meta {:total 1}
                     :crates [{:name "crate1"
                               :max_version "1.1.5"
                               :description "the crate1 description, this crate is for foobar"}]}}
             result)))
    (let [request (add-auth {:action :search
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
