(ns meuse.helpers.fixtures
  (:require [meuse.db :refer [database]]
            [meuse.helpers.db :as helpers]
            [clojure.java.io :as io]
            [clojure.java.jdbc :as jdbc]
            [clojure.test :refer :all]
            [mount.core :as mount])
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
  (jdbc/execute! database ["TRUNCATE TABLE CRATES CASCADE;"])
  (jdbc/execute! database ["TRUNCATE TABLE ROLES CASCADE;"])
  (jdbc/execute! database ["TRUNCATE TABLE CATEGORIES CASCADE;"])
  (jdbc/execute! database ["TRUNCATE TABLE TOKENS CASCADE;"])
  (jdbc/execute! database ["TRUNCATE TABLE USERS CASCADE;"])
  (jdbc/execute! database ["TRUNCATE TABLE CRATES_USERS CASCADE;"])
  (helpers/load-test-db! database)
  (f))
