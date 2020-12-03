(ns meuse.api.crate-test
  (:require [clojure.test :refer :all]
            [meuse.api.crate :as mac]))

(deftest crate-regex-test
  (are [v] (re-matches mac/crate-regex v)
    "foo"
    "foo-bar"
    "foo_bar"
    "foo-1.1.2"
    "1.1.2"
    "1.1.2-alpha"
    "1.1.2_alpha"
    "1.1.2+3.4.5"
    "1.1.2~foo"
    "1.1.2~_-+134.1")
  (are [v] (not (re-matches mac/crate-regex v))
    "foo bar"
    "foo%bar"
    "foo/bar"
    "foo\\bar"))
