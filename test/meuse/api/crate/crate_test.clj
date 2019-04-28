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
            [meuse.helpers.fixtures :refer :all]
            [meuse.helpers.files :refer :all]
            [meuse.git :refer [Git]]
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

(deftest ^:integration crates-api-new-and-yank-test
  (let [name "toto"
        version "1.0.1"
        metadata {:name name :vers version :yanked false}
        crate-file "random content"
        git-actions (atom [])
        request (merge
                 (create-publish-request metadata crate-file)
                 {:git (GitMock. git-actions)
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
                          :args (publish-commit-msg {:metadata metadata})}
                         {:cmd "push"}]))
    (is (= (slurp (str tmp-dir "/toto/1.0.1/download"))
           crate-file))
    (test-crate-version database {:crate-name "toto"
                                  :version-version "1.0.1"
                                  :version-yanked false
                                  :version-description nil})
    (is (thrown-with-msg? ExceptionInfo
                          #"already exists$"
                          (crates-api! request)))
    (let [yank-request (assoc request
                              :route-params {:crate-name "toto"
                                             :crate-version "1.0.1"}
                              :action :yank)]
      (crates-api! yank-request)
      (test-crate-version database {:crate-name "toto"
                                    :version-version "1.0.1"
                                    :version-yanked true
                                    :version-description nil})
      (is (thrown-with-msg? ExceptionInfo
                            #"crate state is already yank$"
                            (crates-api! (assoc yank-request :action :yank))))
      (crates-api! (assoc yank-request :action :unyank))
      (is (thrown-with-msg? ExceptionInfo
                            #"crate state is already unyank$"
                            (crates-api! (assoc yank-request :action :unyank))))
      (test-crate-version database {:crate-name "toto"
                                    :version-version "1.0.1"
                                    :version-yanked false
                                    :version-description nil}))))

(deftest default-not-found-test
  (is (= meuse.api.default/not-found
         (crates-api! {:action :random}))))
