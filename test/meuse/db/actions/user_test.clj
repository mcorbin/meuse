(ns meuse.db.actions.user-test
  (:require [meuse.auth.password :as password]
            [meuse.db :refer [database]]
            [meuse.db.actions.user :refer :all]
            [meuse.db.actions.crate :as crate-db]
            [meuse.db.actions.role :as role-db]
            [meuse.db.actions.token :as token-db]
            [meuse.helpers.fixtures :refer :all]
            [clojure.test :refer :all]
            [meuse.db.actions.role :as role])
  (:import clojure.lang.ExceptionInfo))

(use-fixtures :once system-fixture)
(use-fixtures :each db-clean-fixture table-fixture)

(deftest check-active!-test
  (is (thrown-with-msg? ExceptionInfo
                        #"the user foo is inactive"
                        (check-active! {:name "foo" :active false})))
  (is (check-active! {:name "foo" :active true})))

(deftest create-get-test
  (let [user {:name "mathieu"
              :password "foobar"
              :description "it's me mathieu"
              :active true
              :role "admin"}]
    (create database user)
    (let [user-db (by-name database "mathieu")
          admin-role (role/get-admin-role database)]
      (is (uuid? (:user-id user-db)))
      (is (= (:name user) (:user-name user-db)))
      (is (password/check (:password user) (:user-password user-db)))
      (is (= (:description user) (:user-description user-db)))
      (is (= (:active user) (:user-active user-db)))
      (is (= (:role-id admin-role) (:user-role-id user-db))))
    (is (thrown-with-msg? ExceptionInfo
                          #"already exists$"
                          (create database user))))
  (is (thrown-with-msg? ExceptionInfo
                        #"does not exist$"
                        (create database {:role "foobar"}))))

(deftest crate-owners-test
  (is (thrown-with-msg? ExceptionInfo
                        #"the crate foobar does not exist"
                        (crate-owners database "foobar"))))

(deftest delete-test
  (token-db/create database {:user "user2"
                             :validity 10
                             :name "foo"})
  (token-db/create database {:user "user2"
                             :validity 20
                             :name "bar"})
  (delete database "user2")
  (is (= nil (by-name database "user2"))))

(deftest update-user-test
  (testing "success"
    (let [user {:name "mathieu"
                :password "foobar"
                :description "it's me mathieu"
                :active true
                :role "tech"}]
      (create database user)
      (update-user database "mathieu" {:description "new"
                                       :active false
                                       :password "new password"
                                       :name "notupdated"
                                       :role "admin"})
      (let [result (by-name database "mathieu")
            role-admin (role/get-admin-role database)]
        (is (= (:role-id role-admin) (:user-role-id result)))
        (is (not (:user-active role-admin)))
        (is (= (:user-description result) "new"))
        (is (thrown-with-msg? ExceptionInfo
                              #"invalid password"
                              (password/check (:password user) (:user-password result))))
        (is (password/check "new password" (:user-password result)))
        ;; name not updated
        (is (= (:name user) (:user-name result))))))
  (testing "the user does not exist"
    (is (thrown-with-msg? ExceptionInfo
                          #"the user doesnotexist does not exist"
                          (update-user database
                                       "doesnotexist"
                                       {:description "new"}))))
  (testing "the role does not exist"
    (is (thrown-with-msg? ExceptionInfo
                          #"the role doesnotexist does not exist"
                          (update-user database
                                       "mathieu"
                                       {:description "new"
                                        :role "doesnotexist"})))))

(deftest get-users-test
  (let [users (get-users database)
        user1 (by-name database "user1")]
    (is (= 5 (count users)))
    (is (= {:name "user1"
            :role "admin"
            :description "desc1"
            :active true
            :id (:user-id user1)}
           (-> (filter #(= (:name %) "user1") users)
               first)))))
