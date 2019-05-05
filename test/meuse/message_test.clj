(ns meuse.message-test
  (:require [meuse.message :refer :all]
            [clojure.test :refer :all]))

(deftest yanked?->msg-test
  (is (= "yank" (yanked?->msg true)))
  (is (= "unyank" (yanked?->msg false))))

(deftest publish-commit-msg-test
  (is (= ["foo 1.1.2"
          "meuse published foo 1.1.2"]
         (publish-commit-msg {:name "foo"
                              :vers "1.1.2"}))))

(deftest yank-commit-msg-test
  (is (= ["foo 1.1.2"
          "meuse yank foo 1.1.2"]
         (yank-commit-msg "foo" "1.1.2" true)))
  (is (= ["foo 1.1.2"
          "meuse unyank foo 1.1.2"]
         (yank-commit-msg "foo" "1.1.2" false))))

