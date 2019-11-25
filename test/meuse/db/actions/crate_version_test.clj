(ns meuse.db.actions.crate-version-test
  (:require [meuse.db.actions.crate-version :refer :all]
            [meuse.helpers.db-state :as db-state]
            [meuse.helpers.fixtures :refer :all]
            [clojure.test :refer :all]
            [meuse.db :refer [database]])
  (:import clojure.lang.ExceptionInfo
           java.util.UUID))

(use-fixtures :once system-fixture)
(use-fixtures :each db-clean-fixture table-fixture)

(deftest update-yank-test
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

(deftest last-updated-test
  (let [crates (last-updated database 1)]
    (is (= 1 (count crates)))
    (is (= "crate3" (-> crates first :crate-name))))
  (let [crates (last-updated database 2)]
    (is (= 2 (count crates)))
    (is (= "crate3" (-> crates first :crate-name)))
    (is (= "crate2" (-> crates second :crate-name)))))

(deftest count-crates-versions-test
  (is (= {:crates-versions-count 5} (count-crates-versions database))))
