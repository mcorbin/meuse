(ns meuse.db.actions.crate-test
  (:require [meuse.db :refer [database]]
            [meuse.db.actions.crate :refer :all]
            [meuse.db.actions.category :as category-db]
            [meuse.db.actions.user :as user-db]
            [meuse.helpers.db-state :as db-state]
            [meuse.helpers.fixtures :refer :all]
            [clojure.test :refer :all])
  (:import clojure.lang.ExceptionInfo
           java.util.UUID))

(use-fixtures :once system-fixture)
(use-fixtures :each db-clean-fixture table-fixture)

(deftest create-test
  (let [crate {:name "test1"
               :vers "0.1.3"
               :yanked false}
        {:keys [^UUID user-id]} (user-db/by-name database "user2")]
    (create database crate user-id)
    (is (thrown-with-msg? ExceptionInfo
                          #"already exists$"
                          (create database crate user-id)))
    (db-state/test-crate-version database {:crate-name "test1"
                                           :version-version "0.1.3"
                                           :version-yanked false
                                           :version-description nil})
    (db-state/test-crate database {:crate-name "test1"})
    (create database (assoc crate :vers "2.0.0") user-id)
    (db-state/test-crate-version database {:crate-name "test1"
                                           :version-version "2.0.0"
                                           :version-yanked false
                                           :version-description nil})
    (db-state/test-crate database {:crate-name "test1"})))

(deftest create-with-categories
  (let [crate {:name "test-crate-category"
               :vers "0.1.3"
               :categories ["email" "system"]
               :yanked false}
        {:keys [user-id]} (user-db/by-name database "user2")]
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

(deftest create-with-categories-error
  (let [crate {:name "test1"
               :vers "0.1.3"
               :categories ["cat1" "cat2"]
               :yanked false}
        {:keys [user-id]} (user-db/by-name database "user2")]
    (is (thrown-with-msg? ExceptionInfo
                          #"the category cat1 does not exist"
                          (create database crate user-id)))))

(deftest get-crates-and-versions-test
  (let [result (get-crates-and-versions database)
        crate1 (first (filter #(and (= (:crate-name %) "crate1")
                                    (= (:version-version %) "1.1.0"))
                              result))]
    (is (= 5 (count result)))
    (is (uuid? (:crate-id crate1)))
    (is (= (:crate-name crate1) "crate1"))
    (is (uuid? (:version-id crate1)))
    (is (= (:version-version crate1) "1.1.0"))
    (is (= (:version-description crate1) "the crate1 description, this crate is for foobar"))
    (is (not (:version-yanked crate1)))
    (is (inst? (:version-created-at crate1)))
    (is (inst? (:version-updated-at crate1)))))

(deftest get-crate-and-versions-test
  (testing "success"
    (let [result (get-crate-and-versions database "crate1")
          version1 (first (filter #(= (:version-version %) "1.1.0")
                                  result))]
      (is (= 3 (count result)))
      (is (uuid? (:crate-id version1)))
      (is (= (:crate-name version1) "crate1"))
      (is (uuid? (:version-id version1)))
      (is (= (:version-version version1) "1.1.0"))
      (is (= (:version-description version1) "the crate1 description, this crate is for foobar"))
      (is (not (:version-yanked version1)))
      (is (inst? (:version-created-at version1)))
      (is (inst? (:version-updated-at version1)))))
  (testing "the crate does not exist"
    (is (thrown-with-msg?
         ExceptionInfo
         #"the crate doesnotexist does not exist"
         (get-crate-and-versions database "doesnotexist")))))

(deftest get-crates-for-category-test
  (testing "success"
    (let [result (get-crates-for-category database "email")
          version1 (first (filter #(= (:version-version %) "1.1.0")
                                  result))]
      (is (= 3 (count result)))
      (is (uuid? (:crate-id version1)))
      (is (= (:crate-name version1) "crate1"))
      (is (uuid? (:version-id version1)))
      (is (= (:version-version version1) "1.1.0"))
      (is (= (:version-description version1) "the crate1 description, this crate is for foobar"))
      (is (not (:version-yanked version1)))
      (is (inst? (:version-created-at version1)))
      (is (inst? (:version-updated-at version1)))))
  (testing "the category does not exist"
    (is (thrown-with-msg?
         ExceptionInfo
         #"the category doesnotexist does not exist"
         (get-crates-for-category database "doesnotexist")))))
