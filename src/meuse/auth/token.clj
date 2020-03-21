(ns meuse.auth.token
  (:require [clj-time.core :as t]
            [crypto.password.bcrypt :as bcrypt]
            [crypto.random :as random]
            [exoscale.ex :as ex])
  (:import org.joda.time.DateTime))

(def token-byte-size 32)
(def token-size 44)
(def identifier-byte-size 18)
(def identifier-size 24)

(defn expiration-date
  "Calculates the expiration date of the token"
  [validity]
  (t/plus (t/now) (t/days validity)))

(defn generate-token
  "generates an identifier and a token"
  []
  (str (random/base64 identifier-byte-size)
       (random/base64 token-byte-size)))

(defn extract-identifier
  "Takes a token, and extract the identifier from it."
  [token]
  (if (> (count token) identifier-size)
    (subs token 0 identifier-size)
    (throw (ex/ex-forbidden "invalid token"))))

(defn valid?
  "Takes a token and a token encrypted with bcrypt.
  Checks if the token is valid."
  [token db-token]
  (and (bcrypt/check token (:tokens/token db-token))
       (t/before? (t/now) (DateTime. (:tokens/expired_at db-token)))))
