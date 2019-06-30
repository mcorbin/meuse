(ns meuse.integration.integration-test
  (:require meuse.api.crate.download
            meuse.api.crate.new
            meuse.api.crate.owner
            meuse.api.crate.search
            meuse.api.crate.yank
            meuse.api.meuse.category
            meuse.api.meuse.token
            meuse.api.meuse.user
            [meuse.auth.token :as auth-token]
            [meuse.crate-test :as crate-test]
            [meuse.db :refer [database]]
            [meuse.db.token :as token-db]
            [meuse.helpers.fixtures :refer :all]
            [meuse.http :as http]
            [cheshire.core :as json]
            [clj-http.client :as client]
            [clojure.test :refer :all]))

(def meuse-url "http://127.0.0.1:8855")

(use-fixtures :each project-fixture)

(defn js
  [payload]
  (json/generate-string payload))

(defn test-http
  [expected actual]
  (is (= expected
         (select-keys actual (keys expected)))))

(deftest ^:integration meuse-api-integration-test
  ;; create a token for an admin user
  (let [token (token-db/create database {:user "user1"
                                         :validity 10
                                         :name "integration_token"})
        _ (testing "creating user: success"
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
        _ (testing "creating user: not active"
            (test-http
             {:status 200
              :body (js {:ok true})}
             (client/post (str meuse-url "/api/v1/meuse/user")
                          {:headers {"Authorization" token}
                           :content-type :json
                           :body (js {:description "integration test user not active"
                                      :password "azertyui"
                                      :name "integration_not_active"
                                      :active false
                                      :role "tech"})
                           :throw-exceptions false})))
        integration-token (token-db/create database
                                           {:user "integration"
                                            :validity 10
                                            :name "integration_token_user"})
        integration-na-token (token-db/create database
                                              {:user "integration_not_active"
                                               :validity 10
                                               :name "integration_token_na"})]
    ;; create user
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
                     :throw-exceptions false}))
      (test-http
       {:status 403
        :body (js {:errors [{:detail "invalid token"}]})}
       (client/post (str meuse-url "/api/v1/meuse/user")
                    {:headers {"Authorization" "lol"}
                     :content-type :json
                     :body (js {:description "foo"
                                :password "azertyui"
                                :name "integration"
                                :active true
                                :role "tech"})
                     :throw-exceptions false}))
      (test-http
       {:status 403
        :body (js {:errors [{:detail "user is not active"}]})}
       (client/post (str meuse-url "/api/v1/meuse/user")
                    {:headers {"Authorization" integration-na-token}
                     :content-type :json
                     :body (js {:description "foo"
                                :password "azertyui"
                                :name "integration"
                                :active true
                                :role "tech"})
                     :throw-exceptions false}))
      (test-http
       {:status 403
        :body (js {:errors [{:detail "token not found"}]})}
       (client/post (str meuse-url "/api/v1/meuse/user")
                    {:headers {"Authorization" (auth-token/generate-token)}
                     :content-type :json
                     :body (js {:description "foo"
                                :password "azertyui"
                                :name "integration"
                                :active true
                                :role "tech"})
                     :throw-exceptions false}))
      (test-http
       {:status 403
        :body (js {:errors [{:detail "invalid token"}]})}
       (client/post (str meuse-url "/api/v1/meuse/user")
                    {:headers {"Authorization" (str integration-token "A")}
                     :content-type :json
                     :body (js {:description "foo"
                                :password "azertyui"
                                :name "integration"
                                :active true
                                :role "tech"})
                     :throw-exceptions false}))
      (test-http
       {:status 403
        :body (js {:errors [{:detail "bad permissions"}]})}
       (client/post (str meuse-url "/api/v1/meuse/user")
                    {:headers {"Authorization" integration-token}
                     :content-type :json
                     :body (js {:description "foo"
                                :password "azertyui"
                                :name "integration"
                                :active true
                                :role "tech"})
                     :throw-exceptions false}))
      ;; delete user
      (testing "creating user: success"
        (test-http
         {:status 200
          :body (js {:ok true})}
         (client/post (str meuse-url "/api/v1/meuse/user")
                      {:headers {"Authorization" token}
                       :content-type :json
                       :body (js {:description "integration test user"
                                  :password "azertyui"
                                  :name "integration_deleted"
                                  :active true
                                  :role "tech"})
                       :throw-exceptions false})))
      (testing "deleting user: success"
        (test-http
         {:status 200
          :body (js {:ok true})}
         (client/delete (str meuse-url "/api/v1/meuse/user/integration_deleted")
                        {:headers {"Authorization" token}
                         :content-type :json
                         :throw-exceptions false})))
      (testing "deleting user: not admin"
        (test-http
         {:status 403
          :body (js {:errors [{:detail "bad permissions"}]})}
         (client/delete (str meuse-url "/api/v1/meuse/user/integration_deleted")
                        {:headers {"Authorization" integration-token}
                         :content-type :json
                         :throw-exceptions false})))
      (testing "deleting user: invalid token"
        (test-http
         {:status 403
          :body (js {:errors [{:detail "invalid token"}]})}
         (client/delete (str meuse-url "/api/v1/meuse/user/integration_deleted")
                        {:headers {"Authorization" (str integration-token "A")}
                         :content-type :json
                         :throw-exceptions false})))
      ;; create token
      (testing "creating token: success"
        (test-http
         {:status 200}
         (client/post (str meuse-url "/api/v1/meuse/token/")
                      {:content-type :json
                       :throw-exceptions false
                       :body (js {:name "new token integration"
                                  :user "integration"
                                  :password "azertyui"
                                  :validity 10})})))
      (testing "creating token: token already exists"
        (test-http
         {:status 400
          :body (js {:errors [{:detail "a token named new token integration already exists for user integration"}]})}
         (client/post (str meuse-url "/api/v1/meuse/token/")
                      {:content-type :json
                       :throw-exceptions false
                       :body (js {:name "new token integration"
                                  :user "integration"
                                  :password "azertyui"
                                  :validity 10})})))
      (testing "creating token: user does not exist"
        (test-http
         {:status 400
          :body (js {:errors [{:detail "the user foofoofoo does not exist"}]})}
         (client/post (str meuse-url "/api/v1/meuse/token/")
                      {:content-type :json
                       :throw-exceptions false
                       :body (js {:name "new token integration"
                                  :user "foofoofoo"
                                  :password "azertyui"
                                  :validity 10})})))
      (testing "creating token: invalid password"
        (test-http
         {:status 403
          :body (js {:errors [{:detail "invalid password"}]})}
         (client/post (str meuse-url "/api/v1/meuse/token/")
                      {:content-type :json
                       :throw-exceptions false
                       :body (js {:name "new token integration"
                                  :user "integration"
                                  :password "invalidpassword"
                                  :validity 10})})))
      (testing "creating token: user is not active"
        (test-http
         {:status 403
          :body (js {:errors [{:detail "user is not active"}]})}
         (client/post (str meuse-url "/api/v1/meuse/token/")
                      {:content-type :json
                       :throw-exceptions false
                       :body (js {:name "new token integration"
                                  :user "integration_not_active"
                                  :password "azertyui"
                                  :validity 10})})))
      ;; delete token
      (testing "delete token: success"
        (test-http
         {:status 200}
         (client/delete (str meuse-url "/api/v1/meuse/token/")
                        {:content-type :json
                         :headers {"Authorization" integration-token}
                         :throw-exceptions false
                         :body (js {:name "new token integration"
                                    :user "integration"})})))
      (testing "delete token: an admin can delete a token for another account"
        ;; first, recreate the token
        (test-http
         {:status 200}
         (client/post (str meuse-url "/api/v1/meuse/token/")
                      {:content-type :json
                       :throw-exceptions false
                       :body (js {:name "new token integration"
                                  :user "integration"
                                  :password "azertyui"
                                  :validity 10})}))
        (test-http
         {:status 200}
         (client/delete (str meuse-url "/api/v1/meuse/token/")
                        {:content-type :json
                         :headers {"Authorization" token}
                         :throw-exceptions false
                         :body (js {:name "new token integration"
                                    :user "integration"})})))
      (testing "delete token: unauthorize"
        (test-http
         {:status 403
          :body (js {:errors [{:detail "user integration cannot delete token for integration_not_active"}]})}
         (client/delete (str meuse-url "/api/v1/meuse/token/")
                        {:content-type :json
                         :headers {"Authorization" integration-token}
                         :throw-exceptions false
                         :body (js {:name "integration_token_na"
                                    :user "integration_not_active"
                                    })})))
      (testing "delete token: the token does not exist"
        (test-http
         {:status 404
          :body (js {:errors [{:detail "the token notfound does not exist for the user integration_not_active"}]})}
         (client/delete (str meuse-url "/api/v1/meuse/token/")
                        {:content-type :json
                         :headers {"Authorization" token}
                         :throw-exceptions false
                         :body (js {:name "notfound"
                                    :user "integration_not_active"
                                    })})))
      (testing "delete token: the user does not exist"
        (test-http
         {:status 404
          :body (js {:errors [{:detail "the user notfound does not exist"}]})}
         (client/delete (str meuse-url "/api/v1/meuse/token/")
                        {:content-type :json
                         :headers {"Authorization" token}
                         :throw-exceptions false
                         :body (js {:name "notfound"
                                    :user "notfound"})})))
      ;; categories
      (testing "create categories: success"
        (test-http
         {:status 200
          :body (js {:ok true})}
         (client/post (str meuse-url "/api/v1/meuse/category/")
                      {:content-type :json
                       :headers {"Authorization" token}
                       :throw-exceptions false
                       :body (js {:name "new_category"
                                  :description "category_description"})})))
      (testing "create categories: error already exists"
        (test-http
         {:status 400
          :body (js {:errors [{:detail "the category new_category already exists"}]})}
         (client/post (str meuse-url "/api/v1/meuse/category/")
                      {:content-type :json
                       :headers {"Authorization" token}
                       :throw-exceptions false
                       :body (js {:name "new_category"
                                  :description "category_description"})})))
      (testing "create categories: not admin"
        (test-http
         {:status 403
          :body (js {:errors [{:detail "bad permissions"}]})}
         (client/post (str meuse-url "/api/v1/meuse/category/")
                      {:content-type :json
                       :headers {"Authorization" integration-token}
                       :throw-exceptions false
                       :body (js {:name "new_category"
                                  :description "category_description"})}))))))

