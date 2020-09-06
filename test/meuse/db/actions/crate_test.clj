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

(deftest by-name-and-version-test
  (is (map? (by-name-and-version database "crate1" "1.1.0")))
  (is (nil? (by-name-and-version database "crate1" "1.2.0"))))

(deftest create-test
  (let [crate {:name "test1"
               :vers "0.1.3"
               :yanked false}
        user-id (:users/id (user-db/by-name database "user2"))]
    (create database crate user-id)
    (is (thrown-with-msg? ExceptionInfo
                          #"already exists$"
                          (create database crate user-id)))
    (db-state/test-crate-version database {:crates/name "test1"
                                           :crates_versions/version "0.1.3"
                                           :crates_versions/yanked false
                                           :crates_versions/description nil})
    (db-state/test-crate database {:crates/name "test1"})
    (create database (assoc crate :vers "2.0.0") user-id)
    (db-state/test-crate-version database {:crates/name "test1"
                                           :crates_versions/version "2.0.0"
                                           :crates_versions/yanked false
                                           :crates_versions/description nil})
    (db-state/test-crate database {:crates/name "test1"})))

(deftest create-with-categories
  (let [crate {:name "test-crate-category"
               :vers "0.1.3"
               :categories ["email" "system"]
               :yanked false}
        user-id (:users/id (user-db/by-name database "user2"))]
    (create database crate user-id)
    (let [crate-db (by-name database "test-crate-category")
          [c1 c2 :as categories] (->> (category-db/by-crate-id
                                       database
                                       (:crates/id crate-db))
                                      (sort-by :categories/name))]
      (is (= 2 (count categories)))
      (is (uuid? (:categories/id c1)))
      (is (= {:categories/name "email"
              :categories/description "the email category"}
             (dissoc c1 :categories/id)))
      (is (uuid? (:categories/id c2)))
      (is (= {:categories/name "system"
              :categories/description "the system category"}
             (dissoc c2 :categories/id))))))

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
        crate1 (first (filter #(and (= (:crates/name %) "crate1")
                                    (= (:crates_versions/version %) "1.1.0"))
                              result))]
    (is (= 5 (count result)))
    (is (uuid? (:crates/id crate1)))
    (is (= (:crates/name crate1) "crate1"))
    (is (uuid? (:crates_versions/id crate1)))
    (is (= (:crates_versions/version crate1) "1.1.0"))
    (is (= (:crates_versions/description crate1) "the crate1 description, this crate is for foobar"))
    (is (not (:crates_versions/yanked crate1)))
    (is (inst? (:crates_versions/created_at crate1)))
    (is (inst? (:crates_versions/updated_at crate1)))))

(deftest get-crate-and-versions-test
  (testing "success"
    (let [result (get-crate-and-versions database "crate1")
          version1 (first (filter #(= (:crates_versions/version %) "1.1.0")
                                  result))]
      (is (= 3 (count result)))
      (is (uuid? (:crates/id version1)))
      (is (= (:crates/name version1) "crate1"))
      (is (uuid? (:crates_versions/id version1)))
      (is (= (:crates_versions/version version1) "1.1.0"))
      (is (= (:crates_versions/description version1) "the crate1 description, this crate is for foobar"))
      (is (not (:crates_versions/yanked version1)))
      (is (inst? (:crates_versions/created_at version1)))
      (is (inst? (:crates_versions/updated_at version1)))))
  (testing "the crate does not exist"
    (is (thrown-with-msg?
         ExceptionInfo
         #"the crate doesnotexist does not exist"
         (get-crate-and-versions database "doesnotexist")))))

(deftest get-crates-for-category-test
  (testing "success"
    (let [result (get-crates-for-category database "email")
          version1 (first (filter #(= (:crates_versions/version %) "1.1.0")
                                  result))]
      (is (= 3 (count result)))
      (is (uuid? (:crates/id version1)))
      (is (= (:crates/name version1) "crate1"))
      (is (uuid? (:crates_versions/id version1)))
      (is (= (:crates_versions/version version1) "1.1.0"))
      (is (= (:crates_versions/description version1) "the crate1 description, this crate is for foobar"))
      (is (not (:crates_versions/yanked version1)))
      (is (inst? (:crates_versions/created_at version1)))
      (is (inst? (:crates_versions/updated_at version1)))))
  (testing "the category does not exist"
    (is (thrown-with-msg?
         ExceptionInfo
         #"the category doesnotexist does not exist"
         (get-crates-for-category database "doesnotexist")))))

(deftest get-crates-range-test
  (let [crates (get-crates-range database 0 3 "c")]
    (is (= 3 (count crates)))
    (is (= "crate1" (-> crates first :crates/name)))
    (is (uuid? (-> crates first :crates/id)))
    (is (= 3 (-> crates first :count)))
    (is (= "crate2" (-> crates second :crates/name)))
    (is (uuid? (-> crates second :crates/id)))
    (is (= 1 (-> crates second :count)))
    (is (= "crate3" (-> crates last :crates/name)))
    (is (uuid? (-> crates last :crates/id)))
    (is (= 1 (-> crates last :count))))
  (let [crates (get-crates-range database 0 3 "crate1")]
    (is (= 1 (count crates)))
    (is (= "crate1" (-> crates first :crates/name)))
    (is (uuid? (-> crates first :crates/id)))
    (is (= 3 (-> crates first :count))))
  (let [crates (get-crates-range database 0 1 "c")]
    (is (= 1 (count crates)))
    (is (= "crate1" (-> crates first :crates/name)))
    (is (uuid? (-> crates first :crates/id)))
    (is (= 3 (-> crates first :count))))
  (let [crates (get-crates-range database 1 3 "c")]
    (is (= 2 (count crates)))
    (is (= "crate2" (-> crates first :crates/name)))
    (is (uuid? (-> crates first :crates/id)))
    (is (= 1 (-> crates first :count)))
    (is (= "crate3" (-> crates last :crates/name)))
    (is (uuid? (-> crates last :crates/id)))
    (is (= 1 (-> crates last :count)))))

(deftest count-crates-test
  (is (= {:count 3} (count-crates database))))
