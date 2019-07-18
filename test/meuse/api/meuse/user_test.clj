(ns meuse.api.meuse.user-test
  (:require [meuse.api.meuse.http :refer :all]
            [meuse.api.meuse.user :refer :all]
            [meuse.auth.password :as password]
            [meuse.db :refer [database]]
            [meuse.db.user :as user-db]
            [meuse.db.role :as role-db]
            [meuse.helpers.fixtures :refer :all]
            [meuse.helpers.request :refer [add-auth]]
            [clojure.test :refer :all])
  (:import clojure.lang.ExceptionInfo))

(use-fixtures :once db-fixture)
(use-fixtures :each table-fixture)

(deftest new-user-test
  (let [user {:name "mathieu"
              :password "foobarbaz"
              :active true
              :description "it's me mathieu"
              :role "admin"}
        request (add-auth {:database database
                           :action :new-user
                           :body user}
                          "user1"
                          "admin")]
    (is (= {:status 200
            :body {:ok true}} (meuse-api! request)))
    (let [user-db (user-db/by-name database "mathieu")
          admin-role (role-db/get-admin-role database)]
      (is (uuid? (:user-id user-db)))
      (is (= (:name user) (:user-name user-db)))
      (is (password/check (:password user) (:user-password user-db)))
      (is (= (:description user) (:user-description user-db)))
      (is (= (:active user) (:user-active user-db)))
      (is (= (:role-id admin-role) (:user-role-id user-db))))
    (is (thrown-with-msg? ExceptionInfo
                          #"already exists$"
                          (meuse-api! request))))
  (testing "invalid parameters"
    (is (thrown-with-msg?
         ExceptionInfo
         #"Wrong input parameters:\n - field active missing in body\n - field password: the password should have at least 8 characters\n"
         (meuse-api! (add-auth {:action :new-user
                                :body {:name "mathieu"
                                       :password "foobar"
                                       :description "it's me mathieu"
                                       :role "admin"}}
                               "user1"
                               "admin"))))
    (is (thrown-with-msg?
         ExceptionInfo
         #"Wrong input parameters:\n - field password missing in body\n"
         (meuse-api! (add-auth {:action :new-user
                                :body {:name "mathieu"
                                       :active true
                                       :description "it's me mathieu"
                                       :role "admin"}}
                               "user1"
                               "admin"))))
    (is (thrown-with-msg?
         ExceptionInfo
         #"Wrong input parameters:\n - field password missing in body\n - field role: the role should be 'admin' or 'tech'\n"
         (meuse-api! (add-auth {:action :new-user
                                :body {:name "mathieu"
                                       :active true
                                       :description "it's me mathieu"
                                       :role "lol"}}
                               "user1"
                               "admin")))))
  (testing "bad permissions: not admin"
    (let [user {:name "mathieu"
                :password "foobarbaz"
                :active true
                :description "it's me mathieu"
                :role "admin"}
        request (add-auth {:database database
                           :action :new-user
                           :body user}
                          "user1"
                          "tech")]
      (is (thrown-with-msg?
         ExceptionInfo
         #"bad permissions"
         (meuse-api! request)))))
  (testing "bad permissions: no auth"
    (let [user {:name "mathieu"
              :password "foobarbaz"
              :active true
              :description "it's me mathieu"
              :role "admin"}
          request {:database database
                     :action :new-user
                     :body user}]
      (is (thrown-with-msg?
         ExceptionInfo
         #"bad permissions"
         (meuse-api! request))))))

(deftest delete-user-test
  (let [username "user2"
        request (add-auth {:database database
                           :action :delete-user
                           :route-params {:name username}}
                          "user1"
                          "admin")]
    (is (= {:status 200
            :body {:ok true}} (meuse-api! request)))
    (is (nil? (user-db/by-name database "user2"))))
  (testing "invalid parameters"
    (is (thrown-with-msg?
         ExceptionInfo
         #"Wrong input parameters:\n - field name missing in route-params\n"
         (meuse-api! {:action :delete-user
                      :route-params {}})))
    (is (thrown-with-msg?
         ExceptionInfo
         #"Wrong input parameters:\n - field name: the value should be a non empty string\n"
         (meuse-api! {:action :delete-user
                      :route-params {:name ""}}))))
  (testing "bad permissions"
    (is (thrown-with-msg?
         ExceptionInfo
         #"bad permissions"
         (meuse-api! (add-auth {:database database
                                :action :delete-user
                                :route-params {:name "user2"}}
                               "user1"
                               "tech"))))))

