(ns meuse.api.meuse.category-test
  (:require [meuse.api.meuse.category :refer :all]
            [meuse.api.meuse.http :refer :all]
            [meuse.db.actions.category :as category-db]
            [meuse.db.actions.crate :as crate-db]
            [meuse.db :refer [database]]
            [meuse.helpers.fixtures :refer :all]
            [meuse.helpers.request :refer [add-auth]]
            [meuse.mocks.db :as mocks]
            [clojure.test :refer :all])
  (:import clojure.lang.ExceptionInfo))

(use-fixtures :once system-fixture)
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
      (is (uuid? (:categories/id category)))
      (is (= "foo" (:categories/name category)))
      (is (= "the description" (:categories/description category))))
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
  (testing "read-only"
    (let [request (add-auth {:action :list-categories}
                            "user1"
                            "read-only")
          category-mock (mocks/category-mock
                         {:get-categories [{:categories/id #uuid "d22dffa8-5750-11ea-bb97-b34af80344d5"
                                            :categories/name "foo"
                                            :categories/description "bar"}]})]
      (is (= {:status 200
              :body {:categories [{:id #uuid "d22dffa8-5750-11ea-bb97-b34af80344d5"
                                   :name "foo"
                                   :description "bar"}]}}
           (list-categories category-mock request)))))
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
            music (-> (filter #(= "music" (:categories/name %)) categories)
                      first)]
        (is (= "music description" (:categories/description music)))
        (is (nil? (-> (filter #(= "email" (:categories/name %)) categories)
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

