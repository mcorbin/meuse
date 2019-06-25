(ns meuse.config-test
  (:require [meuse.config :refer :all]
            [clojure.test :refer :all]
            [spy.assert :as assert]
            [spy.core :as spy]))

(deftest test-load-config
  (with-redefs [stop! (spy/stub-throws (ex-info "exit" {}))]
    (try
      (load-config "/this/path/does/not/exist")
      (is (= 1 2))
      (catch Exception _))
    (assert/called? stop!)))
