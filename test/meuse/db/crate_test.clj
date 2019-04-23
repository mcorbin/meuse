(ns meuse.db.crate-test
  (:require [meuse.config :refer [config]]
            [meuse.db :refer [database]]
            [meuse.db.crate :refer :all]
            [mount.core :as mount]
            [clojure.java.jdbc :as jdbc]
            [clojure.test :refer :all])
  (:import clojure.lang.ExceptionInfo))

(defn db-fixture
  [f]
  (mount/start #'meuse.config/config #'meuse.db/database)
  (f)
  (mount/stop #'meuse.config/config #'meuse.db/database))

(defn table-fixture
  [f]
  (jdbc/execute! database ["TRUNCATE TABLE CRATES CASCADE;"])
  (f))

(use-fixtures :once db-fixture)
(use-fixtures :each table-fixture)

(deftest ^:integration new-crate-test
  (let [request {:database database}
        crate {:metadata {:name "test1"
                          :vers "0.1.3"
                          :yanked false}}]
    (new-crate request crate)
    (is (thrown-with-msg? ExceptionInfo
                          #"already exists$"
                          (new-crate request crate)))
    (let [crate-db (get-crate-version database "test1" "0.1.3")]
      (is (uuid? (:crate-id crate-db)))
      (is (uuid? (:version-id crate-db)))
      (is (inst? (:version-created-at crate-db)))
      (is (inst? (:version-updated-at crate-db)))
      (are [x y] (= x y)
        "test1" (:crate-name crate-db)
        "0.1.3" (:version-version crate-db)
        false (:version-yanked crate-db)
        nil (:version-description crate-db)
        (:crate-id crate-db) (:version-crate-id crate-db)))
    (new-crate request (assoc-in crate [:metadata :vers] "2.0.0"))
    (let [crate-db (get-crate-version database "test1" "2.0.0")]
      (is (uuid? (:crate-id crate-db)))
      (is (uuid? (:version-id crate-db)))
      (is (inst? (:version-created-at crate-db)))
      (is (inst? (:version-updated-at crate-db)))
      (are [x y] (= x y)
        "test1" (:crate-name crate-db)
        "2.0.0" (:version-version crate-db)
        false (:version-yanked crate-db)
        nil (:version-description crate-db)
        (:crate-id crate-db) (:version-crate-id crate-db)))))
