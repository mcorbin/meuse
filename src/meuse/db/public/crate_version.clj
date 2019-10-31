(ns meuse.db.public.crate-version
  (:require [meuse.db :refer [database]]
            [meuse.db.actions.crate-version :as crate-version]
            [mount.core :refer [defstate]]))

(defprotocol ICrateVersionDB
  (update-yank [this crate-name crate-version yanked?]))

(defrecord CrateVersionDB [database]
  ICrateVersionDB
  (update-yank [this crate-name crate-version yanked?]
    (crate-version/update-yank database crate-name crate-version yanked?)))

(defstate crate-version-db
  :start (CrateVersionDB. database))
