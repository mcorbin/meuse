(ns meuse.auth.request-test
  (:require [meuse.auth.request :refer :all]
            [meuse.auth.token :refer [generate-token]]
            [meuse.db :refer [database]]
            [meuse.db.actions.token :as public-token]
            [meuse.db.actions.user :as public-user]
            [meuse.db.public.token :refer [token-db]]
            [meuse.helpers.fixtures :refer :all]
            [clojure.test :refer :all])
  (:import clojure.lang.ExceptionInfo))

(use-fixtures :once system-fixture)
(use-fixtures :each db-clean-fixture table-fixture)

(deftest check-user-test
  (testing "success"
    (let [token (public-token/create database {:user "user2"
                                               :validity 10
                                               :name "foo"})
          user (public-user/by-name database "user2")
          request {:headers {"authorization" token}}
          result (check-user token-db request)]
      (is (= result (assoc request :auth {:user-name "user2"
                                          :user-id (:user-id user)
                                          :role-name "tech"}))))
    (let [token (public-token/create database {:user "user1"
                                               :validity 10
                                               :name "foo"})
          user (public-user/by-name database "user1")
          request {:headers {"authorization" token}}
          result (check-user token-db request)]
      (is (= result (assoc request :auth {:user-name "user1"
                                          :user-id (:user-id user)
                                          :role-name "admin"})))))
  (testing "token missing"
    (is (thrown-with-msg?
         ExceptionInfo
         #"token missing in the header"
         (check-user token-db {:headers {}}))))
  (testing "token not found"
    (is (thrown-with-msg?
         ExceptionInfo
         #"token not found"
         (check-user token-db
                     {:headers {"authorization" (generate-token)}}))))
  (testing "user not active"
    (let [token (public-token/create database {:user "user4"
                                               :validity 10
                                               :name "foo"})]
      (is (thrown-with-msg?
           ExceptionInfo
           #"user is not active"
           (check-user token-db
                       {:headers {"authorization" token}})))))
  (testing "invalid token"
    (let [token (public-token/create database {:user "user2"
                                               :validity 10
                                               :name "foobar"})]
      (is (thrown-with-msg?
           ExceptionInfo
           #"invalid token"
           (check-user token-db
                       {:headers {"authorization" (str token "a")}}))))))

(deftest admin?-throw-test
  (is (admin?-throw {:auth {:role-name "admin"}}))
  (is (thrown-with-msg?
       ExceptionInfo
       #"bad permissions"
       (admin?-throw {:auth {:role-name "tech"}})))
  (is (thrown-with-msg?
       ExceptionInfo
       #"bad permissions"
       (admin?-throw {:auth {}}))))

(deftest tech?-throw-test
  (is (tech?-throw {:auth {:role-name "tech"}}))
  (is (thrown-with-msg?
       ExceptionInfo
       #"bad permissions"
       (tech?-throw {:auth {:role-name "admin"}})))
  (is (thrown-with-msg?
       ExceptionInfo
       #"bad permissions"
       (tech?-throw {:auth {}}))))

(deftest admin-or-tech?-throw-test
  (is (admin-or-tech?-throw {:auth {:role-name "tech"}}))
  (is (admin-or-tech?-throw {:auth {:role-name "admin"}}))
  (is (thrown-with-msg?
       ExceptionInfo
       #"bad permissions"
       (admin-or-tech?-throw {:auth {:role-name "foo"}})))
  (is (thrown-with-msg?
       ExceptionInfo
       #"bad permissions"
       (admin-or-tech?-throw {:auth {}}))))
