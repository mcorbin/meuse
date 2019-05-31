(ns meuse.auth.password-test
  (:require [meuse.auth.password :refer :all]
            [clojure.test :refer :all])
  (:import clojure.lang.ExceptionInfo))

(deftest test-encrypt-check
  (is (check "foo" (encrypt "foo")))
  (is (check "foobar" (encrypt "foobar")))
  (is (thrown-with-msg?
       ExceptionInfo
       #"invalid password"
       (check "foobar" (encrypt "foo")))))
