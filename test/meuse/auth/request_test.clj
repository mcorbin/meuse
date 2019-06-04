(ns meuse.auth.request-test
  (:require [meuse.auth.request :refer :all]
            [meuse.auth.token :refer [generate-token]]
            [meuse.db :refer [database]]
            [meuse.db.token :as token-db]
            [meuse.db.user :as user-db]
            [meuse.helpers.fixtures :refer :all]
            [clojure.test :refer :all])
  (:import clojure.lang.ExceptionInfo))

(use-fixtures :once db-fixture)
(use-fixtures :each table-fixture)

(deftest check-user-test
  (testing "success"
    (let [token (token-db/create-token database {:user "user2"
                                                 :validity 10
                                                 :name "foo"})
          user (user-db/get-user-by-name database "user2")
          request {:database database
                   :headers {"authorization" token}}
          result (check-user request)]
      (is (= result (assoc request :auth {:user-name "user2"
                                          :user-id (:user-id user)
                                          :role-name "tech"}))))
    (let [token (token-db/create-token database {:user "user1"
                                                 :validity 10
                                                 :name "foo"})
          user (user-db/get-user-by-name database "user1")
          request {:database database
                   :headers {"authorization" token}}
          result (check-user request)]
      (is (= result (assoc request :auth {:user-name "user1"
                                          :user-id (:user-id user)
                                          :role-name "admin"})))))
  (testing "token missing"
    (is (thrown-with-msg?
         ExceptionInfo
         #"token missing in the header"
         (check-user {:headers {}}))))
  (testing "token not found"
    (is (thrown-with-msg?
         ExceptionInfo
         #"token not found"
         (check-user {:database database
                      :headers {"authorization" (generate-token)}}))))
  (testing "user not active"
    (let [token (token-db/create-token database {:user "user4"
                                                 :validity 10
                                                 :name "foo"})]
          (is (thrown-with-msg?
               ExceptionInfo
               #"user is not active"
               (check-user {:database database
                            :headers {"authorization" token}})))))
  (testing "invalid token"
    (let [token (token-db/create-token database {:user "user2"
                                                 :validity 10
                                                 :name "foo"})]
          (is (thrown-with-msg?
               ExceptionInfo
               #"invalid token"
               (check-user {:database database
                            :headers {"authorization" (str token "a")}}))))))

(deftest admin?-test
  (is (admin? {:auth {:role-name "admin"}}))
  (is (thrown-with-msg?
       ExceptionInfo
       #"bad permissions"
       (admin? {:auth {:role-name "tech"}})))
  (is (thrown-with-msg?
       ExceptionInfo
       #"bad permissions"
       (admin? {:auth {}}))))

(deftest tech?-test
  (is (tech? {:auth {:role-name "tech"}}))
  (is (thrown-with-msg?
       ExceptionInfo
       #"bad permissions"
       (tech? {:auth {:role-name "admin"}})))
  (is (thrown-with-msg?
       ExceptionInfo
       #"bad permissions"
       (tech? {:auth {}}))))

(deftest admin-or-tech?-test
  (is (admin-or-tech? {:auth {:role-name "tech"}}))
  (is (admin-or-tech? {:auth {:role-name "admin"}}))
  (is (thrown-with-msg?
       ExceptionInfo
       #"bad permissions"
       (admin-or-tech? {:auth {:role-name "foo"}})))
  (is (thrown-with-msg?
       ExceptionInfo
       #"bad permissions"
       (admin-or-tech? {:auth {}}))))
