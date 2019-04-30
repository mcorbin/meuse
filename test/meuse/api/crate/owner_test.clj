(ns meuse.api.crate.owner-test
  (:require [meuse.api.crate.owner :refer :all]
            [meuse.api.crate.http :refer [crates-api!]]
            [meuse.db :refer [database]]
            [meuse.db.crate :as crate-db]
            [meuse.db.user :as user-db]
            [meuse.helpers.fixtures :refer :all]
            [cheshire.core :as json]
            [clojure.string :as string]
            [clojure.test :refer :all]))

(use-fixtures :once db-fixture)
(use-fixtures :each table-fixture)

(deftest ^:integration add-owner-test
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
        (user-db/create-user database user))
      (crate-db/create-crate database {:metadata crate})
      (is (= {:status 200
              :body {:ok true
                     :msg (format "added user(s) %s as owner(s) of crate %s"
                                  (string/join ", " (map :name users))
                                  (:name crate))}}
             (crates-api! {:action :add-owner
                           :database database
                           :route-params {:crate-name "foo"}
                           :body (json/generate-string
                                  {:users [(:name (first users))
                                           (:name (second users))]})})))
      (let [crate-db-id (:crate-id (crate-db/get-crate-by-name
                                    database
                                    (:name crate)))
            user1-db-id (:user-id (user-db/get-user-by-name
                                   database
                                   (:name (first users))))
            user2-db-id (:user-id (user-db/get-user-by-name
                                   database
                                   (:name (second users))))
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
               crate-user2))
        (let [crate-users (crates-api! {:action :list-owners
                                        :database database
                                        :route-params {:crate-name (:name crate)}})]
          (is (= (update-in crate-users
                            [:body :users]
                            (fn [u] (map #(dissoc % :id) u)))
                 {:status 200
                  :body {:users [{:login (:name (first users))
                                  :name (:name (first users))}
                                 {:login (:name (second users))
                                  :name (:name (second users))}]}})))))))

(deftest ^:integration remove-owner-test
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
      (crate-db/create-crate database {:metadata crate})
      (doseq [user users]
        (user-db/create-user database user)
        (user-db/create-crate-user database (:name crate) (:name user)))
      (is (= {:status 200
              :body {:ok true
                     :msg (format "removed user(s) %s as owner(s) of crate %s"
                                  (string/join ", " (map :name users))
                                  (:name crate))}}
          (crates-api! {:action :remove-owner
                        :database database
                        :route-params {:crate-name "foo"}
                        :body (json/generate-string
                               {:users [(:name (first users))
                                        (:name (second users))]})})))
      (let [crate-db-id (:crate-id (crate-db/get-crate-by-name
                                    database
                                    (:name crate)))
            user1-db-id (:user-id (user-db/get-user-by-name
                                   database
                                   (:name (first users))))
            user2-db-id (:user-id (user-db/get-user-by-name
                                   database
                                   (:name (second users))))]
        (is (nil? (user-db/get-crate-user
                   database
                   crate-db-id
                   user1-db-id)))
        (is (nil? (user-db/get-crate-user
                   database
                   crate-db-id
                   user2-db-id)))))))
