(ns meuse.db.crate-test
  (:require [meuse.db :refer [database]]
            [meuse.db.crate :refer :all]
            [meuse.helpers.db :refer :all]
            [meuse.helpers.fixtures :refer :all]
            [clojure.test :refer :all])
  (:import clojure.lang.ExceptionInfo))

(use-fixtures :once db-fixture)
(use-fixtures :each table-fixture)

(deftest ^:integration new-crate-test
  (let [crate {:metadata {:name "test1"
                          :vers "0.1.3"
                          :yanked false}}]
    (new-crate database crate)
    (is (thrown-with-msg? ExceptionInfo
                          #"already exists$"
                          (new-crate database crate)))
    (test-db-state database {:crate-name "test1"
                             :version-version "0.1.3"
                             :version-yanked false
                             :version-description nil})
    (new-crate database (assoc-in crate [:metadata :vers] "2.0.0"))
    (test-db-state database {:crate-name "test1"
                             :version-version "2.0.0"
                             :version-yanked false
                             :version-description nil})))

(deftest ^:integration update-yank-test
  (testing "success"
    (let [crate {:metadata {:name "test1"
                            :vers "0.1.3"
                            :yanked false}}]
      (new-crate database crate)
      (test-db-state database {:crate-name "test1"
                               :version-version "0.1.3"
                               :version-yanked false
                               :version-description nil})
      (update-yank database "test1" "0.1.3" true)
      (test-db-state database {:crate-name "test1"
                               :version-version "0.1.3"
                               :version-yanked true
                               :version-description nil})
      (update-yank database "test1" "0.1.3" false)
      (test-db-state database {:crate-name "test1"
                               :version-version "0.1.3"
                               :version-yanked false
                               :version-description nil})))
  (testing "error"
    (let [crate {:metadata {:name "test3"
                            :vers "0.1.3"
                            :yanked false}}]
      (is (thrown-with-msg? ExceptionInfo
                            #"the crate does not exist$"
                            (update-yank database "test3" "0.1.3" false)))
      (new-crate database crate)
      (is (thrown-with-msg? ExceptionInfo
                            #"the version does not exist$"
                            (update-yank database "test3" "0.1.4" false))))))
