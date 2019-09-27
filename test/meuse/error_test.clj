(ns meuse.error-test
  (:require [meuse.error :refer :all]
            [clojure.test :refer :all]))

(deftest handle-unexpected-error-test
  (is (= {:status 500
          :body {:errors [{:detail default-msg}]}}
         (handle-unexpected-error
          {:subsystem :meuse.api.crate.http}
          (ex-info default-msg {})))))
