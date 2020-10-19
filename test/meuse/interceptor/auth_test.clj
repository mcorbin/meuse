(ns meuse.interceptor.auth-test
  (:require [meuse.auth.frontend :as auth-frontend]
            [meuse.auth.token :as auth-token]
            [meuse.helpers.fixtures :as fixtures]
            [meuse.interceptor.auth :as auth]
            [meuse.mocks.db :as mocks]
            [clj-time.core :as t]
            [crypto.password.bcrypt :as bcrypt]
            [clojure.test :refer :all])
  (:import java.util.UUID))

(def user-id (UUID/randomUUID))
(def token-clear (auth-token/generate-token))
(def token (bcrypt/encrypt token-clear))

(deftest auth-request-test
  (testing ":meuse.api.crate.http: skip auth"
    (let [itc (auth/auth-request nil nil nil nil)]
      (is (= {:request {:subsystem :meuse.api.crate.http
                        :action :search}}
             ((:enter itc) {:request {:subsystem :meuse.api.crate.http
                                      :action :search}})))
      (is (= {:request {:subsystem :meuse.api.crate.http
                        :action :download}}
             ((:enter itc) {:request {:subsystem :meuse.api.crate.http
                                      :action :download}})))))
  (testing ":meuse.api.crate.http: valid auth"
    (let [token-mock (mocks/token-mock {:get-token-user-role
                                        {:users/name "foo"
                                         :users/active true
                                         :roles/name "tech"
                                         :tokens/token token
                                         :tokens/expired_at
                                         (t/plus (t/now)
                                                 (t/months 1))}})
          itc (auth/auth-request token-mock nil nil nil)]
      (is (= {:request {:subsystem :meuse.api.crate.http
                        :action :foo
                        :headers {"authorization" token-clear}
                        :auth {:role-name "tech", :user-name "foo"}}}
             ((:enter itc) {:request {:subsystem :meuse.api.crate.http
                                      :action :foo
                                      :headers {"authorization" token-clear}}})))))
  (testing ":meuse.api.crate.http: user not active"
    (let [token-mock (mocks/token-mock {:get-token-user-role
                                        {:users/name "foo"
                                         :users/active false
                                         :roles/name "tech"
                                         :tokens/token token
                                         :tokens/expired_at
                                         (t/plus (t/now)
                                                 (t/months 1))}})
          itc (auth/auth-request token-mock nil nil nil)]
      (is (thrown-with-msg?
           clojure.lang.ExceptionInfo
           #"is not active"
           ((:enter itc) {:request {:subsystem :meuse.api.crate.http
                                    :action :foo
                                    :headers {"authorization" token-clear}}})))))
  (testing ":meuse.api.crate.http: token expired"
    (let [token-mock (mocks/token-mock {:get-token-user-role
                                        {:users/name "foo"
                                         :users/active true
                                         :roles/name "tech"
                                         :tokens/token token
                                         :tokens/expired_at
                                         (t/minus (t/now)
                                                  (t/months 1))}})
          itc (auth/auth-request token-mock nil nil nil)]
      (is (thrown-with-msg?
           clojure.lang.ExceptionInfo
           #"invalid token"
           ((:enter itc) {:request {:subsystem :meuse.api.crate.http
                                    :action :foo
                                    :headers {"authorization" token-clear}}})))))
  (testing ":meuse.api.crate.http: wrong token"
    (let [token-mock (mocks/token-mock {:get-token-user-role
                                        {:users/name "foo"
                                         :users/active true
                                         :roles/name "tech"
                                         :tokens/token "foobar"
                                         :tokens/expired_at
                                         (t/plus (t/now)
                                                 (t/months 1))}})
          itc (auth/auth-request token-mock nil nil nil)]
      (is (thrown-with-msg?
           clojure.lang.ExceptionInfo
           #"invalid token"
           ((:enter itc) {:request {:subsystem :meuse.api.crate.http
                                    :action :foo
                                    :headers {"authorization" token-clear}}})))))

  (testing ":meuse.api.meuse.http: skip auth"
    (let [itc (auth/auth-request nil nil nil nil)]
      (is (= {:request {:subsystem :meuse.api.meuse.http
                        :action :create-token}}
             ((:enter itc) {:request {:subsystem :meuse.api.meuse.http
                                      :action :create-token}})))))
  (testing ":meuse.api.meuse.http: valid auth"
    (let [token-mock (mocks/token-mock {:get-token-user-role
                                        {:users/name "foo"
                                         :users/active true
                                         :roles/name "tech"
                                         :tokens/token token
                                         :tokens/expired_at
                                         (t/plus (t/now)
                                                 (t/months 1))}})
          itc (auth/auth-request token-mock nil nil nil)]
      (is (= {:request {:subsystem :meuse.api.meuse.http
                        :action :foo
                        :headers {"authorization" token-clear}
                        :auth {:role-name "tech", :user-name "foo"}}}
             ((:enter itc) {:request {:subsystem :meuse.api.meuse.http
                                      :action :foo
                                      :headers {"authorization" token-clear}}})))))
  (testing ":meuse.api.meuse.http: user not active"
    (let [token-mock (mocks/token-mock {:get-token-user-role
                                        {:users/name "foo"
                                         :users/active false
                                         :roles/name "tech"
                                         :tokens/token token
                                         :tokens/expired_at
                                         (t/plus (t/now)
                                                 (t/months 1))}})
          itc (auth/auth-request token-mock nil nil nil)]
      (is (thrown-with-msg?
           clojure.lang.ExceptionInfo
           #"is not active"
           ((:enter itc) {:request {:subsystem :meuse.api.meuse.http
                                    :action :foo
                                    :headers {"authorization" token-clear}}})))))
  (testing ":meuse.api.meuse.http: token expired"
    (let [token-mock (mocks/token-mock {:get-token-user-role
                                        {:users/name "foo"
                                         :users/active true
                                         :roles/name "tech"
                                         :tokens/token token
                                         :tokens/expired_at
                                         (t/minus (t/now)
                                                  (t/months 1))}})
          itc (auth/auth-request token-mock nil nil nil)]
      (is (thrown-with-msg?
           clojure.lang.ExceptionInfo
           #"invalid token"
           ((:enter itc) {:request {:subsystem :meuse.api.meuse.http
                                    :action :foo
                                    :headers {"authorization" token-clear}}})))))
  (testing ":meuse.api.meuse.http: wrong token"
    (let [token-mock (mocks/token-mock {:get-token-user-role
                                        {:users/name "foo"
                                         :users/active true
                                         :roles/name "tech"
                                         :tokens/token "foobar"
                                         :tokens/expired_at
                                         (t/plus (t/now)
                                                 (t/months 1))}})
          itc (auth/auth-request token-mock nil nil nil)]
      (is (thrown-with-msg?
           clojure.lang.ExceptionInfo
           #"invalid token"
           ((:enter itc) {:request {:subsystem :meuse.api.meuse.http
                                    :action :foo
                                    :headers {"authorization" token-clear}}})))))
  (testing ":meuse.api.mirror.http: skip auth"
    (let [itc (auth/auth-request nil nil nil nil)]
      (is (= {:request {:subsystem :meuse.api.mirror.http
                        :action :download}}
             ((:enter itc) {:request {:subsystem :meuse.api.mirror.http
                                      :action :download}})))))
  (testing ":meise.api.mirror.http: auth needed"
    (let [itc (auth/auth-request nil nil nil nil)]
      (is (thrown-with-msg?
           clojure.lang.ExceptionInfo
           #"token missing in the header"
           ((:enter itc) {:request {:subsystem :meuse.api.mirror.http
                                    :action :cache}})))))
  (testing ":meuse.api.public.http"
    (let [itc (auth/auth-request nil nil nil nil)]
      (is (= {:request {:subsystem :meuse.api.public.http}}
             ((:enter itc) {:request {:subsystem :meuse.api.public.http}})))))
  (testing "meuse.front.http: skip auth"
    (doseq [action auth/front-actions-no-cookie]
      (let [itc (auth/auth-request nil nil nil nil)
            request {:subsystem :meuse.front.http
                     :action action
                     :cookies {}}]
        (is (= {:request request} ((:enter itc) {:request request}))))))
  (testing "meuse.front.http: invalid cookie"
    (let [user-mock (mocks/user-mock {:by-id #:users{:id user-id}})
          itc (auth/auth-request nil user-mock fixtures/default-key-spec nil)
          request {:subsystem :meuse.front.http
                   :cookies {"session-token"
                             {:value "foaazeeaaeaeaeaaaeazazo"}}}]
      (is (thrown? Exception
                   ((:enter itc) {:request request})))))
  (testing "meuse.front.http: valid cookie"
    (let [user-mock (mocks/user-mock {:by-id #:users{:id user-id :name "foo"}})
          itc (auth/auth-request nil user-mock fixtures/default-key-spec nil)
          request {:subsystem :meuse.front.http
                   :auth {:user-name "foo"
                          :user-id user-id}
                   :cookies {"session-token"
                             {:value (auth-frontend/generate-token
                                      user-id
                                      fixtures/default-key-spec)}}}]
      (Thread/sleep 100)
      (is (= {:request request} ((:enter itc) {:request request})))))
  (testing "meuse.front.http: auth disabled for the frontend"
    (let [user-mock (mocks/user-mock {:by-id #:users{:id user-id :name "foo"}})
          itc (auth/auth-request nil user-mock fixtures/default-key-spec true)
          request {:subsystem :meuse.front.http}]
      (Thread/sleep 100)
      (is (= {:request request} ((:enter itc) {:request request}))))))
