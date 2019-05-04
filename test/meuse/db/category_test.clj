(ns meuse.db.category-test
  (:require [meuse.db :refer [database]]
            [meuse.db.category :refer :all]
            [meuse.helpers.fixtures :refer :all]
            [clojure.test :refer :all])
  (:import clojure.lang.ExceptionInfo))

(use-fixtures :once db-fixture)
(use-fixtures :each table-fixture)

(deftest test-create-categories
  (create-category database "cat1" "category description")
  (is (thrown-with-msg? ExceptionInfo
                        #"already exists$"
                        (create-category database "email" "category description")))
  (let [category (get-category-by-name database "cat1")]
    (is (uuid? (:category-id category)))
    (is (= "cat1" (:category-name category)))
    (is (= "category description" (:category-description category))))
  (create-category database "cat2" "another category")
  (let [category (get-category-by-name database "cat2")]
    (is (uuid? (:category-id category)))
    (is (= "cat2" (:category-name category)))
    (is (= "another category" (:category-description category)))))















