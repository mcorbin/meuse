(ns meuse.helpers.git
  (:require [meuse.git :refer [IGit]]))

(defrecord GitMock [state lock]
  IGit
  (add [this]
    (swap! state conj {:cmd "add"}))
  (commit [this msg-header msg-body]
    (swap! state conj {:cmd "commit"
                       :args [msg-header msg-body]}))
  (get-lock [this]
    lock)
  (push [this]
    (swap! state conj {:cmd "push"}))
  (pull [this]
    (swap! state conj {:cmd "pull"}))
  (reset-hard [this]
    (swap! state conj {:cmd "reset"}))
  (clean [this]
    (swap! state conj {:cmd "clean"})))