(deftest ^:integration crate-api-integration-test
  ;; create a token for an admin user
  (let [token (token-db/create database {:user "user1"
                                         :validity 10
                                         :name "integration_token"})
        _ (testing "creating user: success"
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
        _ (testing "creating user: not active"
            (test-http
             {:status 200
              :body (js {:ok true})}
             (client/post (str meuse-url "/api/v1/meuse/user")
                          {:headers {"Authorization" token}
                           :content-type :json
                           :body (js {:description "integration test user not active"
                                      :password "azertyui"
                                      :name "integration_not_active"
                                      :active false
                                      :role "tech"})
                           :throw-exceptions false})))
        integration-token (token-db/create database
                                           {:user "integration"
                                            :validity 10
                                            :name "integration_token_user"})
        integration-na-token (token-db/create database
                                              {:user "integration_not_active"
                                               :validity 10
                                               :name "integration_token_na"})]
    ;; create crate
    (testing "create a web category"
      (test-http
       {:status 200
        :body (js {:ok true})}
       (client/post (str meuse-url "/api/v1/meuse/category/")
                    {:content-type :json
                     :headers {"Authorization" token}
                     :throw-exceptions false
                     :body (js {:name "web"
                                :description "category_description"})})))
    (testing "publish a crate: success"
      (test-http
       {:status 200
        :body (js {:warning {:invalid_categories []
                             :invalid_badges []
                             :other []}})}
       (client/put (str meuse-url "/api/v1/crates/new/")
                   {:content-type :json
                    :headers {"Authorization" token}
                    :throw-exceptions false
                    :body (:body (crate-test/create-publish-request
                                  {:name "foo" :vers "1.10.2" :yanked false}
                                  "crate file content"))})))
    (testing "publish a crate: invalid semver"
      (test-http
       {:status 200
        :body (js {:errors [{:detail "Wrong input parameters:\n - field vers: the value should be a valid semver string\n"}]})}
       (client/put (str meuse-url "/api/v1/crates/new/")
                   {:content-type :json
                    :headers {"Authorization" token}
                    :throw-exceptions false
                    :body (:body (crate-test/create-publish-request
                                  {:name "foo" :vers "1.10" :yanked false}
                                  "crate file content"))})))
    (testing "publish a crate: new version with category"
      (test-http
       {:status 200
        :body (js {:warning {:invalid_categories []
                             :invalid_badges []
                             :other []}})}
       (client/put (str meuse-url "/api/v1/crates/new/")
                   {:content-type :json
                    :headers {"Authorization" token}
                    :throw-exceptions false
                    :body (:body (crate-test/create-publish-request
                                  {:name "foo"
                                   :vers "1.10.3"
                                   :yanked false
                                   :categories ["web"]}
                                  "crate file content"))})))
    (testing "publish a crate: error: version already exists"
      (test-http
       {:status 200
        :body (js {:errors [{:detail "release 1.10.3 for crate foo already exists"}]})}
       (client/put (str meuse-url "/api/v1/crates/new/")
                   {:content-type :json
                    :headers {"Authorization" token}
                    :throw-exceptions false
                    :body (:body (crate-test/create-publish-request
                                  {:name "foo"
                                   :vers "1.10.3"
                                   :yanked false
                                   :categories ["web"]}
                                  "crate file content"))})))
    (testing "publish a crate: error: the user does not own the crate"
      (test-http
       {:status 200
        :body (js {:errors [{:detail "the user does not own the crate"}]})}
       (client/put (str meuse-url "/api/v1/crates/new/")
                   {:content-type :json
                    :headers {"Authorization" integration-token}
                    :throw-exceptions false
                    :body (:body (crate-test/create-publish-request
                                  {:name "foo"
                                   :vers "1.10.4"
                                   :yanked false
                                   :categories ["web"]}
                                  "crate file content"))})))
    ;; yank
    ))
