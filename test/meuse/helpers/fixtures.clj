(ns meuse.helpers.fixtures
  (:require [meuse.auth.frontend :as auth-frontend]
            [meuse.core :as core]
            meuse.crate-file
            [meuse.db :refer [database]]
            meuse.db.public.category
            meuse.db.public.crate
            meuse.db.public.crate-user
            meuse.db.public.crate-version
            meuse.db.public.search
            meuse.db.public.token
            meuse.db.public.user
            [meuse.helpers.db :as helpers]
            meuse.helpers.git
            [meuse.inject :as inject]
            meuse.metric
            meuse.store.filesystem
            [mount.core :as mount]
            [clojure.java.io :as io]
            [clojure.test :refer :all])
  (:import org.apache.commons.io.FileUtils
           [meuse.helpers.git GitMock]
           [meuse.store.filesystem LocalCrateFile]))

(def tmp-dir "test/resources/tmp/")
(def system-started? (atom false))
(def git-mock-state (atom []))
(def default-key-spec (auth-frontend/secret-key-spec
                       "uJo8QrRAANokteN_xVxpP75lc_A5Sw6t"))

(defn tmp-fixture
  [f]
  (FileUtils/deleteDirectory (io/file tmp-dir))
  (FileUtils/forceMkdir (io/file tmp-dir))
  (f))

(defn create-system
  []
  (when-not @system-started?
    (-> (mount/only #{#'meuse.metric/registry
                      #'meuse.config/config
                      #'meuse.db/database
                      #'meuse.crate-file/crate-file-store
                      #'meuse.db.public.category/category-db
                      #'meuse.db.public.crate/crate-db
                      #'meuse.db.public.crate-user/crate-user-db
                      #'meuse.db.public.crate-version/crate-version-db
                      #'meuse.db.public.search/search-db
                      #'meuse.db.public.token/token-db
                      #'meuse.db.public.user/user-db
                      #'meuse.git/git})
        (mount/swap-states {#'meuse.crate-file/crate-file-store
                            {:start #(LocalCrateFile. tmp-dir)}
                            #'meuse.git/git
                            {:start #(GitMock. git-mock-state
                                               (java.lang.Object.))}})
        mount/start)
    (reset! system-started? true))
  (reset! git-mock-state [])
  (inject/inject! true default-key-spec false))

(defn system-fixture
  [f]
  (create-system)
  (f))

(defn db-clean-fixture
  [f]
  (meuse.helpers.db/clean! database)
  (f))

(defn table-fixture
  [f]
  (helpers/load-test-db! database)
  (f))

(defn project-fixture
  [f]
  (mount/start-with-states {#'meuse.git/git {:start #(GitMock. (atom []) (java.lang.Object.))}})
  (meuse.helpers.db/clean! database)
  (helpers/load-test-db! database)
  (inject/inject! true default-key-spec false)
  (f)
  (core/stop!)
  (Thread/sleep 2))
