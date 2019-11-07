(ns meuse.db.public.user
  (:require [meuse.db :refer [database]]
            [meuse.db.actions.user :as user]
            [mount.core :refer [defstate]]))

(defprotocol IUserDB
  (by-name [this user-name])
  (crate-owners [this crate-name])
  (create [this user])
  (delete [this user-name])
  (get-users [this])
  (update-user [this user-name fields]))

(defrecord UserDB [database]
  IUserDB
  (by-name [this user-name]
    (user/by-name database user-name))
  (crate-owners [this crate-name]
    (user/crate-owners database crate-name))
  (create [this user]
    (user/create database user))
  (delete [this user-name]
    (user/delete database user-name))
  (get-users [this]
    (user/get-users database))
  (update-user [this user-name fields]
    (user/update-user database user-name fields)))

(defstate user-db
  :start (UserDB. database))
