(ns meuse.db.public.crate
  "Manage categories in the database"
  (:require [meuse.db :refer [database]]
            [meuse.db.actions.crate :as crate]
            [mount.core :refer [defstate]]))

(defprotocol ICrateDB
  (create [this metadata user-id])
  (get-crate-and-versions [this crate-name])
  (get-crates-and-versions [this])
  (get-crates-for-category [this category-name])
  (get-crates-range [this start end prefix])
  (count-crates [this])
  (count-crates-prefix [this prefix]))

(defrecord CrateDB [database]
  ICrateDB
  (create [this metadata user-id]
    (crate/create database metadata user-id))

  (get-crate-and-versions [this crate-name]
    (crate/get-crate-and-versions database crate-name))

  (get-crates-and-versions [this]
    (crate/get-crates-and-versions database))

  (get-crates-for-category [this category-name]
    (crate/get-crates-for-category database category-name))

  (get-crates-range [this start end prefix]
    (crate/get-crates-range database start end prefix))

  (count-crates [this]
    (crate/count-crates database))

  (count-crates-prefix [this prefix]
    (crate/count-crates-prefix database prefix)))

(defstate crate-db
  :start (CrateDB. database))
