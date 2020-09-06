(ns meuse.api.meuse.token-test
  (:require [meuse.api.meuse.http :refer :all]
            [meuse.db :refer [database]]
            [meuse.db.actions.token :as token-db]
            [meuse.helpers.fixtures :refer :all]
            [meuse.helpers.request :refer [add-auth]]
            [clojure.test :refer :all])
  (:import clojure.lang.ExceptionInfo))

(use-fixtures :once system-fixture)
(use-fixtures :each db-clean-fixture table-fixture)

(deftest delete-token-test
  (testing "success"
    (token-db/create database {:user "user2"
                               :validity 10
                               :name "mytoken"})
    (is (= 1 (count (token-db/by-user database "user2"))))
    (= {:status 200} (meuse-api! (add-auth {:action :delete-token
                                            :body {:name "mytoken"
                                                   :user "user2"}}
                                           "user2"
                                           "tech")))
    (is (= 0 (count (token-db/by-user database "user2")))))
  (testing "admin users can delete tokens"
    (token-db/create database {:user "user2"
                               :validity 10
                               :name "mytoken"})
    (is (= 1 (count (token-db/by-user database "user2"))))
    ;; non admin cannot delete the token
    (is (thrown-with-msg?
         ExceptionInfo
         #"user user3 cannot delete token for user2"
         (meuse-api! (add-auth {:action :delete-token
                                :body {:name "mytoken"
                                       :user "user2"}}
                               "user3"
                               "tech"))))
    (is (= {:status 200
            :body {:ok true}}
           (meuse-api! (add-auth {:action :delete-token
                                  :body {:name "mytoken"
                                         :user "user2"}}
                                 "user1"
                                 "admin"))))
    (is (= 0 (count (token-db/by-user database "user2")))))
  (testing "the token does not exist"
    (is (thrown-with-msg?
         ExceptionInfo
         #"the token doesnotexist does not exist for the user user2"
         (meuse-api! (add-auth {:action :delete-token
                                :body {:name "doesnotexist"
                                       :user "user2"}}
                               "user2"
                               "tech")))))
  (testing "invalid parameters"
    (is (thrown-with-msg?
         ExceptionInfo
         #"Wrong input parameters:\n - field name missing in body\n"
         (meuse-api! (add-auth {:action :delete-token
                                :body {:user "user2"}}
                               "user2"
                               "tech"))))
    (is (thrown-with-msg?
         ExceptionInfo
         #"Wrong input parameters:\n - field name: the value should be a non empty string\n"
         (meuse-api! (add-auth {:action :delete-token
                                :body {:name ""
                                       :user "user2"}}
                               "user2"
                               "tech"))))
    (is (thrown-with-msg?
         ExceptionInfo
         #"Wrong input parameters:\n - field name missing in body\n - field user missing in body\n"
         (meuse-api! (add-auth {:action :delete-token
                                :body {}}
                               "user2"
                               "tech"))))))

(deftest create-token-test
  (testing "success"
    (let [result (meuse-api! {:action :create-token
                              :body {:user "user2"
                                     :validity 10
                                     :name "mynewtoken"
                                     :password "user2user2"}})
          tokens (token-db/by-user database "user2")]
      (is (= 200 (:status result)))
      (is (string? (get-in result [:body :token])))
      (is (= 1 (count tokens)))
      (is (meuse.auth.token/valid? (get-in result [:body :token])
                                   (first tokens)))))
  (testing "the user does not exist"
    (is (thrown-with-msg?
         ExceptionInfo
         #"the user foo does not exist"
         (meuse-api! {:action :create-token
                      :body {:name "mytoken"
                             :user "foo"
                             :password "azertyui"
                             :validity 10}}))))
  (testing "user is not active"
    (is (thrown-with-msg?
         ExceptionInfo
         #"user is not active"
         (meuse-api! {:action :create-token
                      :body {:name "mytoken"
                             :user "user4"
                             :password "user4user4"
                             :validity 10}}))))
  (testing "invalid password"
    (is (thrown-with-msg?
         ExceptionInfo
         #"invalid password"
         (meuse-api! {:action :create-token
                      :body {:name "mytoken"
                             :user "user2"
                             :password "azertyui"
                             :validity 10}}))))
  (testing "invalid parameters"
    (is (thrown-with-msg?
         ExceptionInfo
         #"Wrong input parameters:\n - field name: the value should be a non empty string\n"
         (meuse-api! {:action :create-token
                      :name ""
                      :body {:name ""
                             :user "foo"
                             :password "azertyui"
                             :validity 10}})))
    (is (thrown-with-msg?
         ExceptionInfo
         #"Wrong input parameters:\n - field user missing in body\n"
         (meuse-api! {:action :create-token
                      :body {:name "mytoken"
                             :password "azertyui"
                             :validity 10}})))
    (is (thrown-with-msg?
         ExceptionInfo
         #"Wrong input parameters:\n - field validity: the value should be a positive integer\n"
         (meuse-api! {:action :create-token
                      :body {:name "mytoken"
                             :user "foo"
                             :password "azertyui"
                             :validity "foo"}})))))

