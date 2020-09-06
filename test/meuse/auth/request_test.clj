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
          result (check-user token-db request)
          db-token (->> (public-token/by-user database "user2")
                        (filter #(= (:tokens/name %) "foo"))
                        (first))]
      (is (= result (assoc request :auth {:user-name "user2"
                                          :user-id (:users/id user)
                                          :role-name "tech"})))
      (is (inst? (:tokens/last_used_at db-token))))
    (let [token (public-token/create database {:user "user1"
                                               :validity 10
                                               :name "foo"})
          user (public-user/by-name database "user1")
          request {:headers {"authorization" token}}
          result (check-user token-db request)]
      (is (= result (assoc request :auth {:user-name "user1"
                                          :user-id (:users/id user)
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

(deftest check-admin-test
  (is (check-admin {:auth {:role-name "admin"}}))
  (is (thrown-with-msg?
       ExceptionInfo
       #"bad permissions"
       (check-admin {:auth {:role-name "tech"}})))
  (is (thrown-with-msg?
       ExceptionInfo
       #"bad permissions"
       (check-admin {:auth {}}))))

(deftest check-tech-test
  (is (check-tech {:auth {:role-name "tech"}}))
  (is (thrown-with-msg?
       ExceptionInfo
       #"bad permissions"
       (check-tech {:auth {:role-name "admin"}})))
  (is (thrown-with-msg?
       ExceptionInfo
       #"bad permissions"
       (check-tech {:auth {}}))))

(deftest check-admin-tech-test
  (is (check-admin-tech {:auth {:role-name "tech"}}))
  (is (check-admin-tech {:auth {:role-name "admin"}}))
  (is (thrown-with-msg?
       ExceptionInfo
       #"bad permissions"
       (check-admin-tech {:auth {:role-name "foo"}})))
  (is (thrown-with-msg?
       ExceptionInfo
       #"bad permissions"
       (check-admin-tech {:auth {}}))))

(deftest read-only?-test
  (is (read-only? {:auth {:role-name "read-only"}}))
  (is (not (read-only? {:auth {:role-name "tech"}})))
  (is (not (read-only? {:auth {:role-name "admin"}})))
  (is (not (read-only? {:auth {:role-name "foo"}})))
  (is (not (read-only? {:auth {}}))))

(deftest check-authenticated-test
  (is (check-authenticated {:auth {:role-name "read-only"}}))
  (is (check-authenticated {:auth {:role-name "tech"}}))
  (is (check-authenticated {:auth {:role-name "admin"}}))
  (is (thrown-with-msg?
       ExceptionInfo
       #"bad permissions"
       (check-authenticated {:auth {:role-name "foo"}})))
  (is (thrown-with-msg?
       ExceptionInfo
       #"bad permissions"
       (check-authenticated {:auth {}}))))
