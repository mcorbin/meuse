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
    (is (uuid? (:category-id category)))
    (is (= "cat1" (:category-name category)))
    (is (= "category description" (:category-description category))))
  (create database "cat2" "another category")
  (let [category (by-name database "cat2")]
    (is (uuid? (:category-id category)))
    (is (= "cat2" (:category-name category)))
    (is (= "another category" (:category-description category)))))

(deftest get-categories-test
  (create database "email" "the email category")
  (create database "system" "the system category")
  (let [result (get-categories database)
        email (-> (filter #(= "email" (:category-name %)) result)
                  first)
        system (-> (filter #(= "system" (:category-name %)) result)
                   first)]
    (is (= 2 (count result)))
    (is (uuid? (:category-id system)))
    (is (= (dissoc system :category-id)
           {:category-name "system"
            :category-description "the system category"}))
    (is (uuid? (:category-id email)))
    (is (= (dissoc email :category-id)
           {:category-name "email"
            :category-description "the email category"}))))

(deftest update-category-test
  (testing "success"
    (create database "email" "the email category")
    (update-category database "email" {:name "music"
                                       :description "music description"})
    (let [categories (get-categories database)
          music (-> (filter #(= "music" (:category-name %)) categories)
                    first)]
      (is (= "music description" (:category-description music)))
      (is (nil? (-> (filter #(= "email" (:category-name %)) categories)
                    first)))))
  (testing "the category does not exist"
    (is (thrown-with-msg?
         ExceptionInfo
         #"the category foo does not exist"
         (update-category database "foo" {:name "music"
                                          :description "music description"})))))
