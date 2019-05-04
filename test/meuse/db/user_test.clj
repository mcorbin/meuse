(ns meuse.db.user-test
  (:require [meuse.auth.password :as password]
            [meuse.db :refer [database]]
            [meuse.db.user :refer :all]
            [meuse.db.crate :as crate-db]
            [meuse.db.role :as role-db]
            [meuse.helpers.fixtures :refer :all]
            [clojure.test :refer :all]
            [meuse.db.role :as role])
  (:import clojure.lang.ExceptionInfo))

(use-fixtures :once db-fixture)
(use-fixtures :each table-fixture)

(deftest ^:integration create-user-get-test
  (let [user {:name "mathieu"
              :password "foobar"
              :description "it's me mathieu"
              :role "admin"}]
    (create-user database user)
    (let [user-db (get-user-by-name database "mathieu")
          admin-role (role/get-admin-role database)]
      (is (uuid? (:user-id user-db)))
      (is (= (:name user) (:user-name user-db)))
      (is (password/check (:password user) (:user-password user-db)))
      (is (= (:description user) (:user-description user-db)))
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
              :role "admin"}
        crate {:name "foo"
               :vers "1.0.1"}]
    (testing "success"
      (create-user database user)
      (crate-db/create-crate database crate)
      (create-crate-user database (:name crate) (:name user))
      (let [crate-db-id (:crate-id (crate-db/get-crate-by-name
                                    database
                                    (:name crate)))
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
                            (create-crate-user database (:name crate) "oups")))
      (is (thrown-with-msg? ExceptionInfo
                            (re-pattern (format "already owns the crate %s$"
                                                (:name crate)))
                            (create-crate-user database (:name crate) (:name user)))))))

(deftest ^:integration delete-crate-user-test
  (let [user {:name "mathieu"
              :password "foobar"
              :description "it's me mathieu"
              :role "admin"}
        crate {:name "foo"
               :vers "1.0.1"}]
    (testing "success"
      (create-user database user)
      (crate-db/create-crate database crate)
      (create-crate-user database (:name crate) (:name user))
      (delete-crate-user database (:name crate) (:name user))
      (let [crate-db-id (:crate-id (crate-db/get-crate-by-name
                                    database
                                    (:name crate)))
            user-db-id (:user-id (get-user-by-name database (:name user)))]
        (is (nil? (get-crate-user database crate-db-id user-db-id)))))
    (testing "error"
      (is (thrown-with-msg? ExceptionInfo
                            #"does not exist$"
                            (delete-crate-user database "oups" (:name user))))
      (is (thrown-with-msg? ExceptionInfo
                            #"does not exist$"
                            (delete-crate-user database (:name crate) "oups")))
      (is (thrown-with-msg? ExceptionInfo
                            (re-pattern (format "does not own the crate %s$"
                                                (:name crate)))
                            (delete-crate-user database (:name crate) (:name user)))))))

(deftest ^:integration create-crate-users-test
  (let [users [{:name "mathieu"
                :password "foobar"
                :description "it's me mathieu"
                :role "admin"}
               {:name "lapin"
                :password "toto"
                :description "( )_( )
                              (='.'=)
                              (o)_(o)"
                :role "admin"}]
        crate {:name "foo"
               :vers "1.0.1"}]
    (testing "success"
      (doseq [user users]
        (create-user database user))
      (crate-db/create-crate database crate)
      (create-crate-users database
                          (:name crate)
                          [(:name (first users))
                           (:name (second users))])
      (let [crate-db-id (:crate-id (crate-db/get-crate-by-name
                                    database
                                    (:name crate)))
            user1-db-id (:user-id (get-user-by-name database (:name (first users))))
            user2-db-id (:user-id (get-user-by-name database (:name (second users))))
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
        (let [crate-users (get-crate-users database (:name crate))]
          (is (int? (:user-cargo-id (first crate-users))))
          (is (int? (:user-cargo-id (second crate-users))))
          (is (= (set [{:user-id user1-db-id
                        :user-name (:name (first users))
                        :crate-id crate-db-id}
                       {:user-id user2-db-id
                        :user-name (:name (second users))
                        :crate-id crate-db-id}])
                 (set (map #(dissoc % :user-cargo-id) crate-users)))))))))

(deftest ^:integration delete-crate-users-test
  (let [users [{:name "mathieu"
                :password "foobar"
                :description "it's me mathieu"
                :role "admin"}
               {:name "lapin"
                :password "toto"
                :description "( )_( )
                              (='.'=)
                              (o)_(o)"
                :role "admin"}]
        crate {:name "foo"
               :vers "1.0.1"}]
    (testing "success"
      (crate-db/create-crate database crate)
      (doseq [user users]
        (create-user database user)
        (create-crate-user database (:name crate) (:name user)))
      (delete-crate-users database
                          (:name crate)
                          [(:name (first users))
                           (:name (second users))])
      (let [crate-db-id (:crate-id (crate-db/get-crate-by-name
                                    database
                                    (:name crate)))
            user1-db-id (:user-id (get-user-by-name database (:name (first users))))
            user2-db-id (:user-id (get-user-by-name database (:name (second users))))]
        (is (nil? (get-crate-user database crate-db-id user1-db-id)))
        (is (nil? (get-crate-user database crate-db-id user2-db-id)))))))
