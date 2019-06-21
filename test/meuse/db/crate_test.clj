(ns meuse.db.crate-test
  (:require [meuse.db :refer [database]]
            [meuse.db.crate :refer :all]
            [meuse.db.category :as category-db]
            [meuse.db.user :as user-db]
            [meuse.helpers.db :refer :all]
            [meuse.helpers.fixtures :refer :all]
            [clojure.test :refer :all])
  (:import clojure.lang.ExceptionInfo
           java.util.UUID))

(use-fixtures :once db-fixture)
(use-fixtures :each table-fixture)

(deftest ^:integration create-test
  (let [crate {:name "test1"
               :vers "0.1.3"
               :yanked false}
        {:keys [user-id]} (user-db/get-user-by-name database "user2")]
    (create database crate user-id)
    (is (thrown-with-msg? ExceptionInfo
                          #"already exists$"
                          (create database crate user-id)))
    (test-crate-version database {:crate-name "test1"
                                  :version-version "0.1.3"
                                  :version-yanked false
                                  :version-description nil})
    (test-crate database {:crate-name "test1"})
    (create database (assoc crate :vers "2.0.0") user-id)
    (test-crate-version database {:crate-name "test1"
                                  :version-version "2.0.0"
                                  :version-yanked false
                                  :version-description nil})
    (test-crate database {:crate-name "test1"})))

(deftest ^:integration create-with-categories
  (let [crate {:name "test-crate-category"
               :vers "0.1.3"
               :categories ["email" "system"]
               :yanked false}
        {:keys [user-id]} (user-db/get-user-by-name database "user2")]
    (create database crate user-id)
    (let [crate-db (by-name database "test-crate-category")
          [c1 c2 :as categories] (->> (category-db/by-crate-id
                                       database
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

(deftest ^:integration create-with-categories-error
  (let [crate {:name "test1"
               :vers "0.1.3"
               :categories ["cat1" "cat2"]
               :yanked false}
        {:keys [user-id]} (user-db/get-user-by-name database "user2")]
    (is (thrown-with-msg? ExceptionInfo
                            #"the category cat1 does not exist"
                            (create database crate user-id)))))
