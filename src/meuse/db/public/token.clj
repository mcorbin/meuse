(ns meuse.db.public.token
  (:require [meuse.db :refer [database]]
            [meuse.db.actions.token :as token]
            [mount.core :refer [defstate]]))

(defprotocol ITokenDB
  (by-user [this user-name])
  (create [this token])
  (delete [this user-name token-name])
  (set-last-used [this token-id])
  (get-token-user-role [this token]))

(defrecord TokenDB [database]
  ITokenDB
  (by-user [this user-name]
    (token/by-user database user-name))
  (create [this token]
    (token/create database token))
  (delete [this user-name token-name]
    (token/delete database user-name token-name))
  (set-last-used [this token-id]
    (token/set-last-used database token-id))
  (get-token-user-role [this token]
    (token/get-token-user-role database token)))

(defstate token-db
  :start (TokenDB. database))
