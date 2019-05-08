(ns meuse.spec
  "Specs of the project"
  (:require [meuse.semver :as semver]
            [clojure.spec.alpha :as s]))

(s/def ::non-empty-string (s/and string? not-empty))
(s/def ::null-or-non-empty-string (s/or :nil nil?
                                        :string (s/and string? not-empty)))
;; crate

(s/def :crate/name ::non-empty-string)
(s/def :crate/version semver/valid?)

;; user

(s/def :user/name ::non-empty-string)

;; category

(s/def :category/name ::non-empty-string)
(s/def :category/description ::non-empty-string)

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

(s/def :meuse.api.crate.download/download
  (s/keys :req-un [:meuse.api.crate.download/route-params]))

;; new

(s/def :meuse.api.crate.new/body #(instance? Object %))
(s/def :meuse.api.crate.new/new
  (s/keys :req-un [:meuse.api.crate.new/body]))

(s/def :deps/name :crate/name)
(s/def :deps/version_req ::non-empty-string)
(s/def :deps/features (s/coll-of ::non-empty-string))
(s/def :deps/optional boolean?)
(s/def :deps/default_features boolean?)
(s/def :deps/target ::null-or-non-empty-string)
(s/def :deps/kind ::non-empty-string)
(s/def :deps/registry ::null-or-non-empty-string)
(s/def :deps/explicit_name_in_toml ::null-or-non-empty-string)

(s/def :deps/deps (s/keys :req-un [:deps/name
                                   :deps/version_req]
                          :opt-un [:deps/features
                                   :deps/optional
                                   :deps/default_features
                                   :deps/target
                                   :deps/kind
                                   :deps/registry
                                   :deps/explicit_name_in_toml]))

(s/def :features/extras (s/coll-of ::non-empty-string))

(s/def :meuse.api.crate.new/name :crate/name)
(s/def :meuse.api.crate.new/vers :crate/version)
(s/def :meuse.api.crate.new/deps (s/coll-of :deps/deps))
(s/def :meuse.api.crate.new/features (s/keys :opt-un [:features/extras]))
(s/def :meuse.api.crate.new/authors (s/coll-of ::non-empty-string))
(s/def :meuse.api.crate.new/description ::null-or-non-empty-string)
(s/def :meuse.api.crate.new/documentation ::null-or-non-empty-string)
(s/def :meuse.api.crate.new/homepage ::null-or-non-empty-string)
(s/def :meuse.api.crate.new/readme ::null-or-non-empty-string)
(s/def :meuse.api.crate.new/readme_file ::null-or-non-empty-string)
(s/def :meuse.api.crate.new/keywords (s/coll-of ::non-empty-string))
(s/def :meuse.api.crate.new/categories (s/coll-of ::non-empty-string))
(s/def :meuse.api.crate.new/license ::null-or-non-empty-string)
(s/def :meuse.api.crate.new/license_file ::null-or-non-empty-string)
(s/def :meuse.api.crate.new/repository ::null-or-non-empty-string)
(s/def :meuse.api.crate.new/links ::null-or-non-empty-string)

(s/def :meuse.api.crate.new/raw-metadata
  (s/keys :req-un [:meuse.api.crate.new/name
                   :meuse.api.crate.new/vers]
          :opt-un [:meuse.api.crate.new/deps
                   :meuse.api.crate.new/features
                   :meuse.api.crate.new/authors
                   :meuse.api.crate.new/description
                   :meuse.api.crate.new/documentation
                   :meuse.api.crate.new/homepage
                   :meuse.api.crate.new/readme
                   :meuse.api.crate.new/readme_file
                   :meuse.api.crate.new/keywords
                   :meuse.api.crate.new/categories
                   :meuse.api.crate.new/license
                   :meuse.api.crate.new/license_file
                   :meuse.api.crate.new/repository
                   :meuse.api.crate.new/links]))

(s/def :meuse.api.crate.new/crate-file bytes?)

(s/def :meuse.api.crate.new/crate
  (s/keys :req-un [:meuse.api.crate.new/raw-metadata
                   :meuse.api.crate.new/crate-file]))

;; owner

(s/def :meuse.api.crate.owner/crate-name :crate/name)
(s/def :meuse.api.crate.owner/users (s/coll-of :user/name
                                               :min-count 1))

(s/def :meuse.api.crate.owner/route-params
  (s/keys :req-un [:meuse.api.crate.owner/crate-name]))
(s/def :meuse.api.crate.owner/body
  (s/keys :req-un [:meuse.api.crate.owner/users]))

(s/def :meuse.api.crate.owner/add
  (s/keys :req-un [:meuse.api.crate.owner/route-params
                   :meuse.api.crate.owner/body]))

(s/def :meuse.api.crate.owner/remove
  (s/keys :req-un [:meuse.api.crate.owner/route-params
                   :meuse.api.crate.owner/body]))

(s/def :meuse.api.crate.owner/list
  (s/keys :req-un [:meuse.api.crate.owner/route-params]))

;; search

(s/def :meuse.api.crate.search/q ::non-empty-string)
(s/def :meuse.api.crate.search/per_page (s/and ::non-empty-string
                                               #(try (Integer/parseInt %)
                                                     (catch Exception _
                                                       false))))

(s/def :meuse.api.crate.search/params
  (s/keys :req-un [:meuse.api.crate.search/q]
          :opt-un [:meuse.api.crate.search/per_page]))

(s/def :meuse.api.crate.search/search
  (s/keys :req-un [:meuse.api.crate.search/params]))

;; yank

(s/def :meuse.api.crate.yank/crate-name :crate/name)
(s/def :meuse.api.crate.yank/crate-version :crate/version)

(s/def :meuse.api.crate.yank/route-params
  (s/keys :req-un [:meuse.api.crate.yank/crate-name
                   :meuse.api.crate.yank/crate-version]))

(s/def :meuse.api.crate.yank/yank
  (s/keys :req-un [:meuse.api.crate.yank/route-params]))

;; category

(s/def :meuse.api.meuse.category/body
  (s/keys :req-un [:category/description
                   :category/name]))

(s/def :meuse.api.meuse.category/new
  (s/keys :req-un [:meuse.api.meuse.category/body]))