(deftest update-user-test
  (testing "success: update by admin"
    (is (= {:status 200
            :body {:ok true}}
           (meuse-api! (add-auth {:database database
                                  :action :update-user
                                  :route-params {:name "user3"}
                                  :body {:description "foo"
                                         :active false
                                         :role "admin"
                                         :name "new_name" ;; does not work
                                         :password "new_password"}}
                                 "user1"
                                 "admin"))))
    (let [user-db (user-db/by-name database "user3")
          admin-role (role-db/get-admin-role database)]
      (is (password/check "new_password" (:user-password user-db)))
      (is (= "foo" (:user-description user-db)))
      (is (not (:user-active user-db)))
      (is (= (:role-id admin-role) (:user-role-id user-db))))
    (is (nil? (user-db/by-name database "new_name"))))
  (testing "success: update by himself"
    (meuse-api! (add-auth {:database database
                           :action :update-user
                           :route-params {:name "user2"}
                           :body {:description "foobar"
                                  :password "new_password_2"}}
                          "user2"
                          "tech"))
    (let [user-db (user-db/by-name database "user2")
          tech-role (role-db/get-tech-role database)]
      (is (password/check "new_password_2" (:user-password user-db)))
      (is (= "foobar" (:user-description user-db)))
      (is (= (:role-id tech-role) (:user-role-id user-db))))
    (is (nil? (user-db/by-name database "new_name"))))
  (testing "error: not admin and restrictions"
    (is (thrown-with-msg?
         ExceptionInfo
         #"only admins can update an user role"
         (meuse-api! (add-auth {:database database
                                :action :update-user
                                :route-params {:name "user2"}
                                :body {:role "admin"}}
                               "user2"
                               "tech"))
         ))
    (is (thrown-with-msg?
         ExceptionInfo
         #"only admins can enable or disable an user"
         (meuse-api! (add-auth {:database database
                                :action :update-user
                                :route-params {:name "user2"}
                                :body {:active false}}
                               "user2"
                               "tech"))))
    (is (thrown-with-msg?
         ExceptionInfo
         #"bad permissions"
         (meuse-api! (add-auth {:database database
                                :action :update-user
                                :route-params {:name "user2"}
                                :body {:description "foo"}}
                               "user3"
                               "tech")))))
  (testing "invalid parameters"
    (is (thrown-with-msg?
         ExceptionInfo
         #"Wrong input parameters:\n - field name missing in route-params\n"
         (meuse-api! (add-auth {:database database
                                :action :update-user
                                :route-params {}
                                :body {:description "foo"}}
                               "user2"
                               "tech"))))
    (is (thrown-with-msg?
         ExceptionInfo
         #"Wrong input parameters:\n - field active: the value should be a boolean\n"
         (meuse-api! (add-auth {:database database
                                :action :update-user
                                :route-params {:name "user2"}
                                :body {:active "lol"}}
                               "user2"
                               "tech"))))))

(deftest list-users-test
  (testing "list users: success"
    (let [result (meuse-api! (add-auth {:database database
                                        :action :list-users}
                                       "user1"
                                       "admin"))
          user1 (user-db/by-name database "user1")]
      (is (= 200 (:status result)))
      (is (= 5 (count (:body result))))
      (is (= {:name "user1"
              :role "admin"
              :description "desc1"
              :active true
              :id (:user-id user1)}
             (-> (filter #(= (:name %) "user1") (:body result))
                 first)))))
  (testing "list users: not admin"
    (is (thrown-with-msg?
         ExceptionInfo
         #"bad permissions"
         (meuse-api! (add-auth {:database database
                                :action :list-users}
                               "user1"
                               "tech"))))))
