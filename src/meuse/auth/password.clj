(ns meuse.auth.password
  (:require [crypto.password.bcrypt :as bcrypt]
            [exoscale.ex :as ex]))

(defn encrypt
  "Encrypt a password"
  [password]
  (bcrypt/encrypt password))

(defn check
  "Verifies is a password is valid."
  [password encrypted]
  (when-not (bcrypt/check password encrypted)
    (throw (ex/ex-forbidden "invalid password" {})))
  true)
