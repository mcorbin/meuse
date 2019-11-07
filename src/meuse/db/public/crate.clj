(ns meuse.db.public.crate
  "Manage categories in the database"
  (:require [meuse.db :refer [database]]
            [meuse.db.actions.crate :as crate]
            [mount.core :refer [defstate]]))

(defprotocol ICrateDB
  (create [this metadata user-id])
  (get-crate-and-versions [this crate-name])
  (get-crates-and-versions [this])
  (get-crates-for-category [this category-name]))

(defrecord CrateDB [database]
  ICrateDB
  (create [this metadata user-id]
    (crate/create database metadata user-id))

  (get-crate-and-versions [this crate-name]
    (crate/get-crate-and-versions database crate-name))

  (get-crates-and-versions [this]
    (crate/get-crates-and-versions database))

  (get-crates-for-category [this category-name]
    (crate/get-crates-for-category database category-name)))

(defstate crate-db
  :start (CrateDB. database))
