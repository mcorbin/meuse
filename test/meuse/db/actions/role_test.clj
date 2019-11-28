(ns meuse.db.actions.role-test
  (:require [meuse.db :refer [database]]
            [meuse.db.actions.role :refer :all]
            [meuse.helpers.fixtures :refer :all]
            [clojure.test :refer :all])
  (:import clojure.lang.ExceptionInfo))

(use-fixtures :once system-fixture)
(use-fixtures :each db-clean-fixture table-fixture)

(deftest get-role-by-name-test
  (let [role (by-name database "admin")]
    (is (uuid? (:roles/id role)))
    (is (= "admin" (:roles/name role))))
  (is (thrown-with-msg? ExceptionInfo
                        #"the role azerty does not exist"
                        (by-name database "azerty"))))

(deftest get-tech-role-name
  (let [role (get-tech-role database)]
    (is (uuid? (:roles/id role)))
    (is (= "tech" (:roles/name role)))))

(deftest get-admin-role-name
  (let [role (get-admin-role database)]
    (is (uuid? (:roles/id role)))
    (is (= "admin" (:roles/name role)))))
