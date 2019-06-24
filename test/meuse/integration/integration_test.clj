(ns meuse.integration.integration-test
  (:require [meuse.helpers.fixtures :refer :all]
            [meuse.db :refer [database]]
            [meuse.db.token :as token-db]
            meuse.http
            [cheshire.core :as json]
            [clj-http.client :as client]
            [clojure.test :refer :all]))

(def meuse-url "http://127.0.0.1:8855")

(use-fixtures :once project-fixture)

(defn js
  [payload]
  (json/generate-string payload))

(defn test-http
  [expected actual]
  (is (= expected
         (select-keys actual (keys expected)))))

(deftest integration-test
  ;; create a token for an admin user
  (let [token (token-db/create database {:user "user1"
                                         :validity 10
                                         :name "integration_token"})]
    (testing "creating user: success"
      (test-http
       {:status 200
        :body (js {:ok true})}
       (client/post (str meuse-url "/api/v1/meuse/user")
                    {:headers {"Authorization" token}
                     :content-type :json
                     :body (js {:description "integration test user"
                                :password "azertyui"
                                :name "integration"
                                :active true
                                :role "tech"})
                     :throw-exceptions false})))
    (testing "creating user: auth issue"
      (test-http
       {:status 403
        :body (js {:errors [{:detail "token missing in the header"}]})}
       (client/post (str meuse-url "/api/v1/meuse/user")
                    {:headers {}
                     :content-type :json
                     :body (js {:description "foo"
                                :password "azertyui"
                                :name "integration"
                                :active true
                                :role "tech"})
                     :throw-exceptions false}
                    ))
      )))
