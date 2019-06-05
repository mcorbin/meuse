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

(deftest ^:integration create-user-get-test
  (let [user {:name "mathieu"
              :password "foobar"
              :description "it's me mathieu"
              :active true
              :role "admin"}]
    (create-user database user)
    (let [user-db (get-user-by-name database "mathieu")
          admin-role (role/get-admin-role database)]
      (is (uuid? (:user-id user-db)))
      (is (= (:name user) (:user-name user-db)))
      (is (password/check (:password user) (:user-password user-db)))
      (is (= (:description user) (:user-description user-db)))
      (is (= (:active user) (:user-active user-db)))
      (is (= (:role-id admin-role) (:user-role-id user-db))))
    (is (thrown-with-msg? ExceptionInfo
                          #"already exists$"
                          (create-user database user))))
  (is (thrown-with-msg? ExceptionInfo
                        #"does not exist$"
                        (create-user database {:role "foobar"}))))

(deftest ^:integration create-crate-user-test
  (let [user {:name "mathieu"
              :password "foobar"
              :description "it's me mathieu"
              :active true
              :role "admin"}]
    (testing "success"
      (create-user database user)
      (create-crate-user database "crate1" (:name user))
      (let [crate-db-id (:crate-id (crate-db/get-crate-by-name
                                    database
                                    "crate1"))
            user-db-id (:user-id (get-user-by-name database (:name user)))
            crate-user (get-crate-user
                        database
                        crate-db-id
                        user-db-id)]
        (is (= {:crate-id crate-db-id
                :user-id user-db-id}
               crate-user))))
    (testing "error"
      (is (thrown-with-msg? ExceptionInfo
                            #"does not exist$"
                            (create-crate-user database "oups" (:name user))))
      (is (thrown-with-msg? ExceptionInfo
                            #"does not exist$"
                            (create-crate-user database "crate1" "oups")))
      (is (thrown-with-msg? ExceptionInfo
                            (re-pattern (format "already owns the crate %s$"
                                                "crate1"))
                            (create-crate-user database "crate1" (:name user)))))))

(deftest ^:integration delete-crate-user-test
  (testing "success"
    (delete-crate-user database "crate1" "user2")
    (let [crate-db-id (:crate-id (crate-db/get-crate-by-name
                                  database
                                  "crate1"))
          user-db-id (:user-id (get-user-by-name database "user2"))]
      (is (nil? (get-crate-user database crate-db-id user-db-id)))))
  (testing "error"
    (is (thrown-with-msg? ExceptionInfo
                          #"the crate oups does not exist$"
                          (delete-crate-user database "oups" "user2")))
    (is (thrown-with-msg? ExceptionInfo
                          #"the user oups does not exist$"
                          (delete-crate-user database "crate1" "oups")))
    (is (thrown-with-msg? ExceptionInfo
                          #"the user user2 does not own the crate crate1"
                          (delete-crate-user database "crate1" "user2")))))

(deftest ^:integration create-crate-users-test
  (testing "success"
    (create-crate-users database
                        "crate2"
                        ["user2" "user3"])
    (let [crate-db-id (:crate-id (crate-db/get-crate-by-name
                                  database
                                  "crate2"))
          ;; user1 is created by the test fixture
          user0-db-id (:user-id (get-user-by-name database "user1"))
          user1-db-id (:user-id (get-user-by-name database "user2"))
          user2-db-id (:user-id (get-user-by-name database "user3"))
          crate-user1 (get-crate-user
                       database
                       crate-db-id
                       user1-db-id)
          crate-user2 (get-crate-user
                       database
                       crate-db-id
                       user2-db-id)]
      (is (= {:crate-id crate-db-id
              :user-id user1-db-id}
             crate-user1))
      (is (= {:crate-id crate-db-id
              :user-id user2-db-id}
             crate-user2))
      (let [crate-users (get-crate-join-crates-users database "crate2")]
        (is (int? (:user-cargo-id (first crate-users))))
        (is (int? (:user-cargo-id (second crate-users))))
        (is (= (set [{:user-id user0-db-id
                      :user-name "user1"
                      :crate-id crate-db-id}
                     {:user-id user1-db-id
                      :user-name "user2"
                      :crate-id crate-db-id}
                     {:user-id user2-db-id
                      :user-name "user3"
                      :crate-id crate-db-id}])
               (set (map #(dissoc % :user-cargo-id) crate-users))))))))

(deftest ^:integration delete-crate-users-test
  (testing "success"
    (delete-crate-users database
                        "crate1"
                        ["user2" "user3"])
    (let [crate-db-id (:crate-id (crate-db/get-crate-by-name
                                  database
                                  "crate1"))
          user1-db-id (:user-id (get-user-by-name database "user2"))
          user2-db-id (:user-id (get-user-by-name database "user3"))]
      (is (nil? (get-crate-user database crate-db-id user1-db-id)))
      (is (nil? (get-crate-user database crate-db-id user2-db-id))))))

(deftest ^:integration get-crate-join-crates-users-error-test
  (is (thrown-with-msg? ExceptionInfo
                        #"the crate foobar does not exist"
                        (get-crate-join-crates-users database "foobar"))))

(deftest delete-user-test
  (token-db/create-token database {:user "user2"
                                   :validity 10
                                   :name "foo"})
  (token-db/create-token database {:user "user2"
                                   :validity 20
                                   :name "bar"})
  (delete-user database "user2")
  (is (= nil (get-user-by-name database "user2"))))
>
