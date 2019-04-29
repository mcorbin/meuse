(ns meuse.db.crate-test
  (:require [meuse.db :refer [database]]
            [meuse.db.crate :refer :all]
            [meuse.db.category :as category-db]
            [meuse.helpers.db :refer :all]
            [meuse.helpers.fixtures :refer :all]
            [clojure.test :refer :all])
  (:import clojure.lang.ExceptionInfo
           java.util.UUID))

(use-fixtures :once db-fixture)
(use-fixtures :each table-fixture)

(deftest ^:integration create-crate-test
  (let [crate {:metadata {:name "test1"
                          :vers "0.1.3"
                          :yanked false}}]
    (create-crate database crate)
    (is (thrown-with-msg? ExceptionInfo
                          #"already exists$"
                          (create-crate database crate)))
    (test-crate-version database {:crate-name "test1"
                                  :version-version "0.1.3"
                                  :version-yanked false
                                  :version-description nil})
    (test-crate database {:crate-name "test1"})
    (create-crate database (assoc-in crate [:metadata :vers] "2.0.0"))
    (test-crate-version database {:crate-name "test1"
                                  :version-version "2.0.0"
                                  :version-yanked false
                                  :version-description nil})
    (test-crate database {:crate-name "test1"})))

(deftest ^:integration create-crate-with-categories
  (let [crate {:metadata {:name "test1"
                          :vers "0.1.3"
                          :categories ["email" "system"]
                          :yanked false}}]
    (category-db/create-category database "email" "description 1")
    (category-db/create-category database "system" "description 2")
    (create-crate database crate)))

(deftest ^:integration update-yank-test
  (testing "success"
    (let [crate {:metadata {:name "test1"
                            :vers "0.1.3"
                            :yanked false}}]
      (create-crate database crate)
      (test-crate-version database {:crate-name "test1"
                                    :version-version "0.1.3"
                                    :version-yanked false
                                    :version-description nil})
      (update-yank database "test1" "0.1.3" true)
      (test-crate-version database {:crate-name "test1"
                                    :version-version "0.1.3"
                                    :version-yanked true
                                    :version-description nil})
      (update-yank database "test1" "0.1.3" false)
      (test-crate-version database {:crate-name "test1"
                                    :version-version "0.1.3"
                                    :version-yanked false
                                    :version-description nil})))
  (testing "error"
    (let [crate {:metadata {:name "test3"
                            :vers "0.1.3"
                            :yanked false}}]
      (is (thrown-with-msg? ExceptionInfo
                            #"the crate does not exist$"
                            (update-yank database "test3" "0.1.3" false)))
      (create-crate database crate)
      (is (thrown-with-msg? ExceptionInfo
                            #"the version does not exist$"
                            (update-yank database "test3" "0.1.4" false))))))

(deftest ^:integration create-crate-category-test
  (let [crate {:name "foo"
               :vers "1.0.1"}
        category-name "email"
        category-description "another category"]
    (testing "success"
      (category-db/create-category database
                       category-name
                       category-description)
      (create-crate database {:metadata crate})
      (let [crate-db-id (:crate-id (get-crate-by-name
                                    database (:name crate)))
            category-db-id (:category-id (category-db/get-category-by-name
                                          database
                                          category-name))
            _  (create-crate-category database crate-db-id category-name)
            crate-category (get-crate-category database
                                               crate-db-id
                                               category-db-id)]
        (is (= (:category-id crate-category) category-db-id))
        (is (= (:crate-id crate-category) crate-db-id))
        (create-crate-category database crate-db-id category-name)))
    (testing "errors"
      (is (thrown-with-msg? ExceptionInfo
                            #"the category foo does not exist$"
                            (create-crate-category database
                                                   (UUID/randomUUID)
                                                   "foo"))))))
