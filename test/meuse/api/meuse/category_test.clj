(ns meuse.api.meuse.category-test
  (:require [meuse.api.meuse.category :refer :all]
            [meuse.api.meuse.http :refer :all]
            [meuse.db.actions.category :as category-db]
            [meuse.db.actions.crate :as crate-db]
            [meuse.db :refer [database]]
            [meuse.helpers.fixtures :refer :all]
            [meuse.helpers.request :refer [add-auth]]
            [clojure.test :refer :all])
  (:import clojure.lang.ExceptionInfo))

(use-fixtures :once db-fixture inject-fixture)
(use-fixtures :each db-clean-fixture table-fixture)

(deftest new-category-test
  (let [request (add-auth {:action :new-category
                           :body {:name "foo"
                                  :description "the description"}}
                          "user1"
                          "admin")]
    (is (= {:status 200
            :body {:ok true}} (meuse-api! request)))
    (let [category (category-db/by-name database "foo")]
      (is (uuid? (:category-id category)))
      (is (= "foo" (:category-name category)))
      (is (= "the description" (:category-description category))))
    (is (thrown-with-msg? ExceptionInfo
                          #"already exists$"
                          (meuse-api! request))))
  (testing "invalid parameters"
    (is (thrown-with-msg?
         ExceptionInfo
         #"Wrong input parameters:\n - field name missing in body\n"
         (meuse-api! {:action :new-category
                      :body {:description "the description"}}))))
  (testing "non-admin"
    (is (thrown-with-msg?
         ExceptionInfo
         #"bad permissions"
         (meuse-api! (add-auth {:action :new-category
                                :body {:name "foo"
                                       :description "the description"}}
                               "user2"
                               "tech"))))))

(deftest get-categories-test
  (testing "success"
    (let [request (add-auth {:action :list-categories}
                            "user1"
                            "tech")
          {:keys [status body]} (meuse-api! request)
          categories (:categories body)
          email (-> (filter #(= "email" (:name %)) categories)
                    first)
          system (-> (filter #(= "system" (:name %)) categories)
                     first)]
      (is (= status 200))
      (is (= 2 (count categories)))
      (is (uuid? (:id system)))
      (is (= (dissoc system :id)
             {:name "system"
              :description "the system category"}))
      (is (uuid? (:id email)))
      (is (= (dissoc email :id)
             {:name "email"
              :description "the email category"}))))
  (testing "bad permissions"
    (is (thrown-with-msg?
         ExceptionInfo
         #"bad permissions"
         (meuse-api! {:action :list-categories
                      :body {:name "foo"
                             :description "the description"}})))))

(deftest update-category-test
  (testing "success"
    (let [request (add-auth {:route-params {:name "email"}
                             :body {:name "music"
                                    :description "music description"}
                             :action :update-category}
                            "user1"
                            "admin")]
      (is {:status 200 :body {:ok true}}) (meuse-api! request)
      (let [categories (category-db/get-categories database)
            music (-> (filter #(= "music" (:category-name %)) categories)
                      first)]
        (is (= "music description" (:category-description music)))
        (is (nil? (-> (filter #(= "email" (:category-name %)) categories)
                      first))))))
  (testing "invalid parameters"
    (is (thrown-with-msg?
         ExceptionInfo
         #"Wrong input parameters:\n - field route-params is missing\n"
         (meuse-api! (add-auth {:action :update-category
                                :body {:description "the description"}}
                               "user1"
                               "admin")))))
  (testing "bad permissions"
    (is (thrown-with-msg?
         ExceptionInfo
         #"bad permissions"
         (meuse-api! {:route-params {:name "email"}
                      :body {:name "music"
                             :description "music description"}
                      :action :update-category})))
    (is (thrown-with-msg?
         ExceptionInfo
         #"bad permissions"
         (meuse-api! (add-auth
                      {:action :update-category}
                      "user1"
                      "tech"))))))

