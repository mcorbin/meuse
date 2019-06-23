(ns meuse.db.user-test
  (:require [meuse.auth.password :as password]
            [meuse.db :refer [database]]
            [meuse.db.user :refer :all]
            [meuse.db.crate :as crate-db]
            [meuse.db.role :as role-db]
            [meuse.db.token :as token-db]
            [meuse.helpers.fixtures :refer :all]
            [clojure.test :refer :all]
            [meuse.db.role :as role])
  (:import clojure.lang.ExceptionInfo))

(use-fixtures :once db-fixture)
(use-fixtures :each table-fixture)

(deftest check-active!-test
  (is (thrown-with-msg? ExceptionInfo
                        #"the user foo is inactive"
                        (check-active! {:name "foo" :active false})))
  (is (check-active! {:name "foo" :active true})))

(deftest ^:integration create-get-test
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

(deftest ^:integration crate-owners-test
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
