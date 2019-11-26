(ns meuse.api.crate.crate-test
  (:require [meuse.api.crate.http :refer :all]
            [meuse.api.crate.new :refer :all]
            [meuse.api.crate.yank :refer :all]
            [meuse.db.actions.crate :as crate-db]
            [meuse.db.actions.user :as user-db]
            [meuse.message :refer [publish-commit-msg
                                   yank-commit-msg]]
            [meuse.db :refer [database]]
            [meuse.crate-test :refer [create-publish-request]]
            [meuse.git :refer [git]]
            [meuse.helpers.db-state :as db-state]
            meuse.helpers.git
            [meuse.helpers.files :refer :all]
            [meuse.helpers.fixtures :refer :all]
            [meuse.metadata :as metadata]
            [clojure.test :refer :all]
            [cheshire.core :as json]
            [meuse.crate :as crate])
  (:import clojure.lang.ExceptionInfo))

(use-fixtures :once system-fixture)
(use-fixtures :each tmp-fixture db-clean-fixture table-fixture)

(deftest crates-api-new-test
  (let [user-id (:users/id (user-db/by-name database "user2"))
        user-id-3 (:users/id (user-db/by-name database "user3"))]
    (testing "no deps"
      (let [name "toto"
            version "1.0.1"
            metadata {:name name :vers version :yanked false}
            crate-file "random content"
            git-actions (:state git)
            request (merge
                     (create-publish-request metadata crate-file)
                     {:registry-config {:allowed-registries ["default"]}
                      :action :new
                      :auth {:user-id user-id
                             :role-name "tech"}
                      :config {:crate {:path tmp-dir}
                               :metadata {:path tmp-dir}}})]
        (= (crates-api! request)
           {:status 200
            :body {:warning {:invalid_categories []
                             :invalid_badges []
                             :other []}}})
        (is (= [{:cmd "add"}
                {:cmd "commit"
                 :args (publish-commit-msg metadata)}
                {:cmd "push"}]
               @git-actions))
        (is (= (slurp (str tmp-dir "/toto/1.0.1/download"))
               crate-file))
        (db-state/test-crate-version database {:crates/name "toto"
                                               :crates_versions/version "1.0.1"
                                               :crates_versions/yanked false
                                               :crates_versions/description nil})
        (is (thrown-with-msg? ExceptionInfo
                              #"already exists$"
                              (crates-api! request)))))
    (testing "allowed deps"
      (let [name "toto"
            version "1.0.2"
            metadata {:name name
                      :vers version
                      :yanked false
                      :deps [{:name "bar"
                              :version_req "^1.0.3"
                              :registry "default"}]}
            crate-file "random content"
            git-actions (:state git)
            request (merge
                     (create-publish-request metadata crate-file)
                     {:auth {:user-id user-id
                             :role-name "tech"}
                      :registry-config {:allowed-registries ["default"]}
                      :action :new
                      :config {:crate {:path tmp-dir}
                               :metadata {:path tmp-dir}}})]
        (= (crates-api! request)
           {:status 200
            :body {:warning {:invalid_categories []
                             :invalid_badges []
                             :other []}}})))
    (testing "not allowed deps"
      (let [name "toto"
            version "1.0.3"
            metadata {:name name
                      :vers version
                      :yanked false
                      :deps [{:name "bar"
                              :version_req "^1.0.3"
                              :registry "default"}]}
            crate-file "random content"
            git-actions (:state git)
            request (merge
                     (create-publish-request metadata crate-file)
                     {:auth {:user-id user-id
                             :role-name "tech"}
                      :registry-config {:allowed-registries ["another"]}
                      :action :new
                      :config {:crate {:path tmp-dir}
                               :metadata {:path tmp-dir}}})]
        (is (thrown-with-msg?
             ExceptionInfo
             #"the registry default is not allowed"
             (crates-api! request)))))
    (testing "does not own the crate"
      (let [name "toto"
            version "1.0.10"
            metadata {:name name :vers version :yanked false}
            crate-file "random content"
            git-actions (:state git)
            request (merge
                     (create-publish-request metadata crate-file)
                     {:registry-config {:allowed-registries ["default"]}
                      :action :new
                      :auth {:user-id user-id-3
                             :role-name "tech"}
                      :config {:crate {:path tmp-dir}
                               :metadata {:path tmp-dir}}})]
        (is (thrown-with-msg? ExceptionInfo
                              #"the user does not own the crate"
                              (crates-api! request)))))
    (testing "invalid parameters"
      (let [name "toto"
            version "1.0.3"
            metadata {:name name
                      :vers version
                      :yanked false
                      :deps [{:name "foo"}]}
            crate-file "random content"
            request (merge
                     (create-publish-request metadata crate-file)
                     {:action :new})]
        (is (thrown-with-msg?
             ExceptionInfo
             #"Wrong input parameters:\n - field version_req missing in deps\n"
             (crates-api! request))))
      (let [name "toto"
            version "1.0.3"
            metadata {:name ""
                      :vers version
                      :yanked false}
            crate-file "random content"
            request (merge
                     (create-publish-request metadata crate-file)
                     {:action :new})]
        (is (thrown-with-msg?
             ExceptionInfo
             #"Wrong input parameters:\n - field name: the value should be a non empty string\n"
             (crates-api! request))))
      (let [name "toto"
            version "aaa"
            metadata {:name ""
                      :vers version
                      :yanked false}
            crate-file "random content"
            request (merge
                     (create-publish-request metadata crate-file)
                     {:action :new})]
        (is (thrown-with-msg?
             ExceptionInfo
             #"Wrong input parameters:\n - field name: the value should be a non empty string\n - field vers: the value should be a valid semver string\n"
             (crates-api! request)))))
    (testing "invalid role"
      (let [name "toto"
            version "1.0.1"
            metadata {:name "aaa"
                      :vers version
                      :yanked false}
            crate-file "random content"
            request (merge
                     (create-publish-request metadata crate-file)
                     {:action :new
                      :auth {:role-name "lol"}})]
        (is (thrown-with-msg?
             ExceptionInfo
             #"bad permissions"
             (crates-api! request)))))))

