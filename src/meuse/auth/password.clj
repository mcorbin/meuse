(ns meuse.auth.password
  (:require [crypto.password.bcrypt :as bcrypt]))

(defn encrypt
  [password]
  (bcrypt/encrypt password))

(defn check
  [password encrypted]
  (bcrypt/check password encrypted))
