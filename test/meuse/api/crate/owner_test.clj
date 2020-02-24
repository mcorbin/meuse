(ns meuse.api.crate.owner-test
  (:require [meuse.api.crate.owner :refer :all]
            [meuse.api.crate.http :refer [crates-api!]]
            [meuse.db :refer [database]]
            [meuse.db.actions.crate :as crate-db]
            [meuse.db.actions.user :as user-db]
            [meuse.db.actions.crate-user :as crate-user-db]
            [meuse.helpers.fixtures :refer :all]
            [meuse.mocks.db :as mocks]
            [cheshire.core :as json]
            [spy.assert :as assert]
            [spy.protocol :as protocol]
            [clojure.string :as string]
            [clojure.test :refer :all])
  (:import clojure.lang.ExceptionInfo
           java.util.UUID))

(use-fixtures :once system-fixture)
(use-fixtures :each db-clean-fixture table-fixture)

(deftest add-owner-test
  (let [user5-id (:users/id (user-db/by-name database "user5"))
        user2-id (:users/id (user-db/by-name database "user2"))]
    (testing "success: admin"
      (is (= {:status 200
              :body {:ok true
                     :msg (format "added user(s) %s as owner(s) of crate %s"
                                  (string/join ", " ["user2" "user3"])
                                  "crate2")}}
             (crates-api! {:action :add-owner
                           :route-params {:crate-name "crate2"}
                           :auth {:user-id user5-id
                                  :role-name "admin"}
                           :body (json/generate-string
                                  {:users ["user2"
                                           "user3"]})})))
      (let [crate-db-id (:crates/id (crate-db/by-name
                                     database
                                     "crate2"))
            user1-db-id (:users/id (user-db/by-name
                                    database
                                    "user2"))
            user2-db-id (:users/id (user-db/by-name
                                    database
                                    "user3"))
            crate-user1 (crate-user-db/by-id
                         database
                         crate-db-id
                         user1-db-id)
            crate-user2 (crate-user-db/by-id
                         database
                         crate-db-id
                         user2-db-id)]
        (is (= {:crates_users/crate_id crate-db-id
                :crates_users/user_id user1-db-id}
               crate-user1))
        (is (= {:crates_users/crate_id crate-db-id
                :crates_users/user_id user2-db-id}
               crate-user2))))
    (testing "success: owner of the crate"
      (is (= {:status 200
              :body {:ok true
                     :msg (format "added user(s) %s as owner(s) of crate %s"
                                  (string/join ", " ["user4"])
                                  "crate2")}}
             (crates-api! {:action :add-owner
                           :route-params {:crate-name "crate2"}
                           :auth {:user-id user2-id
                                  :role-name "tech"}
                           :body (json/generate-string
                                  {:users ["user4"]})})))
      (let [crate-db-id (:crates/id (crate-db/by-name
                                     database
                                     "crate2"))
            user4-db-id (:users/id (user-db/by-name
                                    database
                                    "user4"))
            crate-user4 (crate-user-db/by-id
                         database
                         crate-db-id
                         user4-db-id)]
        (is (= {:crates_users/crate_id crate-db-id
                :crates_users/user_id user4-db-id}
               crate-user4))))
    (testing "user does not own the crate"
      (is (thrown-with-msg?
           ExceptionInfo
           #"user does not own the crate crate3"
           (crates-api! {:action :add-owner
                         :route-params {:crate-name "crate3"}
                         :auth {:user-id user2-id
                                :role-name "tech"}
                         :body (json/generate-string
                                {:users ["user4"]})}))))
    (testing "invalid parameters"
      (is (thrown-with-msg?
           ExceptionInfo
           #"Wrong input parameters:\n - field users is incorrect\n"
           (crates-api! {:action :add-owner
                         :route-params {:crate-name "crate2"}
                         :body (json/generate-string
                                {:users []})}))))
    (testing "invalid parameters"
      (is (thrown-with-msg?
           ExceptionInfo
           #"Wrong input parameters:\n - field crate-name missing in route-params\n"
           (crates-api! {:action :add-owner
                         :route-params {}
                         :body (json/generate-string
                                {:users ["foo"]})}))))))

