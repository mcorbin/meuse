(ns meuse.api.crate.owner-test
  (:require [meuse.api.crate.owner :refer :all]
            [meuse.api.crate.http :refer [crates-api!]]
            [meuse.db :refer [database]]
            [meuse.db.crate :as crate-db]
            [meuse.db.user :as user-db]
            [meuse.db.crate-user :as crate-user-db]
            [meuse.helpers.fixtures :refer :all]
            [cheshire.core :as json]
            [clojure.string :as string]
            [clojure.test :refer :all])
  (:import clojure.lang.ExceptionInfo))

(use-fixtures :once db-fixture)
(use-fixtures :each table-fixture)

(deftest ^:integration add-owner-test
  (let [user5-id (:user-id (user-db/get-user-by-name database "user5"))
        user2-id (:user-id (user-db/get-user-by-name database "user2"))]
    (testing "success: admin"
      (is (= {:status 200
              :body {:ok true
                     :msg (format "added user(s) %s as owner(s) of crate %s"
                                  (string/join ", " ["user2" "user3"])
                                  "crate2")}}
             (crates-api! {:action :add-owner
                           :database database
                           :route-params {:crate-name "crate2"}
                           :auth {:user-id user5-id
                                  :role-name "admin"}
                           :body (json/generate-string
                                  {:users ["user2"
                                           "user3"]})})))
      (let [crate-db-id (:crate-id (crate-db/by-name
                                    database
                                    "crate2"))
            user1-db-id (:user-id (user-db/get-user-by-name
                                   database
                                   "user2"))
            user2-db-id (:user-id (user-db/get-user-by-name
                                   database
                                   "user3"))
            crate-user1 (crate-user-db/get-crate-user
                         database
                         crate-db-id
                         user1-db-id)
            crate-user2 (crate-user-db/get-crate-user
                         database
                         crate-db-id
                         user2-db-id)]
        (is (= {:crate-id crate-db-id
                :user-id user1-db-id}
               crate-user1))
        (is (= {:crate-id crate-db-id
                :user-id user2-db-id}
               crate-user2))))
    (testing "success: owner of the crate"
      (is (= {:status 200
              :body {:ok true
                     :msg (format "added user(s) %s as owner(s) of crate %s"
                                  (string/join ", " ["user4"])
                                  "crate2")}}
             (crates-api! {:action :add-owner
                           :database database
                           :route-params {:crate-name "crate2"}
                           :auth {:user-id user2-id
                                  :role-name "tech"}
                           :body (json/generate-string
                                  {:users ["user4"]})})))
      (let [crate-db-id (:crate-id (crate-db/by-name
                                    database
                                    "crate2"))
            user4-db-id (:user-id (user-db/get-user-by-name
                                   database
                                   "user4"))
            crate-user4 (crate-user-db/get-crate-user
                         database
                         crate-db-id
                         user4-db-id)]
        (is (= {:crate-id crate-db-id
                :user-id user4-db-id}
               crate-user4))))
    (testing "user does not own the crate"
      (is (thrown-with-msg?
           ExceptionInfo
           #"user does not own the crate crate3"
           (crates-api! {:action :add-owner
                         :database database
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
                         :database database
                         :route-params {:crate-name "crate2"}
                         :body (json/generate-string
                                {:users []})}))))
    (testing "invalid parameters"
      (is (thrown-with-msg?
           ExceptionInfo
           #"Wrong input parameters:\n - field crate-name missing in route-params\n"
           (crates-api! {:action :add-owner
                         :database database
                         :route-params {}
                         :body (json/generate-string
                                {:users ["foo"]})}))))))

(deftest ^:integration remove-owner-test
  (let [user5-id (:user-id (user-db/get-user-by-name database "user5"))
        user1-id (:user-id (user-db/get-user-by-name database "user1"))]
    (testing "success: admin"
      (is (= {:status 200
              :body {:ok true
                     :msg (format "removed user(s) %s as owner(s) of crate %s"
                                  (string/join ", " ["user2" "user3"])
                                  "crate1")}}
             (crates-api! {:action :remove-owner
                           :database database
                           :auth {:user-id user5-id
                                  :role-name "admin"}
                           :route-params {:crate-name "crate1"}
                           :body (json/generate-string
                                  {:users ["user2"
                                           "user3"]})})))
      (let [crate-db-id (:crate-id (crate-db/by-name
                                    database
                                    "crate1"))
            user1-db-id (:user-id (user-db/get-user-by-name
                                   database
                                   "user2"))
            user2-db-id (:user-id (user-db/get-user-by-name
                                   database
                                   "user3"))]
        (is (nil? (crate-user-db/get-crate-user
                   database
                   crate-db-id
                   user1-db-id)))
        (is (nil? (crate-user-db/get-crate-user
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
                           :database database
                           :auth {:user-id user1-id
                                  :role-name "tech"}
                           :route-params {:crate-name "crate1"}
                           :body (json/generate-string
                                  {:users ["user1"]})})))
      (let [crate-db-id (:crate-id (crate-db/by-name
                                    database
                                    "crate1"))]
        (is (nil? (crate-user-db/get-crate-user
                   database
                   crate-db-id
                   user1-id)))))
    (testing "user does not own the crate"
      (is (thrown-with-msg?
           ExceptionInfo
           #"user does not own the crate crate3"
           (crates-api! {:action :remove-owner
                         :database database
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
                         :database database
                         :route-params {:crate-name "crate2"}
                         :body (json/generate-string
                                {:users []})}))))
    (testing "invalid parameters"
      (is (thrown-with-msg?
           ExceptionInfo
           #"Wrong input parameters:\n - field crate-name missing in route-params\n"
           (crates-api! {:action :remove-owner
                         :database database
                         :route-params {}
                         :body (json/generate-string
                                {:users ["foo"]})}))))))

(deftest list-owner-test
  (testing "success"
    (let [crate-users (crates-api! {:action :list-owners
                                    :database database
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
  (testing "invalid parameters"
    (is (thrown-with-msg?
         ExceptionInfo
         #"Wrong input parameters:\n - field crate-name missing in route-params\n"
         (crates-api! {:action :list-owners
                       :database database
                       :route-params {}})))))