(deftest crates-api-yank-unyank-test
  (testing "success:admin"
    (let [user5-id (:users/id (user-db/by-name database "user5"))
          request {:action :yank
                   :auth {:user-id user5-id
                          :role-name "admin"}
                   :config {:crate {:path tmp-dir}
                            :metadata {:path tmp-dir}}
                   :route-params {:crate-name "crate1"
                                  :crate-version "1.1.0"}}]
      (metadata/write-metadata tmp-dir {:name "crate1" :vers "1.1.0" :yanked false})
      (crates-api! request)
      (db-state/test-crate-version database {:crates/name "crate1"
                                             :crates_versions/version "1.1.0"
                                             :crates_versions/yanked true
                                             :crates_versions/description "the crate1 description, this crate is for foobar"})
      (is (thrown-with-msg? ExceptionInfo
                            #"crate state is already yank$"
                            (crates-api! (assoc request :action :yank))))
      (crates-api! (assoc request :action :unyank))
      (is (thrown-with-msg? ExceptionInfo
                            #"crate state is already unyank$"
                            (crates-api! (assoc request :action :unyank))))
      (db-state/test-crate-version database {:crates/name "crate1"
                                             :crates_versions/version "1.1.0"
                                             :crates_versions/yanked false
                                             :crates_versions/description "the crate1 description, this crate is for foobar"})))
  (testing "success: the user owns the crate"
    (let [user2-id (:users/id (user-db/by-name database "user2"))
          request {:action :yank
                   :auth {:user-id user2-id
                          :role-name "tech"}
                   :config {:crate {:path tmp-dir}
                            :metadata {:path tmp-dir}}
                   :route-params {:crate-name "crate1"
                                  :crate-version "1.1.0"}}]
      (metadata/write-metadata tmp-dir {:name "crate1" :vers "1.1.0" :yanked false})
      (crates-api! request)
      (db-state/test-crate-version database {:crates/name "crate1"
                                             :crates_versions/version "1.1.0"
                                             :crates_versions/yanked true
                                             :crates_versions/description "the crate1 description, this crate is for foobar"})
      (is (thrown-with-msg? ExceptionInfo
                            #"crate state is already yank$"
                            (crates-api! (assoc request :action :yank))))
      (crates-api! (assoc request :action :unyank))
      (is (thrown-with-msg? ExceptionInfo
                            #"crate state is already unyank$"
                            (crates-api! (assoc request :action :unyank))))
      (db-state/test-crate-version database {:crates/name "crate1"
                                             :crates_versions/version "1.1.0"
                                             :crates_versions/yanked false
                                             :crates_versions/description "the crate1 description, this crate is for foobar"})))
  (testing "invalid parameters"
    (is (thrown-with-msg?
         ExceptionInfo
         #"Wrong input parameters:\n - field crate-version missing in route-params\n"
         (crates-api! {:route-params {:crate-name "crate1"}
                       :action :yank}))))
  (testing "invalid parameters"
    (is (thrown-with-msg?
         ExceptionInfo
         #"Wrong input parameters:\n - field crate-version: the value should be a valid semver string\n"
         (crates-api! {:route-params {:crate-name "crate1"
                                      :crate-version "1.1"}
                       :action :yank})))))

(deftest default-not-found-test
  (is (= (meuse.api.default/not-found {})
         (crates-api! {:action :random}))))
