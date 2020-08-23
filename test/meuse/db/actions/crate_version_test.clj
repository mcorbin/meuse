(ns meuse.db.actions.crate-version-test
  (:require [meuse.db.actions.crate-version :refer :all]
            [meuse.db.actions.crate :as crate-db]
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
                                 {:crates/name "crate1"
                                  :crates_versions/version "1.1.0"
                                  :crates_versions/yanked true
                                  :crates_versions/description "the crate1 description, this crate is for foobar"})
    (update-yank database "crate1" "1.1.0" false)
    (db-state/test-crate-version database
                                 {:crates/name "crate1"
                                  :crates_versions/version "1.1.0"
                                  :crates_versions/yanked false
                                  :crates_versions/description "the crate1 description, this crate is for foobar"}))
  (testing "error"
    (is (thrown-with-msg? ExceptionInfo
                          #"the crate does not exist$"
                          (update-yank database "doesnotexist" "0.1.3" false)))
    (is (thrown-with-msg? ExceptionInfo
                          #"the version does not exist$"
                          (update-yank database "crate1" "0.1.4" false)))))

(deftest delete-test
  (testing "success"
    (is (crate-db/by-name-and-version database "crate1" "1.1.0"))
    (delete database "crate1" "1.1.0")
    (is (nil? (crate-db/by-name-and-version database "crate1" "1.1.0")))))

(deftest last-updated-test
  (let [crates (last-updated database 1)]
    (is (= 1 (count crates)))
    (is (= "crate3" (-> crates first :crates/name))))
  (let [crates (last-updated database 2)]
    (is (= 2 (count crates)))
    (is (= "crate3" (-> crates first :crates/name)))
    (is (= "crate2" (-> crates second :crates/name)))))

(deftest count-crates-versions-test
  (is (= {:count 5} (count-crates-versions database))))

(deftest inc-download-test
  (inc-download database "crate1" "1.1.0")
  (is (= 1 (:crates_versions/download_count (crate-db/by-name-and-version
                                             database
                                             "crate1"
                                             "1.1.0"))))
  (inc-download database "crate1" "1.1.0")
  (is (= 2 (:crates_versions/download_count (crate-db/by-name-and-version
                                             database
                                             "crate1"
                                             "1.1.0"))))
  (is (thrown-with-msg? ExceptionInfo
                        #"crate doesnotexist version 1.1.0 not found"
                        (inc-download database "doesnotexist" "1.1.0"))))

(deftest sum-download-count-test
  (is (= {:sum 0} (sum-download-count database)))
  (inc-download database "crate1" "1.1.0")
  (is (= {:sum 1} (sum-download-count database)))
  (inc-download database "crate1" "1.1.0")
  (inc-download database "crate1" "1.1.4")
  (inc-download database "crate1" "1.1.4")
  (is (= {:sum 4} (sum-download-count database))))

(deftest top-n-downloads-test
  (inc-download database "crate1" "1.1.0")
  (is (= [{:crates/name "crate1"
           :crates_versions/version "1.1.0"
           :crates_versions/download_count 1}]
         (->> (top-n-downloads database 1)
              (map #(select-keys % [:crates/name
                                    :crates_versions/version
                                    :crates_versions/download_count])))))
  (inc-download database "crate1" "1.1.4")
  (inc-download database "crate1" "1.1.4")
  (is (= [{:crates/name "crate1"
           :crates_versions/version "1.1.4"
           :crates_versions/download_count 2}]
         (->> (top-n-downloads database 1)
              (map #(select-keys % [:crates/name
                                    :crates_versions/version
                                    :crates_versions/download_count])))))
  (is (= [{:crates/name "crate1"
           :crates_versions/version "1.1.4"
           :crates_versions/download_count 2}
          {:crates/name "crate1"
           :crates_versions/version "1.1.0"
           :crates_versions/download_count 1}]
         (->> (top-n-downloads database 2)
              (map #(select-keys % [:crates/name
                                    :crates_versions/version
                                    :crates_versions/download_count]))))))

