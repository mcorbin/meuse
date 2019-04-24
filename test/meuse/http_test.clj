(ns meuse.http-test
  (:require [meuse.http :refer :all]
            [clojure.test :refer :all]))

(deftest handle-req-errors-test
  (let [error-msg "an error msg"]
    (testing "unexpected, non-crate error"
      (is (= {:status 500
              :body {:errors [{:detail default-error-msg}]}}
             (handle-req-errors
              {}
              (ex-info error-msg {})))))
    (testing "crate error"
      (is (= {:status 200
              :body {:errors [{:detail error-msg}]}}
             (handle-req-errors
              {:subsystem :meuse.api.crate}
              (ex-info error-msg {})))))
    (testing "non crate error"
      (is (= {:status 401
              :body {:errors [{:detail error-msg}]}}
             (handle-req-errors
              {:subsystem :meuse.foo}
              (ex-info error-msg {:status 401})))))))
