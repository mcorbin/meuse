(ns meuse.db.actions.token-test
  (:require [meuse.auth.token :as auth-token]
            [meuse.db :refer [database]]
            [meuse.db.actions.token :as token-db]
            [meuse.helpers.fixtures :refer :all]
            [clj-time.core :as t]
            [crypto.password.bcrypt :as bcrypt]
            [clojure.test :refer :all])
  (:import org.joda.time.DateTime
           clojure.lang.ExceptionInfo))

(use-fixtures :once system-fixture)
(use-fixtures :each db-clean-fixture table-fixture)

(deftest create-get-token-test
  (testing "success"
    (let [user-name "user2"
          validity 10
          token-name "mytoken"
          token (token-db/create database {:user user-name
                                           :validity validity
                                           :name token-name})]
      (let [[db-token :as tokens] (token-db/by-user database "user2")]
        (is (= 1 (count tokens)))
        (is (= (auth-token/extract-identifier token)
               (:tokens/identifier db-token)))
        (is (uuid? (:tokens/id db-token)))
        (is (= token-name (:tokens/name db-token)))
        (is (t/within? (t/minus (t/now) (t/minutes 1))
                       (t/now)
                       (DateTime. (:tokens/created_at db-token))))
        (is (t/within? (t/plus (t/minus (t/now) (t/minutes 1)) (t/days validity))
                       (t/plus (t/now) (t/days validity))
                       (DateTime. (:tokens/expired_at db-token))))
        (is (auth-token/valid? token db-token))
        (is (thrown-with-msg?
             ExceptionInfo
             (re-pattern (format "a token named %s already exists for user %s"
                                 token-name
                                 user-name))
             (token-db/create database {:user user-name
                                        :validity validity
                                        :name token-name}))))))
  (testing "the user does not exist"
    (is (thrown-with-msg?
         ExceptionInfo
         #"the user toto does not exist"
         (token-db/create database {:user "toto"
                                    :validity 10
                                    :name "foo"})))))

(deftest delete-token-test
  (testing "success"
    (let [user-name "user2"
          validity 10
          token-name "mytoken"
          token (token-db/create database {:user user-name
                                           :validity validity
                                           :name token-name})]
      (token-db/delete database user-name token-name)
      (is (= 0 (count (token-db/by-user database "user2"))))))
  (testing "errors"
    (is (thrown-with-msg?
         ExceptionInfo
         #"the user toto does not exist"
         (token-db/delete database "toto" "foo")))
    (is (thrown-with-msg?
         ExceptionInfo
         #"the token foo does not exist for the user user2"
         (token-db/delete database "user2" "foo")))))

(deftest get-token-user-role-test
  (testing "success"
    (let [user-name "user2"
          validity 10
          token-name "mytoken"
          token (token-db/create database {:user user-name
                                           :validity validity
                                           :name token-name})
          db-token (token-db/get-token-user-role database token)]
      (is (= token-name (:tokens/name db-token)))
      (is (= (auth-token/extract-identifier token)
             (:tokens/identifier db-token)))
      (is (auth-token/valid? token db-token))
      (is (= user-name (:users/name db-token)))
      (is (= "tech" (:roles/name db-token))))))

(deftest set-last-used-test
  (let [user-name "user2"
        validity 10
        token-name "mytoken"
        token (token-db/create database {:user user-name
                                         :validity validity
                                         :name token-name})
        db-token-fn (fn [] (->> (token-db/by-user database user-name)
                                (filter #(= (:tokens/name %) token-name))
                                first))
        db-token (db-token-fn)]
    (is (nil? (:tokens/last_used_at db-token)))
    (token-db/set-last-used database (:tokens/id db-token))
    (is (inst? (:tokens/last_used_at (db-token-fn))))))
