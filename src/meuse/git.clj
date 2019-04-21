(ns meuse.git
  (:require [meuse.config :refer [config]]
            [meuse.crate :as c]
            [mount.core :refer [defstate]]
            [clojure.string :as string]
            [clojure.java.shell :as shell]
            [clojure.tools.logging :refer [debug info error]]))

(defprotocol Git
  (add [this])
  (git-cmd [this args])
  (add-crate [this crate])
  (commit [this msg])
  (pull [this])
  (push [this]))

(defrecord LocalRepository [path target]
  Git
  (add [this]
    (git-cmd this ["add" "."]))
  (git-cmd [this args]
    (debug "command" args)
    (let [result (apply shell/sh "git" "-C" path args)]
      (debug "exit="(:exit result)
             "out="(:out result)
             "err="(:err result))
      (when (not= 0 (:exit result))
        (throw (ex-info "error executing git command"
                        :exit-code (:exit result)
                        :stdout (:out result)
                        :stderr (:err result)
                        :command args)))))
  (add-crate [this crate]
    (c/write-metadata path crate))
  (commit [this crate]
    (let [[header body] (c/commit-msg crate)]
      (git-cmd this ["commit" "-m" header "-m" body])))
  (push [this]
    (git-cmd this (concat ["push"] (string/split target #"/"))))
  (pull [this]
    (git-cmd this ["pull" target])))

(defstate git
  :start (map->LocalRepository (:git config)))
