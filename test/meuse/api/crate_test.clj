(ns meuse.api.crate-test
  (:require [meuse.api.crate :refer :all]
            [meuse.git :refer [Git]]
            [clojure.test :refer :all]))

(defrecord GitMock [state]
  Git
  (add [this]
    (swap! state conj {:cmd "add"}))
  (git-cmd [this args]
    (swap! state conj {:cmd "git-cmd"
                       :args [args]}))
  (add-crate [this crate]
    (swap! state conj {:cmd "add-crate"
                       :args [crate]}))
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

