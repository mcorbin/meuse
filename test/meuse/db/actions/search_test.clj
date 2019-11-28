(ns meuse.db.actions.search-test
  (:require [meuse.db :refer [database]]
            [meuse.db.actions.search :refer :all]
            [meuse.helpers.fixtures :refer :all]
            [clojure.test :refer :all]))

(use-fixtures :once system-fixture)
(use-fixtures :each db-clean-fixture table-fixture)

(deftest format-query-string-test
  (is (= (format-query-string "foo bar baz")
         "foo | bar | baz"))
  (is (= (format-query-string "foo")
         "foo")))

(deftest search-test
  (let [result (search database "crate1")]
    (is (= 3 (count result)))
    (mapv #(is (= "crate1" (:crates/name %))) result))
  (let [result (search database "barbaz")]
    (is (= 1 (count result))))
  (is (empty? (search database "arandomstring")))
  (testing "search by category"
    (let [result (search database "system")]
      (is (= 3 (count result)))))
  (testing "search by keyword"
    (let [result (search database "keyword1")]
      (is (= 1 (count result))))))
