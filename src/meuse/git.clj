(ns meuse.git
  (:require [meuse.config :refer [config]]
            [meuse.metadata :as metadata]
            [mount.core :refer [defstate]]
            [clojure.string :as string]
            [clojure.java.shell :as shell]
            [clojure.tools.logging :refer [debug info error]]
            [meuse.crate :as crate]))

(defprotocol Git
  (add [this])
  (git-cmd [this args])
  (commit [this msg-header msg-body])
  (pull [this])
  (push [this])
  (update-yank [this crate-name crate-version yanked]))

(defrecord LocalRepository [path target]
  Git
  (add [this]
    (git-cmd this ["add" "."]))
  (git-cmd [this args]
    (debug "git command" (string/join " " args))
    (let [result (apply shell/sh "git" "-C" path args)]
      (debug "git command status code="(:exit result)
             "out="(:out result)
             "err="(:err result))
      (when-not (= 0 (:exit result))
        (throw (ex-info "error executing git command"
                        {:exit-code (:exit result)
                         :stdout (:out result)
                         :stderr (:err result)
                         :command args})))))
  (commit [this msg-header msg-body]
    (git-cmd this ["commit" "-m" msg-header "-m" msg-body]))
  (push [this]
    (git-cmd this (concat ["push"] (string/split target #"/"))))
  (pull [this]
    (git-cmd this ["pull" target]))
  (update-yank [this crate-name crate-version yanked]
    (metadata/update-yank path crate-name crate-version yanked)))

(defstate git
  :start (map->LocalRepository (:metadata config)))
