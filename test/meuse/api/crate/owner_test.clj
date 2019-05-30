(ns meuse.api.crate.owner-test
  (:require [meuse.api.crate.owner :refer :all]
            [meuse.api.crate.http :refer [crates-api!]]
            [meuse.db :refer [database]]
            [meuse.db.crate :as crate-db]
            [meuse.db.user :as user-db]
            [meuse.helpers.fixtures :refer :all]
            [cheshire.core :as json]
            [clojure.string :as string]
            [clojure.test :refer :all])
  (:import clojure.lang.ExceptionInfo))

(use-fixtures :once db-fixture)
(use-fixtures :each table-fixture)

(deftest ^:integration add-owner-test
  (testing "success"
    (is (= {:status 200
            :body {:ok true
                   :msg (format "added user(s) %s as owner(s) of crate %s"
                                (string/join ", " ["user2" "user3"])
                                "crate2")}}
           (crates-api! {:action :add-owner
                         :database database
                         :route-params {:crate-name "crate2"}
                         :body (json/generate-string
                                {:users ["user2"
                                         "user3"]})})))
    (let [crate-db-id (:crate-id (crate-db/get-crate-by-name
                                  database
                                  "crate2"))
          user1-db-id (:user-id (user-db/get-user-by-name
                                 database
                                 "user2"))
          user2-db-id (:user-id (user-db/get-user-by-name
                                 database
                                 "user3"))
          crate-user1 (user-db/get-crate-user
                       database
                       crate-db-id
                       user1-db-id)
          crate-user2 (user-db/get-crate-user
                       database
                       crate-db-id
                       user2-db-id)]
      (is (= {:crate-id crate-db-id
              :user-id user1-db-id}
             crate-user1))
      (is (= {:crate-id crate-db-id
              :user-id user2-db-id}
             crate-user2))))
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
                              {:users ["foo"]})})))))

(deftest ^:integration remove-owner-test
  (testing "success"
    (is (= {:status 200
            :body {:ok true
                   :msg (format "removed user(s) %s as owner(s) of crate %s"
                                (string/join ", " ["user2" "user3"])
                                "crate1")}}
           (crates-api! {:action :remove-owner
                         :database database
                         :route-params {:crate-name "crate1"}
                         :body (json/generate-string
                                {:users ["user2"
                                         "user3"]})})))
    (let [crate-db-id (:crate-id (crate-db/get-crate-by-name
                                  database
                                  "crate1"))
          user1-db-id (:user-id (user-db/get-user-by-name
                                 database
                                 "user2"))
          user2-db-id (:user-id (user-db/get-user-by-name
                                 database
                                 "user3"))]
      (is (nil? (user-db/get-crate-user
                 database
                 crate-db-id
                 user1-db-id)))
      (is (nil? (user-db/get-crate-user
                 database
                 crate-db-id
                 user2-db-id)))))
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
                              {:users ["foo"]})})))))

(deftest list-owner-test
  (testing "success"
    (let [crate-users (crates-api! {:action :list-owners
                                    :database database
                                    :route-params {:crate-name "crate1"}})]
      (is (= (update-in crate-users
                        [:body :users]
                        (fn [u] (set (map #(dissoc % :id) u))))
             {:status 200
              :body {:users (set [{:login "user2"
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
