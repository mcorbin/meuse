(ns meuse.git
  "Interacts with a git repository"
  (:require [meuse.config :as config]
            [meuse.log :as log]
            [meuse.metric :as metric]
            [exoscale.cloak :as cloak]
            [exoscale.ex :as ex]
            [mount.core :refer [defstate]]
            [clojure.java.io :as io]
            [clojure.java.shell :as shell]
            [clojure.string :as string])
  (:import (org.eclipse.jgit.api Git)
           (org.eclipse.jgit.api ResetCommand$ResetType)
           (org.eclipse.jgit.transport CredentialsProvider
                                       RefSpec
                                       UsernamePasswordCredentialsProvider)))

(defprotocol IGit
  (add [this])
  (commit [this msg-header msg-body])
  (get-lock [this])
  (pull [this])
  (reset-hard [this])
  (clean [this])
  (push [this]))

(defn git-cmd
  [path args]
  (log/debug {} "git command" (string/join " " args))
  (metric/with-time :git.shell {"command" (first args)}
    (let [result (apply shell/sh "git" "-C" path args)]
      (log/debug {} "git command status code=" (:exit result)
                 "out=" (:out result)
                 "err=" (:err result))
      (when-not (= 0 (:exit result))
        (throw (ex/ex-fault (format "error executing git command %s: %s %s"
                                    (first args)
                                    (:out result)
                                    (:err result))
                            {:exit-code (:exit result)
                             :stdout (:out result)
                             :stderr (:err result)
                             :command args}))))))

(defrecord LocalRepository [path target lock]
  IGit
  (add [this]
    (git-cmd path ["add" "."]))
  (commit [this msg-header msg-body]
    (git-cmd path ["commit" "-m" msg-header "-m" msg-body]))
  (get-lock [this]
    lock)
  (push [this]
    (git-cmd path (concat ["push"] (string/split target #"/"))))
  (reset-hard [this]
    (git-cmd path (concat ["reset"] (string/split target #"/"))))
  (clean [this]
    (git-cmd path ["clean" "-f" "-d"]))
  (pull [this]
    (git-cmd path (concat ["pull"] (string/split target #"/")))))

(defrecord JGitFileRepository [target
                               lock
                               ^Git git
                               ^CredentialsProvider credentials]
  IGit
  (add [this]
    (metric/with-time :git.jgit {"command" "add"}
      (doto (.add git)
        (.addFilepattern ".")
        (.call))))
  (commit [this msg-header msg-body]
    (metric/with-time :git.jgit {"command" "commit"}
      (doto (.commit git)
        (.setMessage (str msg-header "\n\n" msg-body))
        (.call))))
  (get-lock [this]
    lock)
  (pull [this]
    (metric/with-time :git.jgit {"command" "pull"}
      (let [[remote branch] (string/split target #"/")]
        (doto (.pull git)
          (.setCredentialsProvider credentials)
          (.setRemote remote)
          (.setRemoteBranchName branch)
          (.call)))))
  (reset-hard [this]
    (metric/with-time :git.jgit {"command" "reset"}
      (doto (.reset git)
        (.setMode ResetCommand$ResetType/HARD)
        (.call))))
  (clean [this]
    (metric/with-time :git.jgit {"command" "clean"}
      (doto (.clean git)
        (.setCleanDirectories true)
        (.call))))
  (push [this]
    (metric/with-time :git.jgit {"command" "push"}
      (let [[remote branch] (string/split target #"/")
            ref-spec (RefSpec. branch)]
        (doto (.push git)
          (.setCredentialsProvider credentials)
          (.setRemote remote)
          (.setRefSpecs (into-array [ref-spec]))
          (.call))))))

(defn build-jgit
  [config]
  (map->JGitFileRepository
   {:credentials (UsernamePasswordCredentialsProvider.
                  (:username config)
                  (:password config))
    :git (Git/open (io/file (:path config)))
    :lock (java.lang.Object.)
    :target (:target config)}))

(defn build-local-git
  [config]
  (map->LocalRepository
   {:path (:path config)
    :lock (java.lang.Object.)
    :target (:target config)}))

(defstate git
  :start (condp = (get-in config/config [:metadata :type])
           "jgit" (build-jgit (cloak/unmask (:metadata config/config)))
           "shell" (build-local-git (:metadata config/config))
           (build-local-git (:metadata config/config))))
