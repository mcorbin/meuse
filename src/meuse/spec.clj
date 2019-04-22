(ns meuse.spec
  (:require [clojure.spec.alpha :as s]))

(s/def ::non-empty-string (s/and string? not-empty))

;; config

(s/def :db/subname ::non-empty-string)
(s/def :db/user ::non-empty-string)
(s/def :db/password ::non-empty-string)
(s/def :db/database (s/keys :req-un [:db/subname
                                     :db/user
                                     :db/password]))


(s/def :http/port pos-int?)
(s/def :http/address ::non-empty-string)
(s/def :http/http (s/keys :req-un [:http/port
                                   :http/address]))

(s/def :git/path ::non-empty-string)
(s/def :git/target ::non-empty-string)

(s/def :crate/path ::non-empty-string)

(s/def ::crate (s/keys :req-un [:crate/path]))

(s/def ::git (s/keys :req-un [:git/path :git/target]))

(s/def ::level #{"debug" "info"})
(s/def ::encoder #{"json"})
(s/def ::console (s/or :boot boolean?
                       :map (s/keys :req-un [::encoder])))
(s/def ::logging (s/keys :req-un [::level ::console]))


(s/def ::config (s/keys :req-un [:http/http
                                 :db/database
                                 ::git
                                 ::crate
                                 ::logging]))
