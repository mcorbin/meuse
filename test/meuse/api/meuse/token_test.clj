(ns meuse.api.meuse.token-test
  (:require [meuse.api.meuse.http :refer :all]
            [meuse.db :refer [database]]
            [meuse.db.token :as token-db]
            [meuse.helpers.fixtures :refer :all]
            [meuse.helpers.request :refer [add-auth]]
            [clojure.test :refer :all])
  (:import clojure.lang.ExceptionInfo))

(use-fixtures :once db-fixture)
(use-fixtures :each table-fixture)

(deftest ^:integration delete-token-test
  (testing "success"
    (token-db/create-token database {:user "user2"
                                     :validity 10
                                     :name "mytoken"})
    (is (= 1 (count (token-db/get-user-tokens database "user2"))))
    (= {:status 200} (meuse-api! (add-auth {:database database
                                            :action :delete-token
                                            :body {:name "mytoken"
                                                   :user "user2"}}
                                           "user2"
                                           "tech")))
    (is (= 0 (count (token-db/get-user-tokens database "user2")))))
  (testing "admin users can delete tokens"
    (token-db/create-token database {:user "user2"
                                     :validity 10
                                     :name "mytoken"})
    (is (= 1 (count (token-db/get-user-tokens database "user2"))))
    (= {:status 200} (meuse-api! (add-auth {:database database
                                            :action :delete-token
                                            :body {:name "mytoken"
                                                   :user "user2"}}
                                           "user1"
                                           "admin")))
    (is (= 0 (count (token-db/get-user-tokens database "user2")))))
  (testing "the token does not exist"
    (is (thrown-with-msg?
         ExceptionInfo
         #"the token doesnotexist does not exist for the user user2"
         (meuse-api! (add-auth {:database database
                                :action :delete-token
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

(deftest ^:integration create-token-test
  (testing "success"
    (let [result (meuse-api! {:database database
                              :action :create-token
                              :body {:user "user2"
                                     :validity 10
                                     :name "mynewtoken"
                                     :password "user2user2"}})
          tokens (token-db/get-user-tokens database "user2")]
      (is (= 200 (:status result)))
      (is (string? (get-in result [:body :token])))
      (is (= 1 (count tokens)))
      (is (meuse.auth.token/valid? (get-in result [:body :token])
                                   (first tokens)))))
  (testing "the user does not exist"
    (is (thrown-with-msg?
         ExceptionInfo
         #"the user foo does not exist"
         (meuse-api! {:database database
                      :action :create-token
                      :body {:name "mytoken"
                             :user "foo"
                             :password "azertyui"
                             :validity 10}}))))
  (testing "invalid password"
    (is (thrown-with-msg?
         ExceptionInfo
         #"invalid password"
         (meuse-api! {:database database
                      :action :create-token
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



