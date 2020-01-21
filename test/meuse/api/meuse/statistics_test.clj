(ns meuse.api.meuse.statistics-test
  (:require [meuse.api.meuse.http :refer :all]
            [meuse.helpers.fixtures :refer :all]
            [meuse.helpers.request :refer [add-auth]]
            [clojure.test :refer :all])
  (:import clojure.lang.ExceptionInfo))

(use-fixtures :once system-fixture)
(use-fixtures :each db-clean-fixture table-fixture)

(deftest get-stats-test
  (testing "success"
    (is (= {:status 200
            :body {:crates 3, :crates-versions 5, :downloads 0, :users 5}}
           (meuse-api! (add-auth {:action :statistics})))))
  (testing "success - read only"
    (is (= {:status 200
            :body {:crates 3, :crates-versions 5, :downloads 0, :users 5}}
           (meuse-api! (add-auth {:action :statistics} "user-ro" "read-only")))))
  (testing "bad permissions"
    (is (thrown-with-msg?
         ExceptionInfo
         #"bad permissions"
         (meuse-api! {:action :statistics})))))
