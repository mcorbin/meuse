(ns meuse.db.actions.crate-category-test
  (:require [meuse.db :refer [database]]
            [meuse.db.actions.crate :as crate-db]
            [meuse.db.actions.crate-category :refer :all]
            [meuse.db.actions.category :as category-db]
            [meuse.db.actions.user :as user-db]
            [meuse.helpers.fixtures :refer :all]
            [clojure.test :refer :all])
  (:import clojure.lang.ExceptionInfo
           java.util.UUID))

(use-fixtures :once system-fixture)
(use-fixtures :each db-clean-fixture table-fixture)

(deftest create-crate-category-test
  (let [crate-db-id (:crates/id (crate-db/by-name
                                 database
                                 "crate2"))
        category-db-id (:categories/id (category-db/by-name
                                        database
                                        "email"))
        _ (create database crate-db-id "email")
        crate-category (by-crate-and-category database
                                              crate-db-id
                                              category-db-id)]
    (is (= (:crates_categories/category_id crate-category) category-db-id))
    (is (= (:crates_categories/crate_id crate-category) crate-db-id))
    (create database crate-db-id "email"))
  (testing "errors"
    (is (thrown-with-msg? ExceptionInfo
                          #"the category foo does not exist$"
                          (create database
                                  (UUID/randomUUID)
                                  "foo")))))

(deftest create-categories-test
  (let [crate-db-id (:crates/id (crate-db/by-name
                                 database
                                 "crate2"))
        email-db-id (:categories/id (category-db/by-name
                                     database
                                     "email"))
        system-db-id (:categories/id (category-db/by-name
                                      database
                                      "system"))
        _ (create-categories database crate-db-id ["system" "email"])
        crate-category-email (by-crate-and-category database
                                                    crate-db-id
                                                    email-db-id)
        crate-category-system (by-crate-and-category database
                                                     crate-db-id
                                                     system-db-id)]
    (is (= (:crates_categories/category_id crate-category-email) email-db-id))
    (is (= (:crates_categories/crate_id crate-category-email) crate-db-id))
    (is (= (:crates_categories/category_id crate-category-system) system-db-id))
    (is (= (:crates_categories/crate_id crate-category-system) crate-db-id))
    (create-categories database crate-db-id ["system" "email"]))
  (testing "errors"
    (is (thrown-with-msg? ExceptionInfo
                          #"the category foo does not exist$"
                          (create-categories database
                                             (UUID/randomUUID)
                                             ["foo"])))))
