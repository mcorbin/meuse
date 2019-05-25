(ns meuse.api.meuse.token-test
  (:require [meuse.api.meuse.http :refer :all]
            [meuse.db :refer [database]]
            [meuse.db.token :as token-db]
            [meuse.helpers.fixtures :refer :all]
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
    (= {:status 200} (meuse-api! {:database database
                                  :action :delete-token
                                  :body {:name "mytoken"
                                         :user "user2"}}))
    (is (= 0 (count (token-db/get-user-tokens database "user2")))))
  (testing "the token does not exist"
    (is (thrown-with-msg?
         ExceptionInfo
         #"the token doesnotexist does not exist for the user user2"
         (meuse-api! {:database database
                      :action :delete-token
                      :body {:name "doesnotexist"
                             :user "user2"}}))))
  (testing "invalid parameters"
    (is (thrown-with-msg?
         ExceptionInfo
         #"invalid parameters"
         (meuse-api! {:action :delete-token
                      :name ""
                      :body {:user "user2"}})))
    (is (thrown-with-msg?
         ExceptionInfo
         #"invalid parameters"
         (meuse-api! {:action :delete-token
                      :body {:name ""
                             :user "user2"}})))
    (is (thrown-with-msg?
         ExceptionInfo
         #"invalid parameters"
         (meuse-api! {:action :delete-token
                      :body {}})))))

(deftest ^:integration create-token-test
  (testing "success"
    (= {:status 200} (meuse-api! {:database database
                                  :action :create-token
                                  :body {:user "user2"
                                         :validity 10
                                         :name "mynewtoken"}}))
    (is (= 1 (count (token-db/get-user-tokens database "user2")))))
  (testing "the user does not exist"
    (is (thrown-with-msg?
         ExceptionInfo
         #"the user foo does not exist"
         (meuse-api! {:database database
                      :action :create-token
                      :body {:name "mytoken"
                             :user "foo"
                             :validity 10}}))))
  (testing "invalid parameters"
    (is (thrown-with-msg?
         ExceptionInfo
         #"invalid parameters"
         (meuse-api! {:action :create-token
                      :name ""
                      :body {:name ""
                             :user-name "foo"
                             :validity 10}})))
    (is (thrown-with-msg?
         ExceptionInfo
         #"invalid parameters"
         (meuse-api! {:action :create-token
                      :body {:name "mytoken"
                             :validity 10}})))
    (is (thrown-with-msg?
         ExceptionInfo
         #"invalid parameters"
         (meuse-api! {:action :create-token
                      :body {:name "mytoken"
                             :user-name "foo"
                             :validity "foo"}})))))



