(ns meuse.spec
  "Specs of the project"
  (:require exoscale.cloak
            [meuse.semver :as semver]
            [clojure.java.io :as io]
            [clojure.spec.alpha :as s]))

(s/def ::non-empty-string (s/and string? not-empty))
(s/def ::null-or-non-empty-string (s/or :nil nil?
                                        :string (s/and string? not-empty)))
(s/def ::semver semver/valid?)
(s/def ::boolean boolean?)
(s/def ::uuid uuid?)
(s/def ::pos-int pos-int?)
(s/def ::inst inst?)
(s/def ::file (fn [path]
                (let [file (io/file path)]
                  (and (.exists file)
                       (.isFile file)))))

(s/def ::directory (fn [path]
                     (let [file (io/file path)]
                       (and (.exists file)
                            (.isDirectory file)))))

(s/def ::string-or-secret
  (s/or :str ::non-empty-string
        :secret :exoscale.cloak/secret))

;; crate

(s/def :crate/name ::non-empty-string)
(s/def :crate/version ::semver)

;; user

(def min-password-size 8)
(def user-roles #{"admin" "tech" "read-only"})

(s/def :user/name ::non-empty-string)
(s/def :user/password (s/and ::non-empty-string
                             #(>= (count %) min-password-size)))
(s/def :user/description ::non-empty-string)
(s/def :user/active ::boolean)
(s/def :user/role user-roles)

;; token

(s/def :token/id ::uuid)
(s/def :token/name ::non-empty-string)
(s/def :token/validity ::pos-int)
(s/def :token/user :user/name)
(s/def :token/token ::non-empty-string)
(s/def :token/created-at ::inst)
(s/def :token/expired-at ::inst)
(s/def :token/user-id ::uuid)

;; category

(s/def :category/name ::non-empty-string)
(s/def :category/category :category/name)
(s/def :category/description ::non-empty-string)

;; config

(s/def :db/user ::non-empty-string)
(s/def :db/password ::string-or-secret)
(s/def :db/host ::non-empty-string)
(s/def :db/port pos-int?)
(s/def :db/name ::non-empty-string)
(s/def :db/max-pool-size pos-int?)
(s/def :db/key ::file)
(s/def :db/cert ::file)
(s/def :db/cacert ::file)
(s/def :db/ssl-mode ::non-empty-string)
(s/def :db/schema ::non-empty-string)

(s/def :db/database (s/keys :req-un [:db/user
                                     :db/password
                                     :db/host
                                     :db/port
                                     :db/name]
                            :opt-un [:db/max-pool-size
                                     :db/key
                                     :db/cert
                                     :db/cacert
                                     :db/ssl-mode
                                     :db/schema]))

(s/def :http/port ::pos-int)
(s/def :http/address ::non-empty-string)

(s/def :http/key ::file)
(s/def :http/cert ::file)
(s/def :http/cacert ::file)

(s/def :http/tls (fn [config]
                   (or (and (:key config) (:cert config) (:cacert config))
                       (and (not (:key config))
                            (not (:cert config))
                            (not (:cacert config))))))

(s/def :http/http (s/and (s/keys :req-un [:http/port
                                          :http/address]
                                 :opt-un [:http/key
                                          :http/cert
                                          :http/cacert])
                         :http/tls))

(defmulti metadata :type)

(s/def :metadata/path ::directory)
(s/def :metadata/target ::non-empty-string)
(s/def :metadata/url ::non-empty-string)

(defmethod metadata "shell"
  [_]
  (s/keys :req-un [:metadata/path
                   :metadata/target
                   :metadata/url]))

(s/def :metadata/username ::non-empty-string)
(s/def :metadata/password ::string-or-secret)

(defmethod metadata "jgit"
  [_]
  (s/keys :req-un [:metadata/path
                   :metadata/target
                   :metadata/url
                   :metadata/username
                   :metadata/password]))

(defmethod metadata :default
  [_]
  (s/keys :req-un [:metadata/path
                   :metadata/target
                   :metadata/url]))

(s/def :metadata/metadata (s/multi-spec metadata :type))

(s/def :crate/path ::directory)

(defmulti crate-store :store)

(defmethod crate-store "filesystem"
  [_]
  (s/keys :req-un [:crate/path]))

(s/def :s3/access-key ::string-or-secret)
(s/def :s3/secret-key ::string-or-secret)
(s/def :s3/endpoint ::non-empty-string)
(s/def :s3/bucket ::non-empty-string)

(defmethod crate-store "s3"
  [_]
  (s/keys :req-un [:s3/access-key
                   :s3/secret-key
                   :s3/endpoint
                   :s3/bucket]))

(defmethod crate-store :default
  [_]
  (throw (ex-info "invalid crate store configuration" {})))

(s/def :crate/crate (s/multi-spec crate-store :store))

(s/def ::level #{"debug" "info"})
(s/def ::encoder #{"json"})
(s/def ::console (s/or :bool ::boolean
                       :map (s/keys :req-un [::encoder])))
(s/def ::logging (s/keys :req-un [::level ::console]))

(s/def :frontend/enabled boolean?)

(def frontend-secret-min-size 20)
(s/def :frontend/secret ::string-or-secret)

(defmulti frontend :public)
(defmethod frontend true [_]
  (s/keys :req-un [:frontend/enabled]))
(defmethod frontend :default [_]
  (s/keys :req-un [:frontend/enabled
                   :frontend/secret]))

(s/def :frontend/frontend
   (s/multi-spec frontend :public))

(s/def ::config (s/keys :req-un [:http/http
                                 :db/database
                                 :metadata/metadata
                                 :crate/crate
                                 ::logging
                                 :frontend/frontend]))

;; api

;; download

(s/def :meuse.api.crate.download/crate-name :crate/name)
(s/def :meuse.api.crate.download/crate-version :crate/version)
(s/def :meuse.api.crate.download/route-params
  (s/keys :req-un [:meuse.api.crate.download/crate-name
                   :meuse.api.crate.download/crate-version]))

(s/def :meuse.api.crate.download/download
  (s/keys :req-un [:meuse.api.crate.download/route-params]))

(s/def :meuse.api.crate.mirror/cache
  (s/keys :req-un [:meuse.api.crate.download/route-params]))

;; new

(s/def :meuse.api.crate.new/body #(instance? Object %))
(s/def :meuse.api.crate.new/new
  (s/keys :req-un [:meuse.api.crate.new/body]))

(s/def :deps/name :crate/name)
(s/def :deps/version_req ::non-empty-string)
(s/def :deps/features (s/coll-of ::non-empty-string))
(s/def :deps/optional ::boolean)
(s/def :deps/default_features ::boolean)
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

(s/def :meuse.api.meuse.category-update/route-params
  (s/keys :req-un [:category/name]))

(s/def :meuse.api.meuse.category-update/body
  (s/keys :opt-un [:category/name
                   :category/description]))

(s/def :meuse.api.meuse.category/update
  (s/keys :req-un [:meuse.api.meuse.category-update/route-params
                   :meuse.api.meuse.category-update/body]))

;; user

(s/def :meuse.api.meuse.user-new/body
  (s/keys :req-un [:user/description
                   :user/password
                   :user/name
                   :user/active
                   :user/role]))

(s/def :meuse.api.meuse.user/new
  (s/keys :req-un [:meuse.api.meuse.user-new/body]))

(s/def :meuse.api.meuse.user-delete/route-params
  (s/keys :req-un [:user/name]))

(s/def :meuse.api.meuse.user/delete
  (s/keys :req-un [:meuse.api.meuse.user-delete/route-params]))

(s/def :meuse.api.meuse.user-update/route-params
  (s/keys :req-un [:user/name]))

(s/def :meuse.api.meuse.user-update/body
  (s/keys :opt-un [:user/password
                   :user/description
                   :user/active
                   :user/role]))

(s/def :meuse.api.meuse.user/update
  (s/keys :req-un [:meuse.api.meuse.user-update/route-params
                   :meuse.api.meuse.user-update/body]))

;; token

(s/def :meuse.api.meuse.token-delete/body
  (s/keys :req-un [:token/name
                   :token/user]))

(s/def :meuse.api.meuse.token/delete
  (s/keys :req-un [:meuse.api.meuse.token-delete/body]))

(s/def :meuse.api.meuse.token-create/body
  (s/keys :req-un [:token/name
                   :token/validity
                   :token/user
                   :user/password]))

(s/def :meuse.api.meuse.token/create
  (s/keys :req-un [:meuse.api.meuse.token-create/body]))

(s/def :meuse.api.meuse.token-list/params
  (s/keys :opt-un [:token/user]))

(s/def :meuse.api.meuse.token/list
  (s/keys :opt-un [:meuse.api.meuse.token-list/params]))

;; crate

(s/def :meuse.api.meuse.crate/route-params
  (s/keys :req-un [:crate/name]))

(s/def :meuse.api.meuse.crate/get
  (s/keys :req-un [:meuse.api.meuse.crate/route-params]))

(s/def :meuse.api.meuse.crate/params
  (s/keys :opt-un [:category/category]))

(s/def :meuse.api.meuse.crate/list
  (s/keys :opt-un [:meuse.api.meuse.crate/params]))
