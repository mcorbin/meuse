(ns meuse.db.category-test
  (:require [meuse.db :refer [database]]
            [meuse.db.category :refer :all]
            [meuse.db.crate :as crate-db]
            [meuse.helpers.fixtures :refer :all]
            [clojure.test :refer :all])
  (:import clojure.lang.ExceptionInfo))

(use-fixtures :once db-fixture)
(use-fixtures :each table-fixture)

(deftest test-create-categories
  (create-category database "email" "category description")
  (is (thrown-with-msg? ExceptionInfo
                          #"already exists$"
                          (create-category database "email" "category description")))
  (let [category (get-category database "email")]
    (is (uuid? (:category-id category)))
    (is (= "email" (:category-name category)))
    (is (= "category description" (:category-description category))))
  (create-category database "food" "another category")
  (let [category (get-category database "food")]
    (is (uuid? (:category-id category)))
    (is (= "food" (:category-name category)))
    (is (= "another category" (:category-description category)))))

(deftest add-crate-category-test
  (let [crate {:name "foo"
               :vers "1.0.1"}
        category-name "email"
        category-description "another category"]
    (testing "success"
      (create-category database
                       category-name
                       category-description)
      (crate-db/new-crate database {:metadata crate})
      (add-crate-category database (:name crate) category-name)
      (let [crate-db-id (:crate-id (crate-db/get-crate database (:name crate)))
            category-db-id (:category-id (get-category database category-name))
            crate-category (get-crate-category database
                                               crate-db-id
                                               category-db-id)]
        (is (= (:category-id crate-category) category-db-id))
        (is (= (:crate-id crate-category) crate-db-id))
        (add-crate-category database (:name crate) category-name)))
    (testing "errors"
      (is (thrown-with-msg? ExceptionInfo
                            #"does not exist$"
                            (add-crate-category database "oups" category-name)))
      (is (thrown-with-msg? ExceptionInfo
                            #"does not exist$"
                            (add-crate-category database
                                                (:crate-name crate)
                                                "oups"))))))
















