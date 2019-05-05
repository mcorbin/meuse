(ns meuse.api.crate.crate-test
  (:require [meuse.api.crate.http :refer :all]
            [meuse.api.crate.new :refer :all]
            [meuse.api.crate.yank :refer :all]
            [meuse.db.crate :as crate-db]
            [meuse.message :refer [publish-commit-msg
                                   yank-commit-msg]]
            [meuse.db :refer [database]]
            [meuse.crate-test :refer [create-publish-request]]
            [meuse.helpers.db :refer :all]
            [meuse.helpers.files :refer :all]
            [meuse.helpers.fixtures :refer :all]
            [meuse.git :refer [Git]]
            [meuse.metadata :as metadata]
            [clojure.test :refer :all]
            [cheshire.core :as json]
            [meuse.db.crate :as crate-db]
            [meuse.crate :as crate])
  (:import clojure.lang.ExceptionInfo))

(use-fixtures :each tmp-fixture)

(use-fixtures :once db-fixture)
(use-fixtures :each table-fixture)

(defrecord GitMock [state]
  Git
  (add [this]
    (swap! state conj {:cmd "add"}))
  (git-cmd [this args]
    (swap! state conj {:cmd "git-cmd"
                       :args [args]}))
  (commit [this msg-header msg-body]
    (swap! state conj {:cmd "commit"
                       :args [msg-header msg-body]}))
  (push [this]
    (swap! state conj {:cmd "push"}))
  (pull [this]
    (swap! state conj {:cmd "pull"})))

(deftest ^:integration crates-api-new-test
  (testing "no deps"
    (let [name "toto"
          version "1.0.1"
          metadata {:name name :vers version :yanked false}
          crate-file "random content"
          git-actions (atom [])
          request (merge
                   (create-publish-request metadata crate-file)
                   {:git (GitMock. git-actions)
                    :registry-config {:allowed-registries ["default"]}
                    :action :new
                    :config {:crate {:path tmp-dir}
                             :metadata {:path tmp-dir}}
                    :database database})]
      (= (crates-api! request)
         {:status 200
          :body {:warning {:invalid_categories []
                           :invalid_badges []
                           :other []}}})
      (is (= @git-actions [{:cmd "add"}
                           {:cmd "commit"
                            :args (publish-commit-msg metadata)}
                           {:cmd "push"}]))
      (is (= (slurp (str tmp-dir "/toto/1.0.1/download"))
             crate-file))
      (test-crate-version database {:crate-name "toto"
                                    :version-version "1.0.1"
                                    :version-yanked false
                                    :version-description nil})
      (is (thrown-with-msg? ExceptionInfo
                            #"already exists$"
                            (crates-api! request)))))
  (testing "allowed deps"
    (let [name "toto"
          version "1.0.2"
          metadata {:name name
                    :vers version
                    :yanked false
                    :deps [{:registry "default"}]}
          crate-file "random content"
          git-actions (atom [])
          request (merge
                   (create-publish-request metadata crate-file)
                   {:git (GitMock. git-actions)
                    :registry-config {:allowed-registries ["default"]}
                    :action :new
                    :config {:crate {:path tmp-dir}
                             :metadata {:path tmp-dir}}
                    :database database})]
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
                    :deps [{:registry "default"}]}
          crate-file "random content"
          git-actions (atom [])
          request (merge
                   (create-publish-request metadata crate-file)
                   {:git (GitMock. git-actions)
                    :registry-config {:allowed-registries ["another"]}
                    :action :new
                    :config {:crate {:path tmp-dir}
                             :metadata {:path tmp-dir}}
                    :database database})]
      (is (thrown-with-msg?
           ExceptionInfo
           #"the registry default is not allowed"
           (crates-api! request))))))

(deftest ^:integration crates-api-yank-unyank-test
  (let [git-actions (atom [])
        request {:git (GitMock. git-actions)
                 :database database
                 :action :yank
                 :config {:crate {:path tmp-dir}
                          :metadata {:path tmp-dir}}
                 :route-params {:crate-name "crate1"
                                :crate-version "1.1.0"}}]
    (metadata/write-metadata tmp-dir {:name "crate1" :vers "1.1.0" :yanked false})
    (crates-api! request)
    (test-crate-version database {:crate-name "crate1"
                                  :version-version "1.1.0"
                                  :version-yanked true
                                  :version-description "the crate1 description, this crate is for foobar"})
    (is (thrown-with-msg? ExceptionInfo
                          #"crate state is already yank$"
                          (crates-api! (assoc request :action :yank))))
    (crates-api! (assoc request :action :unyank))
    (is (thrown-with-msg? ExceptionInfo
                          #"crate state is already unyank$"
                          (crates-api! (assoc request :action :unyank))))
    (test-crate-version database {:crate-name "crate1"
                                  :version-version "1.1.0"
                                  :version-yanked false
                                  :version-description "the crate1 description, this crate is for foobar"})))

(deftest default-not-found-test
  (is (= meuse.api.default/not-found
         (crates-api! {:action :random}))))
