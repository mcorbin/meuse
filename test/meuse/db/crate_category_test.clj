(ns meuse.db.crate-category-test
  (:require [meuse.db :refer [database]]
            [meuse.db.crate :as crate-db]
            [meuse.db.crate-category :refer :all]
            [meuse.db.category :as category-db]
            [meuse.helpers.fixtures :refer :all]
            [clojure.test :refer :all])
  (:import clojure.lang.ExceptionInfo
           java.util.UUID))

(use-fixtures :once db-fixture)
(use-fixtures :each table-fixture)

(deftest create-crate-category-test
  (let [crate-db-id (:crate-id (crate-db/by-name
                                database
                                "crate2"))
        category-db-id (:category-id (category-db/by-name
                                      database
                                      "email"))
        _ (create database crate-db-id "email")
        crate-category (by-crate-and-category database
                                              crate-db-id
                                              category-db-id)]
    (is (= (:category-id crate-category) category-db-id))
    (is (= (:crate-id crate-category) crate-db-id))
    (create database crate-db-id "email"))
  (testing "errors"
    (is (thrown-with-msg? ExceptionInfo
                          #"the category foo does not exist$"
                          (create database
                                  (UUID/randomUUID)
                                  "foo")))))

