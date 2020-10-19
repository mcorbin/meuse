(ns meuse.integration.integration-test
  (:require meuse.api.crate.download
            meuse.api.crate.new
            meuse.api.crate.owner
            meuse.api.crate.search
            meuse.api.crate.yank
            meuse.api.meuse.category
            meuse.api.meuse.crate
            meuse.api.meuse.token
            meuse.api.meuse.user
            [meuse.api.public.healthz :refer [healthz-msg]]
            [meuse.api.public.me :refer [me-msg]]
            [meuse.auth.token :as auth-token]
            [meuse.crate-test :as crate-test]
            [meuse.db :refer [database]]
            [meuse.db.actions.token :as token-db]
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
        _ (testing "creating user: success"
            (test-http
             {:status 200
              :body (js {:ok true})}
             (client/post (str meuse-url "/api/v1/meuse/user")
                          {:headers {"Authorization" token}
                           :content-type :json
                           :body (js {:description "integration2 test user"
                                      :password "azertyui"
                                      :name "integration2"
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
        integration2-token (token-db/create database
                                            {:user "integration2"
                                             :validity 10
                                             :name "integration2_token_user"})
        integration-na-token (token-db/create database
                                              {:user "integration_not_active"
                                               :validity 10
                                               :name "integration_token_na"})]
    ;; check crates
    (testing "check crates: success"
      (let [{:keys [status body]} (client/get
                                   (str meuse-url "/api/v1/meuse/check")
                                   {:headers {"Authorization" integration-token}
                                    :content-type :json
                                    :throw-exceptions false})
            body (json/parse-string body true)]
        (is (= 200 status))
        (is (= 3 (count body)))))
    (testing "check crates: invalid token"
      (test-http
       {:status 403
        :body (js {:errors [{:detail "invalid token"}]})}
       (client/get (str meuse-url "/api/v1/meuse/check")
                   {:headers {"Authorization" (str integration-token "a")}
                    :content-type :json
                    :throw-exceptions false})))
    ;; list crates
    (testing "list crates: success"
      (let [response (client/get (str meuse-url "/api/v1/meuse/crate")
                                 {:headers {"Authorization" integration-token}
                                  :content-type :json
                                  :throw-exceptions false})
            crates (:crates (json/parse-string (:body response) true))
            crate1 (first (filter #(= (:name %) "crate1") crates))
            crate2 (first (filter #(= (:name %) "crate2") crates))
            crate2 (first (filter #(= (:name %) "crate3") crates))]
        (is (= 200 (:status response)))
        (is (= (count crates) 3))
        (is (= 3 (count (:versions crate1))))
        (is (= 1 (count (:versions crate2))))
        (is (= 1 (count (:versions crate2))))))
    (testing "list crates: category"
      (let [response (client/get (str meuse-url "/api/v1/meuse/crate?category=email")
                                 {:headers {"Authorization" integration-token}
                                  :content-type :json
                                  :throw-exceptions false})
            crates (:crates (json/parse-string (:body response) true))
            crate1 (first crates)]
        (is (= 200 (:status response)))
        (is (= 1 (count crates)))
        (is (string? (:id crate1)))
        (is (= "crate1" (:name crate1)))
        (is (= 3 (count (:versions crate1))))))
    (testing "list crates: invalid token"
      (test-http
       {:status 403
        :body (js {:errors [{:detail "invalid token"}]})}
       (client/get (str meuse-url "/api/v1/meuse/crate")
                   {:headers {"Authorization" (str token "A")}
                    :content-type :json
                    :throw-exceptions false})))
    ;; get crate
    (testing "get crate: success"
      (let [response (client/get (str meuse-url "/api/v1/meuse/crate/crate1")
                                 {:headers {"Authorization" integration-token}
                                  :content-type :json
                                  :throw-exceptions false})
            crate (json/parse-string (:body response) true)]
        (is (= 200 (:status response)))
        (is (= (count (:versions crate)) 3))
        (is (= #{"email" "system"} (set (map :name (:categories crate)))))
        (is (= #{"the email category" "the system category"}
               (set (map :description (:categories crate)))))
        (mapv #(is (string? (:id %))) (:categories crate))))
    (testing "get crate: invalid token"
      (test-http
       {:status 403
        :body (js {:errors [{:detail "invalid token"}]})}
       (client/get (str meuse-url "/api/v1/meuse/crate/crate1")
                   {:headers {"Authorization" (str integration-token "B")}
                    :content-type :json
                    :throw-exceptions false})))
    ;; list users
    (testing "list users: success"
      (let [response (client/get (str meuse-url "/api/v1/meuse/user")
                                 {:headers {"Authorization" token}
                                  :content-type :json
                                  :throw-exceptions false})
            users (:users (json/parse-string (:body response) true))
            user1 (-> (filter #(= (:name %) "user1") users)
                      first)]
        (is (= 200 (:status response)))
        (is (= 8 (count users)))
        (is (= {:name "user1"
                :role "admin"
                :description "desc1"
                :active true}
               (dissoc user1 :id)))
        (is (string? (:id user1)))))
    (testing "list users: bad permissions"
      (test-http
       {:status 403
        :body (js {:errors [{:detail "bad permissions"}]})}
       (client/get (str meuse-url "/api/v1/meuse/user")
                   {:headers {"Authorization" integration-token}
                    :content-type :json
                    :throw-exceptions false})))
    (testing "list users: invalid token"
      (test-http
       {:status 403
        :body (js {:errors [{:detail "invalid token"}]})}
       (client/get (str meuse-url "/api/v1/meuse/user")
                   {:headers {"Authorization" "lol"}
                    :content-type :json
                    :throw-exceptions false})))
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
                     :throw-exceptions false})))
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
    ;; update user
    (testing "update user by admin: success"
      (test-http
       {:status 200
        :body (js {:ok true})}
       (client/post (str meuse-url "/api/v1/meuse/user/integration2")
                    {:headers {"Authorization" token}
                     :content-type :json
                     :body (js {:description "integration test user"
                                :password "new_password"
                                :active true
                                :role "admin"})
                     :throw-exceptions false})))
    (testing "update user by admin: success"
      (test-http
       {:status 200
        :body (js {:ok true})}
       (client/post (str meuse-url "/api/v1/meuse/user/integration2")
                    {:headers {"Authorization" token}
                     :content-type :json
                     :body (js {:description "integration test user update"
                                :password "new_password"
                                :active true
                                :role "tech"})
                     :throw-exceptions false})))
    (testing "update user by itself (not admin): success"
      (test-http
       {:status 200
        :body (js {:ok true})}
       (client/post (str meuse-url "/api/v1/meuse/user/integration2")
                    {:headers {"Authorization" integration2-token}
                     :content-type :json
                     :body (js {:description "integration test user update2"
                                :password "new_password2"})
                     :throw-exceptions false})))
    (testing "update user: invalid token"
      (test-http
       {:status 403
        :body (js {:errors [{:detail "invalid token"}]})}
       (client/post (str meuse-url "/api/v1/meuse/user/integration2")
                    {:headers {"Authorization" (str integration2-token "A")}
                     :content-type :json
                     :throw-exceptions false})))
    (testing "update user: cannot update a role if not admin"
      (test-http
       {:status 403
        :body (js {:errors [{:detail "only admins can update an user role"}]})}
       (client/post (str meuse-url "/api/v1/meuse/user/integration2")
                    {:headers {"Authorization" integration2-token}
                     :content-type :json
                     :body (js {:role "admin"})
                     :throw-exceptions false})))
    (testing "update user: cannot update active if not admin"
      (test-http
       {:status 403
        :body (js {:errors [{:detail "only admins can enable or disable an user"}]})}
       (client/post (str meuse-url "/api/v1/meuse/user/integration2")
                    {:headers {"Authorization" integration2-token}
                     :content-type :json
                     :body (js {:active false})
                     :throw-exceptions false})))
    (testing "update user: cannot update another user"
      (test-http
       {:status 403
        :body (js {:errors [{:detail "bad permissions"}]})}
       (client/post (str meuse-url "/api/v1/meuse/user/integration")
                    {:headers {"Authorization" integration2-token}
                     :body (js {:description "foo"})
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
       {:status 404
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
                                  :user "integration_not_active"})})))
    (testing "delete token: the token does not exist"
      (test-http
       {:status 404
        :body (js {:errors [{:detail "the token notfound does not exist for the user integration_not_active"}]})}
       (client/delete (str meuse-url "/api/v1/meuse/token/")
                      {:content-type :json
                       :headers {"Authorization" token}
                       :throw-exceptions false
                       :body (js {:name "notfound"
                                  :user "integration_not_active"})})))
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
    ;; list token
    (testing "list tokens for the integration user"
      (let [response (client/get (str meuse-url "/api/v1/meuse/token/")
                                 {:content-type :json
                                  :headers {"Authorization" integration-token}
                                  :throw-exceptions false})
            tokens (-> (:body response)
                       (json/parse-string true)
                       :tokens)]
        (test-http
         {:status 200}
         response)
        (is (= 1 (count tokens)))
        (is (string? (:id (first tokens))))
        (is (= "integration_token_user" (:name (first tokens))))
        (is (string? (:created-at (first tokens))))
        (is (string? (:expired-at (first tokens))))
        (is (string? (:last-used-at (first tokens))))
        (is (= 5 (count (keys (first tokens)))))))
    (testing "list tokens: admin can list tokens for another user"
      (let [response (client/get (str meuse-url "/api/v1/meuse/token?user=integration")
                                 {:content-type :json
                                  :headers {"Authorization" token}
                                  :throw-exceptions false})
            tokens (-> (:body response)
                       (json/parse-string true)
                       :tokens)]
        (test-http
         {:status 200}
         response)
        (is (= 1 (count tokens)))
        (is (string? (:id (first tokens))))
        (is (= "integration_token_user" (:name (first tokens))))
        (is (string? (:created-at (first tokens))))
        (is (string? (:expired-at (first tokens))))
        (is (string? (:last-used-at (first tokens))))
        (is (= 5 (count (keys (first tokens)))))))
    (testing "list tokens: no auth"
      (test-http
       {:status 403
        :body (js {:errors [{:detail "token missing in the header"}]})}
       (client/get (str meuse-url "/api/v1/meuse/token")
                   {:content-type :json
                    :throw-exceptions false})))
    (testing "list tokens: cannot list tokens for another user if non admin"
      (test-http
       {:status 403
        :body (js {:errors [{:detail "bad permissions"}]})}
       (client/get (str meuse-url "/api/v1/meuse/token?user=integration_not_active")
                   {:content-type :json
                    :headers {"Authorization" integration-token}
                    :throw-exceptions false})))
    ;; list categories
    (testing "list categories: success"
      (let [response (client/get (str meuse-url "/api/v1/meuse/category")
                                 {:content-type :json
                                  :headers {"Authorization" token}
                                  :throw-exceptions false})
            categories (:categories (json/parse-string (:body response) true))
            email (-> (filter #(= "email" (:name %)) categories)
                      first)
            system (-> (filter #(= "system" (:name %)) categories)
                       first)]
        (is (= 200 (:status response)))
        (is (= 2 (count categories)))
        (is (string? (:id system)))
        (is (= (dissoc system :id)
               {:name "system"
                :description "the system category"}))
        (is (string? (:id email)))
        (is (= (dissoc email :id)
               {:name "email"
                :description "the email category"}))))
    ;; create categories
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
                                :description "category_description"})})))
    ;; update category
    (testing "update category: success"
      (test-http
       {:status 200
        :body (js {:ok true})}
       (client/post (str meuse-url "/api/v1/meuse/category/new_category")
                    {:content-type :json
                     :headers {"Authorization" token}
                     :throw-exceptions false
                     :body (js {:name "new_name"
                                :description "new_description"})})))
    (testing "update category: not admin"
      (test-http
       {:status 403
        :body (js {:errors [{:detail "bad permissions"}]})}
       (client/post (str meuse-url "/api/v1/meuse/category/new_name")
                    {:content-type :json
                     :headers {"Authorization" integration-token}
                     :throw-exceptions false
                     :body (js {:name "new_category"
                                :description "category_description"})})))
    (testing "update category: invalid parameters"
      (test-http
       {:status 400
        :body (js {:errors [{:detail "Wrong input parameters:\n - field body is incorrect\n"}]})}
       (client/post (str meuse-url "/api/v1/meuse/category/new_name")
                    {:content-type :json
                     :headers {"Authorization" token}
                     :throw-exceptions false})))
    (testing "statistics: success"
      (test-http
       {:status 200
        :body (js {:crates 3 :crates-versions 5 :downloads 0 :users 8})}
       (client/get (str meuse-url "/api/v1/meuse/statistics")
                   {:content-type :json
                    :throw-exceptions false
                    :headers {"Authorization" integration-token}})))
    (testing "statistics: no auth"
      (test-http
       {:status 403
        :body (js {:errors [{:detail "token missing in the header"}]})}
       (client/get (str meuse-url "/api/v1/meuse/statistics")
                   {:content-type :json
                    :throw-exceptions false})))))

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
    ;; yank/unyank
    (testing "yank a crate: success"
      (test-http
       {:status 200
        :body (js {:ok true})}
       (client/delete (str meuse-url "/api/v1/crates/foo/1.10.3/yank")
                      {:content-type :json
                       :headers {"Authorization" token}
                       :throw-exceptions false})))
    (testing "yank: error: already yank"
      (test-http
       {:status 200
        :body (js {:errors [{:detail "cannot yank the crate: crate state is already yank"}]})}
       (client/delete (str meuse-url "/api/v1/crates/foo/1.10.3/yank")
                      {:content-type :json
                       :headers {"Authorization" token}
                       :throw-exceptions false})))
    (testing "yank: error: does not own the crate"
      (test-http
       {:status 200
        :body (js {:errors [{:detail "user does not own the crate foo"}]})}
       (client/delete (str meuse-url "/api/v1/crates/foo/1.10.3/yank")
                      {:content-type :json
                       :headers {"Authorization" integration-token}
                       :throw-exceptions false})))
    (testing "yank: error: crate does not exist"
      (test-http
       {:status 200
        :body (js {:errors [{:detail "cannot yank the crate: the crate does not exist"}]})}
       (client/delete (str meuse-url "/api/v1/crates/bar/1.10.3/yank")
                      {:content-type :json
                       :headers {"Authorization" token}
                       :throw-exceptions false})))
    (testing "yank: error: version does not exist"
      (test-http
       {:status 200
        :body (js {:errors [{:detail "cannot yank the crate: the version does not exist"}]})}
       (client/delete (str meuse-url "/api/v1/crates/foo/3.10.3/yank")
                      {:content-type :json
                       :headers {"Authorization" token}
                       :throw-exceptions false})))
    (testing "unyank a crate: success"
      (test-http
       {:status 200
        :body (js {:ok true})}
       (client/put (str meuse-url "/api/v1/crates/foo/1.10.3/unyank")
                   {:content-type :json
                    :headers {"Authorization" token}
                    :throw-exceptions false})))
    ;; add/remove owner
    (testing "add owner"
      (test-http
       {:status 200
        :body (js {:ok true
                   :msg "added user(s) integration as owner(s) of crate foo"})}
       (client/put (str meuse-url "/api/v1/crates/foo/owners")
                   {:content-type :json
                    :headers {"Authorization" token}
                    :throw-exceptions false
                    :body (js {:users ["integration"]})})))
    (testing "add owner: error: already owns the crate"
      (test-http
       {:status 200
        :body (js {:errors [{:detail "the user integration already owns the crate foo"}]})}
       (client/put (str meuse-url "/api/v1/crates/foo/owners")
                   {:content-type :json
                    :headers {"Authorization" token}
                    :throw-exceptions false
                    :body (js {:users ["integration"]})})))
    (testing "remove owner"
      (test-http
       {:status 200
        :body (js {:ok true
                   :msg "removed user(s) integration as owner(s) of crate foo"})}
       (client/delete (str meuse-url "/api/v1/crates/foo/owners")
                      {:content-type :json
                       :headers {"Authorization" token}
                       :throw-exceptions false
                       :body (js {:users ["integration"]})})))
    (testing "remove owner: error: does not own the crate"
      (test-http
       {:status 200
        :body (js {:errors [{:detail "the user integration does not own the crate foo"}]})}
       (client/delete (str meuse-url "/api/v1/crates/foo/owners")
                      {:content-type :json
                       :headers {"Authorization" token}
                       :throw-exceptions false
                       :body (js {:users ["integration"]})})))
    (testing "add owner: error: does not own the crate"
      (test-http
       {:status 200
        :body (js {:errors [{:detail "user does not own the crate foo"}]})}
       (client/put (str meuse-url "/api/v1/crates/foo/owners")
                   {:content-type :json
                    :headers {"Authorization" integration-token}
                    :throw-exceptions false
                    :body (js {:users ["integration"]})})))
    (testing "add owner: error: the crate does not exist"
      (test-http
       {:status 200
        :body (js {:errors [{:detail "the crate bar does not exist"}]})}
       (client/put (str meuse-url "/api/v1/crates/bar/owners")
                   {:content-type :json
                    :headers {"Authorization" integration-token}
                    :throw-exceptions false
                    :body (js {:users ["integration"]})})))
    (testing "add owner: error: the user does not exist"
      (test-http
       {:status 200
        :body (js {:errors [{:detail "user does not own the crate foo"}]})}
       (client/put (str meuse-url "/api/v1/crates/foo/owners")
                   {:content-type :json
                    :headers {"Authorization" integration-token}
                    :throw-exceptions false
                    :body (js {:users ["does not exist"]})})))
    (testing "remove owner: error: does not own the crate"
      (test-http
       {:status 200
        :body (js {:errors [{:detail "user does not own the crate foo"}]})}
       (client/delete (str meuse-url "/api/v1/crates/foo/owners")
                      {:content-type :json
                       :headers {"Authorization" integration-token}
                       :throw-exceptions false
                       :body (js {:users ["integration"]})})))
    (testing "remove owner: error: the crate does not exist"
      (test-http
       {:status 200
        :body (js {:errors [{:detail "the crate bar does not exist"}]})}
       (client/delete (str meuse-url "/api/v1/crates/bar/owners")
                      {:content-type :json
                       :headers {"Authorization" integration-token}
                       :throw-exceptions false
                       :body (js {:users ["integration"]})})))
    (testing "remove owner: error: the user does not exist"
      (test-http
       {:status 200
        :body (js {:errors [{:detail "user does not own the crate foo"}]})}
       (client/delete (str meuse-url "/api/v1/crates/foo/owners")
                      {:content-type :json
                       :headers {"Authorization" integration-token}
                       :throw-exceptions false
                       :body (js {:users ["does not exist"]})})))
    ;; search
    (testing "search: success"
      (test-http
       {:status 200
        :body (js {:crates [{:name "foo"
                             :max_version "1.10.3"
                             :description ""}]
                   :meta {:total 1}})}
       (client/get (str meuse-url "/api/v1/crates?q=foo")
                   {:content-type :json
                    :throw-exceptions false})))
    (testing "search: no query"
      (test-http
       {:status 200
        :body (js {:errors [{:detail "Wrong input parameters:\n - field q missing in params\n"}]})}
       (client/get (str meuse-url "/api/v1/crates")
                   {:content-type :json
                    :throw-exceptions false})))
    ;; download
    (testing "download crate file"
      (test-http
       {:status 200
        :body "crate file content"}
       (client/get (str meuse-url "/api/v1/crates/foo/1.10.3/download")
                   {:content-type :json
                    :throw-exceptions false})))))

(deftest ^:integration public-api-integration-test
  (testing "/me"
    (test-http
     {:status 200
      :body me-msg}
     (client/get (str meuse-url "/me")
                 {:content-type :json
                  :throw-exceptions false})))
  (testing "/healthz"
    (test-http
     {:status 200
      :body healthz-msg}
     (client/get (str meuse-url "/healthz")
                 {:content-type :json
                  :throw-exceptions false})))
  (testing "/health"
    (test-http
     {:status 200
      :body healthz-msg}
     (client/get (str meuse-url "/health")
                 {:content-type :json
                  :throw-exceptions false})))
  (testing "/status"
    (test-http
     {:status 200
      :body healthz-msg}
     (client/get (str meuse-url "/status")
                 {:content-type :json
                  :throw-exceptions false})))
  (testing "/metrics"
    (let [{:keys [body status]} (client/get (str meuse-url "/metrics")
                                            {:content-type :json
                                             :throw-exceptions false})]
      (is (= 200 status))
      (is (.contains body "jvm_buffer_memory_used_bytes")))))

(deftest ^:integration mirror-api-integration-test
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
    ;; write a crate file in the mirror store
    (meuse.store.protocol/write-file
     meuse.mirror/mirror-store
     {:name "foo"
      :vers "1.0.0"}
     (.getBytes "file content"))
    ;; auth is disabled for the mirror for now
    (testing "download crate file: no auth"
      (test-http
       {:status 200
        :body "file content"}
       (client/get (str meuse-url "/api/v1/mirror/foo/1.0.0/download")
                   {:content-type :json
                    :headers {}
                    :throw-exceptions false})))

    (testing "cache crate file: no auth fails"
      (test-http
       {:status 403
        :body (js {:errors [{:detail "token missing in the header"}]})}
       (client/post (str meuse-url "/api/v1/mirror/foo/1.0.0/cache")
                    {:content-type :json
                     :headers {}
                     :throw-exceptions false})))

    (testing "cache crate file: works with auth"
      (test-http
       {:status 200
        :body (js {:ok true})}
       (client/post (str meuse-url "/api/v1/mirror/foo/1.0.0/cache")
                    {:content-type :json
                     :headers {"Authorization" integration-token}
                     :throw-exceptions false})))))

(deftest ^:integration front-api-integration-test
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
                           :throw-exceptions false})))]
    (testing "no auth needed for some pages"
      (let [result (client/get (str meuse-url "/front/login")
                               {})]
        (is (= 200 (:status result)))
        (is (.contains (:body result) "menu-username-input")))

      (let [result (client/get (str meuse-url "/static/css/style.css")
                               {})]
        (is (= 200 (:status result)))
        (is (.contains (:body result) ".crate-list-element")))
      (let [result (client/post (str meuse-url "/front/logout")
                                {})]
        (is (= 302 (:status result)))))
    (testing "auth failure: redirect"
      (let [result (client/get (str meuse-url "/front/crates")
                               {})]
        (is (= 200 (:status result)))
        (is (.contains (:body result) "menu-username-input"))))))
