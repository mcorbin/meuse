(ns meuse.api.crate-test
  (:require [meuse.api.crate :refer :all]
            [meuse.message :refer [publish-commit-msg]]
            [meuse.db :refer [database]]
            [meuse.crate-test :refer [create-publish-request]]
            [meuse.fixtures :refer :all]
            [meuse.git :refer [Git]]
            [clojure.test :refer :all]
            [cheshire.core :as json]))

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
  (add-crate [this crate]
    (swap! state conj {:cmd "add-crate"
                       :args [(update crate :crate-file #(String. %))]}))
  (commit [this msg-header msg-body]
    (swap! state conj {:cmd "commit"
                       :args [msg-header msg-body]}))
  (push [this]
    (swap! state conj {:cmd "push"}))
  (pull [this]
    (swap! state conj {:cmd "pull"}))
  (update-yank [this crate-name crate-version yanked]
    (swap! state conj {:cmd "update-yank"
                       :args [crate-name crate-version yanked]})))

(deftest crates-api-new-test
  (let [name "toto"
        version "1.0.1"
        metadata {:name name :vers version :yanked false}
        crate-file "random content"
        git-actions (atom [])
        request (merge
                 (create-publish-request metadata crate-file)
                 {:git (GitMock. git-actions)
                  :action :new
                  :crate-config {:path tmp-dir}
                  :database database})]
    (crates-api! request)
    (is (= @git-actions [{:cmd "add-crate"
                          :args [{:metadata metadata
                                   :crate-file crate-file}]}
                         {:cmd "add"}
                         {:cmd "commit"
                          :args (publish-commit-msg {:metadata metadata})}
                         {:cmd "push"}]))
    (is (= (slurp (str tmp-dir "/toto/1.0.1/download"))
           crate-file))))




