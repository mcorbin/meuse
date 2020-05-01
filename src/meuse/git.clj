(ns meuse.git
  "Interacts with a git repository"
  (:require [meuse.config :refer [config]]
            [meuse.log :as log]
            [meuse.metric :as metric]
            [exoscale.ex :as ex]
            [mount.core :refer [defstate]]
            [clojure.java.shell :as shell]
            [clojure.string :as string]))

(defprotocol Git
  (add [this])
  (commit [this msg-header msg-body])
  (get-lock [this])
  (pull [this])
  (push [this]))

(defn git-cmd
  [path args]
  (log/debug {} "git command" (string/join " " args))
  (metric/with-time :git.local ["command" (first args)]
    (let [result (apply shell/sh "git" "-C" path args)]
      (log/debug {} "git command status code=" (:exit result)
                 "out=" (:out result)
                 "err=" (:err result))
      (when-not (= 0 (:exit result))
        (throw (ex/ex-fault "error executing git command"
                            {:exit-code (:exit result)
                             :stdout (:out result)
                             :stderr (:err result)
                             :command args}))))))

(defrecord LocalRepository [path target lock]
  Git
  (add [this]
    (git-cmd path ["add" "."]))
  (commit [this msg-header msg-body]
    (git-cmd path ["commit" "-m" msg-header "-m" msg-body]))
  (get-lock [this]
    lock)
  (push [this]
    (git-cmd path (concat ["push"] (string/split target #"/"))))
  (pull [this]
    (git-cmd path ["pull" target])))

(defstate git
  :start (map->LocalRepository (merge (:metadata config)
                                      {:lock (java.lang.Object.)})))
