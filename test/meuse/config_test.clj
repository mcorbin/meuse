(ns meuse.config-test
  (:require [meuse.config :refer :all]
            [clojure.test :refer :all]
            [spy.assert :as assert]
            [spy.core :as spy]))

(deftest test-load-config
  (with-redefs [stop! (spy/spy)]
    (load-config "/this/path/does/not/exist")
    (assert/called? stop!)))
