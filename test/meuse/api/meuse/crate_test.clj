(ns meuse.api.meuse.crate-test
  (:require [meuse.api.meuse.crate :refer :all]
            [meuse.api.meuse.http :refer :all]
            [meuse.db.actions.crate :as crate-db]
            [meuse.db :refer [database]]
            [meuse.helpers.fixtures :refer :all]
            [meuse.helpers.request :refer [add-auth]]
            [clojure.test :refer :all])
  (:import clojure.lang.ExceptionInfo
           java.util.Date
           java.util.UUID))

(use-fixtures :once system-fixture)
(use-fixtures :each db-clean-fixture table-fixture)

(def crate-id-1 (UUID/randomUUID))
(def crate-id-2 (UUID/randomUUID))
(def date-1 (new Date))
(def date-2 (new Date))

(deftest format-crate-test
  (is (= {:id crate-id-1
          :name "foo"
          :versions [{:version "1.1.0"
                      :yanked false
                      :description "desc1"
                      :created-at date-1
                      :updated-at date-2}]}
         (format-crate [crate-id-1
                        [{:crate-id crate-id-1
                          :crate-name "foo"
                          :version-description "desc1"
                          :version-version "1.1.0"
                          :version-yanked false
                          :version-created-at date-1
                          :version-updated-at date-2
                          :foo "bar"}]])))
  (is (= {:id crate-id-1
          :name "foo"
          :versions [{:version "1.1.0"
                      :yanked false
                      :description "desc1"
                      :created-at date-1
                      :updated-at date-2}
                     {:version "1.1.8"
                      :yanked false
                      :description "desc2"
                      :created-at date-1
                      :updated-at date-2}]}
         (format-crate [crate-id-1
                        [{:crate-id crate-id-1
                          :crate-name "foo"
                          :version-description "desc1"
                          :version-version "1.1.0"
                          :version-yanked false
                          :version-created-at date-1
                          :version-updated-at date-2
                          :foo "bar"}
                         {:crate-id crate-id-1
                          :version-description "desc2"
                          :crate-name "foo"
                          :version-version "1.1.8"
                          :version-yanked false
                          :version-created-at date-1
                          :version-updated-at date-2}]]))))

(deftest format-crates-test
  (is (= [{:id crate-id-1
           :name "foo"
           :versions [{:version "1.1.0"
                       :yanked false
                       :description "desc1"
                       :created-at date-1
                       :updated-at date-2}]}]
         (format-crates [{:crate-id crate-id-1
                          :crate-name "foo"
                          :version-description "desc1"
                          :version-version "1.1.0"
                          :version-yanked false
                          :version-created-at date-1
                          :version-updated-at date-2
                          :foo "bar"}])))
  (is (= [{:id crate-id-1
           :name "foo"
           :versions [{:version "1.1.0"
                       :yanked false
                       :description "desc1"
                       :created-at date-1
                       :updated-at date-2}
                      {:version "1.1.8"
                       :yanked false
                       :description "desc2"
                       :created-at date-1
                       :updated-at date-2}]}]
         (format-crates [{:crate-id crate-id-1
                          :crate-name "foo"
                          :version-description "desc1"
                          :version-version "1.1.0"
                          :version-yanked false
                          :version-created-at date-1
                          :version-updated-at date-2
                          :foo "bar"}
                         {:crate-id crate-id-1
                          :version-description "desc2"
                          :crate-name "foo"
                          :version-version "1.1.8"
                          :version-yanked false
                          :version-created-at date-1
                          :version-updated-at date-2}])))
  (is (= [{:id crate-id-1
           :name "foo"
           :versions [{:version "1.1.0"
                       :yanked false
                       :description "desc1"
                       :created-at date-1
                       :updated-at date-2}
                      {:version "1.1.8"
                       :yanked false
                       :description "desc2"
                       :created-at date-1
                       :updated-at date-2}]}
          {:id crate-id-2
           :name "bar"
           :versions [{:version "3.0.0"
                       :yanked true
                       :description "desc3"
                       :created-at date-1
                       :updated-at date-2}]}]
         (format-crates [{:crate-id crate-id-1
                          :crate-name "foo"
                          :version-description "desc1"
                          :version-version "1.1.0"
                          :version-yanked false
                          :version-created-at date-1
                          :version-updated-at date-2
                          :foo "bar"}
                         {:crate-id crate-id-1
                          :version-description "desc2"
                          :crate-name "foo"
                          :version-version "1.1.8"
                          :version-yanked false
                          :version-created-at date-1
                          :version-updated-at date-2}
                         {:crate-id crate-id-2
                          :version-description "desc3"
                          :crate-name "bar"
                          :version-version "3.0.0"
                          :version-yanked true
                          :version-created-at date-1
                          :version-updated-at date-2}]))))

