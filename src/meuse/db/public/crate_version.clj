(ns meuse.db.public.crate-version
  (:require [meuse.db :refer [database]]
            [meuse.db.actions.crate-version :as crate-version]
            [mount.core :refer [defstate]]))

(defprotocol ICrateVersionDB
  (last-updated [this n])
  (update-yank [this crate-name crate-version yanked?])
  (count-crates-versions [this]))

(defrecord CrateVersionDB [database]
  ICrateVersionDB
  (last-updated [this n]
    (crate-version/last-updated database n))
  (update-yank [this crate-name crate-version yanked?]
    (crate-version/update-yank database crate-name crate-version yanked?))
  (count-crates-versions [this]
    (crate-version/count-crates-versions database)))

(defstate crate-version-db
  :start (CrateVersionDB. database))
