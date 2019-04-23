(ns meuse.middleware-test
  (:require [meuse.middleware :refer :all]
            [clojure.test :refer :all]))

(deftest wrap-json-test
  (is (= {:body "blablabla"}
         @((wrap-json identity) {:body "blablabla"})))
  (is (= {:body "{\"foo\":\"bar\"}"
          :headers {:content-type "application/json"}}
         @((wrap-json identity) {:body {:foo "bar"}}))))
