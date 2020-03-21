(ns meuse.front.login-test
  (:require [meuse.auth.password :as password]
            [meuse.front.login :as login]
            [meuse.helpers.fixtures :as fixtures]
            [meuse.mocks.db :as mocks]
            [clojure.test :refer :all]))

(deftest check-password-test
  (testing "valid password"
    (let [pw "mypassword"
          db-user #:users{:name "foo"
                          :password (password/encrypt pw)}
          user-mock (mocks/user-mock {:by-name db-user})]
      (is (= db-user
             (login/check-password user-mock {:params {:username "foo"
                                                       :password pw}})))))
  (testing "invalid password"
    (let [pw "mypassword"
          db-user #:users{:name "foo"
                          :password (password/encrypt pw)}
          user-mock (mocks/user-mock {:by-name db-user})]
      (is (thrown-with-msg?
           clojure.lang.ExceptionInfo
           #"invalid password"
           (= db-user
              (login/check-password user-mock {:params {:username "foo"
                                                        :password "bar"}}))))))
    (testing "user not found"
    (let [db-user nil
          user-mock (mocks/user-mock {:by-name db-user})]
      (is (thrown-with-msg?
           clojure.lang.ExceptionInfo
           #"forbidden"
           (= db-user
              (login/check-password user-mock {:params {:username "foo"
                                                        :password "bar"}})))))))

(deftest login!-test
  (testing "valid password"
    (let [pw "mypassword"
          db-user #:users{:name "foo"
                          :password (password/encrypt pw)}
          user-mock (mocks/user-mock {:by-name db-user})
          result (login/login! {:params {:username "foo"
                                         :password pw}}
                               user-mock
                               fixtures/default-key-spec)]
      (is (= {:status 302
              :headers {"Location" "/front/"}
              :cookies {}}
             (update result :cookies dissoc "session-token"))
          (is (string? (get-in result [:cookies "session-token" :value])))))))


