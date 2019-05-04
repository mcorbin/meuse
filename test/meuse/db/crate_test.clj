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
  (let [crate {:name "test1"
               :vers "0.1.3"
               :yanked false}]
    (create-crate database crate)
    (is (thrown-with-msg? ExceptionInfo
                          #"already exists$"
                          (create-crate database crate)))
    (test-crate-version database {:crate-name "test1"
                                  :version-version "0.1.3"
                                  :version-yanked false
                                  :version-description nil})
    (test-crate database {:crate-name "test1"})
    (create-crate database (assoc crate :vers "2.0.0"))
    (test-crate-version database {:crate-name "test1"
                                  :version-version "2.0.0"
                                  :version-yanked false
                                  :version-description nil})
    (test-crate database {:crate-name "test1"})))

(deftest ^:integration create-crate-with-categories
  (let [crate {:name "test-crate-category"
               :vers "0.1.3"
               :categories ["email" "system"]
               :yanked false}]
    (create-crate database crate)
    (let [crate-db (get-crate-by-name database "test-crate-category")
          [c1 c2 :as categories] (->> (get-crate-categories database
                                                            (:crate-id crate-db))
                                      (sort-by :category-name))]
      (is (= 2 (count categories)))
      (is (uuid? (:category-id c1)))
      (is (= {:category-name "email"
              :category-description "the email category"}
             (dissoc c1 :category-id)))
      (is (uuid? (:category-id c2)))
      (is (= {:category-name "system"
              :category-description "the system category"}
             (dissoc c2 :category-id))))))

(deftest ^:integration create-crate-with-categories-error
  (let [crate {:name "test1"
               :vers "0.1.3"
               :categories ["cat1" "cat2"]
               :yanked false}]
    (is (thrown-with-msg? ExceptionInfo
                            #"the category cat1 does not exist"
                            (create-crate database crate)))))

(deftest ^:integration update-yank-test
  (testing "success"
    (let [crate {:name "test1"
                 :vers "0.1.3"
                 :yanked false}]
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
    (let [crate {:name "test3"
                 :vers "0.1.3"
                 :yanked false}]
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
        category-name "cat1"
        category-description "another category"]
    (testing "success"
      (category-db/create-category database
                       category-name
                       category-description)
      (create-crate database crate)
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
