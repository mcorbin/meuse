(ns meuse.request-test
  (:require [meuse.request :refer :all]
            [clojure.test :refer :all])
  (:import clojure.lang.ExceptionInfo))

(deftest convert-body-edn-test
  (is (= {:body {:foo "bar"}}
         (convert-body-edn {:body "{\"foo\":\"bar\"}"})))
  (is (= {:body {:foo "bar"}}
         (convert-body-edn {:body (.getBytes "{\"foo\":\"bar\"}")})))
  (is (= {:body nil}
         (convert-body-edn {:body nil})))
  (is (thrown-with-msg?
       ExceptionInfo
       #"fail to convert the request body to json"
       (convert-body-edn {:body "{\"foo\":\"bar\""}))))
