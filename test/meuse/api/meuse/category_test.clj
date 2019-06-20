(ns meuse.api.meuse.category-test
  (:require [meuse.api.meuse.category :refer :all]
            [meuse.api.meuse.http :refer :all]
            [meuse.db.category :refer :all]
            [meuse.db.crate :as crate-db]
            [meuse.db :refer [database]]
            [meuse.helpers.fixtures :refer :all]
            [meuse.helpers.request :refer [add-auth]]
            [clojure.test :refer :all])
  (:import clojure.lang.ExceptionInfo))

(use-fixtures :once db-fixture)
(use-fixtures :each table-fixture)

(deftest ^:integration new-category-test
  (let [request (add-auth {:database database
                           :action :new-category
                           :body {:name "foo"
                                  :description "the description"}}
                          "user1"
                          "admin")]
    (is (= {:status 200} (meuse-api! request)))
    (let [category (by-name database "foo")]
      (is (uuid? (:category-id category)))
      (is (= "foo" (:category-name category)))
      (is (= "the description" (:category-description category))))
    (is (thrown-with-msg? ExceptionInfo
                          #"already exists$"
                          (meuse-api! request))))
  (testing "invalid parameters"
    (is (thrown-with-msg?
         ExceptionInfo
         #"Wrong input parameters:\n - field name missing in body\n"
         (meuse-api! {:action :new-category
                      :body {:description "the description"}}))))
  (testing "non-admin"
    (is (thrown-with-msg?
         ExceptionInfo
         #"bad permissions"
         (meuse-api! (add-auth {:database database
                                :action :new-category
                                :body {:name "foo"
                                       :description "the description"}}
                               "user2"
                               "tech"))))))
