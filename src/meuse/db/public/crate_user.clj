(ns meuse.db.public.crate-user
  (:require [meuse.db :refer [database]]
            [meuse.db.actions.crate-user :as crate-user]
            [mount.core :refer [defstate]]))

(defprotocol ICrateUserDB
  (create-crate-users [this crate-name users])
  (delete [this crate-name user-name])
  (delete-crate-users [this crate-name users])
  (owned-by? [this crate-name user-id]))

(defrecord CrateUserDB [database]
  ICrateUserDB
  (create-crate-users [this crate-name users]
    (crate-user/create-crate-users database crate-name users))
  (delete [this crate-name user-name]
    (crate-user/delete database crate-name user-name))
  (delete-crate-users [this crate-name users]
    (crate-user/delete-crate-users database crate-name users))
  (owned-by? [this crate-name user-id]
    (crate-user/owned-by? database crate-name user-id)))

(defstate crate-user-db
  :start (CrateUserDB. database))
