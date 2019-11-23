(ns meuse.db.public.category
  (:require [meuse.db :refer [database]]
            [meuse.db.actions.category :as category]
            [mount.core :refer [defstate]]))

(defprotocol ICategoryDB
  (by-crate-id [this crate-id])
  (create [this category-name description])
  (update-category [this category-name fields])
  (get-categories [this])
  (count-crates [this]))

(defrecord CategoryDB [database]
  ICategoryDB
  (by-crate-id [this crate-id]
    (category/by-crate-id database crate-id))

  (create [this category-name description]
    (category/create database category-name description))

  (update-category [this category-name fields]
    (category/update-category database category-name fields))

  (get-categories [this]
    (category/get-categories database))

  (count-crates [this]
    (category/count-crates-for-categories database)))

(defstate category-db
  :start (CategoryDB. database))
