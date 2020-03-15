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
      (is (uuid? (:users/id user-db)))
      (is (= (:name user) (:users/name user-db)))
      (is (password/check (:password user) (:users/password user-db)))
      (is (= (:description user) (:users/description user-db)))
      (is (= (:active user) (:users/active user-db)))
      (is (= (:roles/id admin-role) (:users/role_id user-db))))
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
        (is (= (:roles/id role-admin) (:users/role_id result)))
        (is (not (:users/active role-admin)))
        (is (= (:users/description result) "new"))
        (is (thrown-with-msg? ExceptionInfo
                              #"invalid password"
                              (password/check (:password user)
                                              (:users/password result))))
        (is (password/check "new password" (:users/password result)))
        ;; name not updated
        (is (= (:name user) (:users/name result))))))
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
    (is (= {:users/name "user1"
            :roles/name "admin"
            :users/description "desc1"
            :users/active true
            :users/id (:users/id user1)}
           (-> (filter #(= (:users/name %) "user1") users)
               first)))))

(deftest by-id-test
  (let [user1 (by-name database "user1")]
    (is (= user1
           (by-id database (str (:users/id user1)))))))


(deftest count-users-test
  (is (= {:count 5} (count-users database))))
