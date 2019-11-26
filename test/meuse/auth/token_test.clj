(ns meuse.auth.token-test
  (:require [meuse.auth.token :refer :all]
            [meuse.db.actions.token :as token-db]
            [meuse.db :refer [database]]
            [meuse.helpers.fixtures :refer :all]
            [clj-time.core :as t]
            [clojure.test :refer :all]
            [crypto.password.bcrypt :as bcrypt])
  (:import clojure.lang.ExceptionInfo))

(use-fixtures :once system-fixture)
(use-fixtures :each db-clean-fixture table-fixture)

(deftest expiration-date-test
  (let [n (t/now)
        validity 5
        expiration (expiration-date validity)]
    (is (t/within? n
                   (t/plus (t/now) (t/days validity))
                   expiration))
    (is (not (t/within? n
                        (t/plus (t/now) (t/days (dec validity)))
                        expiration)))))

(deftest generate-token-test
  (let [token (generate-token)]
    (is (= (count (extract-identifier token)) identifier-size))
    (is (= (count token) (+ identifier-size token-size)))))

(deftest valid?-test
  (let [token (token-db/create database {:user "user2"
                                         :validity 10
                                         :name "mytoken"})
        db-token (token-db/get-token-user-role database token)]
    (is (valid? token db-token))
    (is (not (valid? token (assoc db-token :tokens/expired_at (t/now)))))
    (is (not (valid? "foo" db-token)))))
