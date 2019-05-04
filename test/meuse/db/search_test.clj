(ns meuse.db.search-test
  (:require [meuse.db :refer [database]]
            [meuse.db.search :refer :all]
            [meuse.helpers.fixtures :refer :all]
            [clojure.test :refer :all]))

(use-fixtures :once db-fixture)
(use-fixtures :each table-fixture)

(deftest format-query-string-test
  (is (= (format-query-string "foo bar baz")
         "foo | bar | baz"))
  (is (= (format-query-string "foo")
         "foo")))

(deftest search-test
  (let [result (search database "crate1")]
    (is (= 3 (count result)))
    (map #(is (= "crate1" (:crate-name %)) result)))
  (let [result (search database "barbaz")]
    (is (= 1 (count result)))
    (map (= "crate2" (:crate-name (first result)))))
  (is (empty? (search database "arandomstring"))))
