(ns meuse.db.actions.category-test
  (:require [meuse.db :refer [database]]
            [meuse.db.actions.category :refer :all]
            [meuse.helpers.fixtures :refer :all]
            [clojure.test :refer :all])
  (:import clojure.lang.ExceptionInfo))

(use-fixtures :once system-fixture)
(use-fixtures :each db-clean-fixture)

(deftest test-create-and-get-categories
  (create database "cat1" "category description")
  (is (thrown-with-msg? ExceptionInfo
                        #"already exists$"
                        (create database "cat1" "category description")))
  (let [category (by-name database "cat1")]
    (is (uuid? (:categories/id category)))
    (is (= "cat1" (:categories/name category)))
    (is (= "category description" (:categories/description category))))
  (create database "cat2" "another category")
  (let [category (by-name database "cat2")]
    (is (uuid? (:categories/id category)))
    (is (= "cat2" (:categories/name category)))
    (is (= "another category" (:categories/description category)))))

(deftest get-categories-test
  (create database "email" "the email category")
  (create database "system" "the system category")
  (let [result (get-categories database)
        email (-> (filter #(= "email" (:categories/name %)) result)
                  first)
        system (-> (filter #(= "system" (:categories/name %)) result)
                   first)]
    (is (= 2 (count result)))
    (is (uuid? (:categories/id system)))
    (is (= (dissoc system :categories/id)
           {:categories/name "system"
            :categories/description "the system category"}))
    (is (uuid? (:categories/id email)))
    (is (= (dissoc email :categories/id)
           {:categories/name "email"
            :categories/description "the email category"}))))

(deftest update-category-test
  (testing "success"
    (create database "email" "the email category")
    (update-category database "email" {:name "music"
                                       :description "music description"})
    (let [categories (get-categories database)
          music (-> (filter #(= "music" (:categories/name %)) categories)
                    first)]
      (is (= "music description" (:categories/description music)))
      (is (nil? (-> (filter #(= "email" (:categories/name %)) categories)
                    first)))))
  (testing "the category does not exist"
    (is (thrown-with-msg?
         ExceptionInfo
         #"the category foo does not exist"
         (update-category database "foo" {:name "music"
                                          :description "music description"})))))

(deftest count-crates-for-categories-test
  (let [categories (get-categories database)
        count-crates (count-crates-for-categories database)]
    (is (= (count categories) (count count-crates)))
    (doseq [count-crate count-crates]
      (is (string? (:categories/id count-crate)))
      (is (= 1 (:categories/count count-crate))))))
