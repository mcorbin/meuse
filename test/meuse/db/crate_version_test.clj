(ns meuse.db.crate-version-test
  (:require [meuse.db.crate-version :refer :all]
            [meuse.helpers.db-state :as db-state]
            [meuse.helpers.fixtures :refer :all]
            [clojure.test :refer :all]
            [meuse.db :refer [database]])
  (:import clojure.lang.ExceptionInfo
           java.util.UUID))

(use-fixtures :once db-fixture)
(use-fixtures :each table-fixture)

(deftest ^:integration update-yank-test
  (testing "success"
    (update-yank database "crate1" "1.1.0" true)
    (db-state/test-crate-version database
                                 {:crate-name "crate1"
                                  :version-version "1.1.0"
                                  :version-yanked true
                                  :version-description "the crate1 description, this crate is for foobar"})
    (update-yank database "crate1" "1.1.0" false)
    (db-state/test-crate-version database
                                 {:crate-name "crate1"
                                  :version-version "1.1.0"
                                  :version-yanked false
                                  :version-description "the crate1 description, this crate is for foobar"}))
  (testing "error"
    (is (thrown-with-msg? ExceptionInfo
                          #"the crate does not exist$"
                          (update-yank database "doesnotexist" "0.1.3" false)))
    (is (thrown-with-msg? ExceptionInfo
                          #"the version does not exist$"
                          (update-yank database "crate1" "0.1.4" false)))))
