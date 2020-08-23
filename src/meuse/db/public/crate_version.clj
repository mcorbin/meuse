(ns meuse.db.public.crate-version
  (:require [meuse.db :refer [database]]
            [meuse.db.actions.crate-version :as crate-version]
            [mount.core :refer [defstate]]))

(defprotocol ICrateVersionDB
  (count-crates-versions [this])
  (inc-download [this crate-name version])
  (last-updated [this n])
  (top-n-downloads [this n])
  (update-yank [this crate-name crate-version yanked?])
  (sum-download-count [this])
  (delete [this crate-name version]))

(defrecord CrateVersionDB [database]
  ICrateVersionDB
  (count-crates-versions [this]
    (crate-version/count-crates-versions database))
  (inc-download [this crate-name version]
    (crate-version/inc-download database crate-name version))
  (last-updated [this n]
    (crate-version/last-updated database n))
  (top-n-downloads [this n]
    (crate-version/top-n-downloads database n))
  (update-yank [this crate-name crate-version yanked?]
    (crate-version/update-yank database crate-name crate-version yanked?))
  (sum-download-count [this]
    (crate-version/sum-download-count database))
  (delete [this crate-name version]
    (crate-version/delete database crate-name version)))

(defstate crate-version-db
  :start (CrateVersionDB. database))