(deftest remove-owner-test
  (let [user5-id (:users/id (user-db/by-name database "user5"))
        user1-id (:users/id (user-db/by-name database "user1"))]
    (testing "success: admin"
      (is (= {:status 200
              :body {:ok true
                     :msg (format "removed user(s) %s as owner(s) of crate %s"
                                  (string/join ", " ["user2" "user3"])
                                  "crate1")}}
             (crates-api! {:action :remove-owner
                           :auth {:user-id user5-id
                                  :role-name "admin"}
                           :route-params {:crate-name "crate1"}
                           :body (json/generate-string
                                  {:users ["user2"
                                           "user3"]})})))
      (let [crate-db-id (:crates/id (crate-db/by-name
                                     database
                                     "crate1"))
            user1-db-id (:users/id (user-db/by-name
                                    database
                                    "user2"))
            user2-db-id (:users/id (user-db/by-name
                                    database
                                    "user3"))]
        (is (nil? (crate-user-db/by-id
                   database
                   crate-db-id
                   user1-db-id)))
        (is (nil? (crate-user-db/by-id
                   database
                   crate-db-id
                   user2-db-id)))))
    (testing "success: user owns the crate"
      (is (= {:status 200
              :body {:ok true
                     :msg (format "removed user(s) %s as owner(s) of crate %s"
                                  (string/join ", " ["user1"])
                                  "crate1")}}
             (crates-api! {:action :remove-owner
                           :auth {:user-id user1-id
                                  :role-name "tech"}
                           :route-params {:crate-name "crate1"}
                           :body (json/generate-string
                                  {:users ["user1"]})})))
      (let [crate-db-id (:crates/id (crate-db/by-name
                                     database
                                     "crate1"))]
        (is (nil? (crate-user-db/by-id
                   database
                   crate-db-id
                   user1-id)))))
    (testing "user does not own the crate"
      (is (thrown-with-msg?
           ExceptionInfo
           #"user does not own the crate crate3"
           (crates-api! {:action :remove-owner
                         :route-params {:crate-name "crate3"}
                         :auth {:user-id user5-id
                                :role-name "tech"}
                         :body (json/generate-string
                                {:users ["user4"]})}))))
    (testing "invalid parameters"
      (is (thrown-with-msg?
           ExceptionInfo
           #"Wrong input parameters:\n - field users is incorrect\n"
           (crates-api! {:action :remove-owner
                         :route-params {:crate-name "crate2"}
                         :body (json/generate-string
                                {:users []})}))))
    (testing "invalid parameters"
      (is (thrown-with-msg?
           ExceptionInfo
           #"Wrong input parameters:\n - field crate-name missing in route-params\n"
           (crates-api! {:action :remove-owner
                         :route-params {}
                         :body (json/generate-string
                                {:users ["foo"]})}))))))

(deftest list-owner-test
  (testing "success"
    (let [user5-id (:user-id (user-db/by-name database "user5"))
          crate-users (crates-api! {:action :list-owners
                                    :auth {:user-id user5-id
                                           :role-name "admin"}
                                    :route-params {:crate-name "crate1"}})]
      (is (= (update-in crate-users
                        [:body :users]
                        (fn [u] (set (map #(dissoc % :id) u))))
             {:status 200
              :body {:users (set [{:login "user1"
                                   :name "user1"}
                                  {:login "user2"
                                   :name "user2"}
                                  {:login "user3"
                                   :name "user3"}])}}))))
  (testing "success: read-only user"
    (let [user-mock (mocks/user-mock {:crate-owners [{:users/name "user1"
                                                      :users/cargo_id 1}
                                                     {:users/name "user2"
                                                      :users/cargo_id 2}]})]
      (is (= {:status 200
              :body {:users [{:login "user1"
                              :name "user1"
                              :id 1}
                             {:login "user2"
                              :name "user2"
                              :id 2}]}}
             (list-owners user-mock
                          {:action :list-owners
                           :auth {:user-id (UUID/randomUUID)
                                  :role-name "read-only"}
                           :route-params {:crate-name "crate1"}})))
      (assert/called-once-with? (:crate-owners (protocol/spies user-mock))
                                user-mock
                                "crate1")))
  (testing "invalid parameters"
    (is (thrown-with-msg?
         ExceptionInfo
         #"Wrong input parameters:\n - field crate-name missing in route-params\n"
         (crates-api! {:action :list-owners
                       :route-params {}})))))
