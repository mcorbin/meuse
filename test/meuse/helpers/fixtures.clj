(ns meuse.helpers.fixtures
  (:require [meuse.core :as core]
            [meuse.db :refer [database]]
            [meuse.helpers.db :as helpers]
            [mount.core :as mount]
            [clojure.java.io :as io]
            [clojure.test :refer :all])
  (:import org.apache.commons.io.FileUtils))

(def tmp-dir "test/resources/tmp/")
(def db-started? (atom false))

(defn tmp-fixture
  [f]
  (FileUtils/deleteDirectory (io/file tmp-dir))
  (FileUtils/forceMkdir (io/file tmp-dir))
  (f))

(defn db-fixture
  [f]
  (when-not @db-started?
    (mount/start #'meuse.config/config #'meuse.db/database)
    (reset! db-started? true))
  (f)
  (comment (mount/stop #'meuse.config/config #'meuse.db/database)))

(defn table-fixture
  [f]
  (meuse.helpers.db/clean! database)
  (helpers/load-test-db! database)
  (f))


(defn project-fixture
  [f]
  (core/start!)
  (meuse.helpers.db/clean! database)
  (helpers/load-test-db! database)
  (f)
  (core/stop!)
  (Thread/sleep 2)
  )