(deftest list-crates-test
  (testing "success"
    (let [{:keys [status body]} (meuse-api! (add-auth {:action :list-crates}
                                                      "user1"
                                                      "tech"))
          crates (:crates body)
          crate1 (first (filter #(= (:name %) "crate1") crates))
          crate2 (first (filter #(= (:name %) "crate2") crates))
          crate3 (first (filter #(= (:name %) "crate3") crates))]
      (is (= 200 status))
      (is (= (count crates) 3))
      (is (= 3 (count (:versions crate1))))
      (is (= 1 (count (:versions crate2))))
      (is (= 1 (count (:versions crate3))))))
  (testing "list for a category"
    (let [{:keys [status body]} (meuse-api! (add-auth {:params {:category "email"}
                                                       :action :list-crates}
                                                      "user1"
                                                      "tech"))
          crates (:crates body)
          crate1 (first crates)]
      (is (= 200 status))
      (is (= "crate1" (:name crate1)))
      (is (uuid? (:id crate1)))
      (is (= 1 (count crates)))
      (is (= 3 (count (:versions crate1))))))
  (testing "bad permissions"
    (is (thrown-with-msg?
         ExceptionInfo
         #"bad permissions"
         (meuse-api! {:action :list-crates}))))
  (testing "bad parameters"
    (is (thrown-with-msg?
         ExceptionInfo
         #"Wrong input parameters:\n - field category: the value should be a non empty string\n"
         (meuse-api! {:action :list-crates
                      :params {:category 1}})))))

(deftest get-crate-test
  (testing "success"
    (let [{:keys [status body]} (meuse-api! (add-auth {:route-params {:name "crate1"}
                                                       :action :get-crate}
                                                      "user1"
                                                      "tech"))]
      (is (= 200 status))
      (is (= (count (:versions body)) 3))
      (is (= 3 (count (:versions body))))
      (is (= #{"email" "system"} (set (map :name (:categories body)))))
      (is (= #{"the email category" "the system category"}
             (set (map :description (:categories body)))))
      (mapv #(is (uuid? (:id %))) (:categories body))))
  (testing "bad parameters"
    (is (thrown-with-msg?
         ExceptionInfo
         #"Wrong input parameters:\n - field name: the value should be a non empty string\n"
         (meuse-api! {:action :get-crate
                      :route-params {:name 1}}))))
  (testing "bad permissions"
    (is (thrown-with-msg?
         ExceptionInfo
         #"bad permissions"
         (meuse-api! {:action :get-crate
                      :route-params {:name "crate1"}})))))

(deftest check-crates-test
  (testing "success"
    (let [result (meuse-api! (add-auth {:action :check-crates
                                        :git {:lock "foo"}}
                                       "user1"
                                       "admin"))]
      (is (= 3 (count (:body result))))
      (is (= 200 (:status result)))))
  (testing "bad permissions"
    (is (thrown-with-msg?
         ExceptionInfo
         #"bad permissions"
         (meuse-api! {:action :check-crates})))))