(deftest list-tokens-test
  (let [token1 (token-db/create database {:user "user2"
                                          :validity 10
                                          :name "token1"})
        token2 (token-db/create database {:user "user2"
                                          :validity 10
                                          :name "token2"})
        token3 (token-db/create database {:user "user1"
                                          :validity 10
                                          :name "token3"})
        tokens (get-in (meuse-api! (add-auth {:action :list-tokens}
                                             "user2"
                                             "tech"))
                       [:body :tokens])
        tokens-admin (get-in (meuse-api! (add-auth {:action :list-tokens
                                                    :params {:user "user2"}}
                                                   "user1"
                                                   "admin"))
                             [:body :tokens])]
    (testing "an user can retrieve its tokens"
      (is (= 2 (count tokens)))
      (let [db-token-1 (first (filter #(= (:name %) "token1") tokens))
            db-token-2 (first (filter #(= (:name %) "token2") tokens))]
        (is (uuid? (:id db-token-1)))
        (is (inst? (:created-at db-token-1)))
        (is (nil? (:last-used-at db-token-1)))
        (is (inst? (:expired-at db-token-1)))
        (is (= 5 (count (keys db-token-1))))
        (is (uuid? (:id db-token-2)))
        (is (inst? (:created-at db-token-2)))
        (is (nil? (:last-used-at db-token-2)))
        (is (inst? (:expired-at db-token-2)))
        (is (= 5 (count (keys db-token-2))))))
    (testing "admin user can retrieve tokens for another user"
      (is (= 2 (count tokens)))
      (let [db-token-1 (first (filter #(= (:name %) "token1") tokens-admin))
            db-token-2 (first (filter #(= (:name %) "token2") tokens-admin))]
        (is (uuid? (:id db-token-1)))
        (is (inst? (:created-at db-token-1)))
        (is (inst? (:expired-at db-token-1)))
        (is (= 5 (count (keys db-token-1))))
        (is (uuid? (:id db-token-2)))
        (is (inst? (:created-at db-token-2)))
        (is (inst? (:expired-at db-token-2)))
        (is (= 5 (count (keys db-token-2))))))
    (testing "last-used-at is returned"
      (let [db-token-1 (first (filter #(= (:name %) "token1") tokens))]
        (token-db/set-last-used database (:id db-token-1))
        (let [tokens (get-in (meuse-api! (add-auth {:action :list-tokens}
                                                   "user2"
                                                   "tech"))
                             [:body :tokens])]
          (is (inst? (:last-used-at (first (filter #(= (:name %) "token1") tokens))))))))
    (testing "non-admin cannot retrieve the tokens for another user"
      (is (thrown-with-msg?
           ExceptionInfo
           #"bad permissions"
           (meuse-api! (add-auth {:action :list-tokens
                                  :params {:user "user1"}}
                                 "user2"
                                 "tech")))))
    (testing "bad parameters"
      (is (thrown-with-msg?
           ExceptionInfo
           #"Wrong input parameters:\n - field user: the value should be a non empty string\n"
           (meuse-api! (add-auth {:action :list-tokens
                                  :params {:user 1}}
                                 "user2"
                                 "tech")))))))



