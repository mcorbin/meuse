(ns meuse.db.token-test
  (:require [meuse.db :refer [database]]
            [meuse.db.token :as token-db]
            [meuse.helpers.fixtures :refer :all]
            [clj-time.core :as t]
            [crypto.password.bcrypt :as bcrypt]
            [clojure.test :refer :all])
  (:import org.joda.time.DateTime
           clojure.lang.ExceptionInfo))

(use-fixtures :once db-fixture)
(use-fixtures :each table-fixture)

(deftest create-get-token-test
  (testing "success"
    (let [user-name "user2"
          validity 10
          token (token-db/create-token database user-name validity)]
      (let [[db-token :as tokens] (token-db/get-user-tokens database "user2")]
        (is (= 1 (count tokens)))
        (is (uuid? (:token-id db-token)))
        (is (t/within? (t/minus (t/now) (t/minutes 1))
                       (t/now)
                       (DateTime. (:token-created-at db-token))))
        (is (t/within? (t/plus (t/minus (t/now) (t/minutes 1)) (t/days validity))
                       (t/plus (t/now) (t/days validity))
                       (DateTime. (:token-expired-at db-token))))
        (is (bcrypt/check token (:token-token db-token))))))
  (testing "errors"
    (is (thrown-with-msg?
         ExceptionInfo
         #"the user toto does not exist"
         (token-db/create-token database "toto" 10))))
  (testing "errors"
    (is (thrown-with-msg?
         ExceptionInfo
         #"the user toto does not exist"
         (token-db/get-user-tokens database "toto")))))
