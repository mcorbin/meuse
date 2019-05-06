(ns meuse.spec
  "Specs of the project"
  (:require [meuse.semver :as semver]
            [clojure.spec.alpha :as s]))

(s/def ::non-empty-string (s/and string? not-empty))
(s/def :crate/name ::non-empty-string)
(s/def :crate/version semver/valid?)

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

(s/def :metadata/path ::non-empty-string)
(s/def :metadata/target ::non-empty-string)
(s/def :metadata/url ::non-empty-string)
(s/def :metadata/metadata (s/keys :req-un [:metadata/path
                                           :metadata/target
                                           :metadata/url]))

(s/def :crate/path ::non-empty-string)
(s/def :crate/crate (s/keys :req-un [:crate/path]))

(s/def ::level #{"debug" "info"})
(s/def ::encoder #{"json"})
(s/def ::console (s/or :boot boolean?
                       :map (s/keys :req-un [::encoder])))
(s/def ::logging (s/keys :req-un [::level ::console]))


(s/def ::config (s/keys :req-un [:http/http
                                 :db/database
                                 :metadata/metadata
                                 :crate/crate
                                 ::logging]))

;; api

;; download

(s/def :meuse.api.crate.download/crate-name :crate/name)
(s/def :meuse.api.crate.download/crate-version :crate/version)
(s/def :meuse.api.crate.download/route-params
  (s/keys :req-un [:meuse.api.crate.download/crate-name
                   :meuse.api.crate.download/crate-version]))

(s/def :meuse.api.crate.download/api
  (s/keys :req-un [:meuse.api.crate.download/route-params]))
