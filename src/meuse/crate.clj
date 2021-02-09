(ns meuse.crate
  "Crate utility functions"
  (:require [meuse.db.public.crate :as public-crate]
            [meuse.git :as git]
            [meuse.metadata :as metadata]
            [meuse.store.protocol :as store]
            [byte-streams :as bs]
            [cheshire.core :as json]
            [digest :as digest]
            [exoscale.ex :as ex]
            [clojure.set :as set])
  (:import java.util.Arrays))

(defn check-size
  "Takes a request, a byte array and a size.
  Throws an exception if the byte array size is lower than the size."
  [byte-array size]
  (when (< (alength #^bytes byte-array) size)
    (throw (ex/ex-incorrect (format "invalid request size %d"
                                    (alength #^bytes byte-array))))))

(def git-metadata-keys [:name :vers :deps :cksum :features :yanked :links])
(def deps-metadata-keys [:name :version_req :features :optional :default_features :target :kind :registry :package])
(def deps-keys-renamed {:version_req :req})

(defn handler-crate-renaming
  "Handle renaming for a dependency.
  :explicit_name_in_toml is the new name of the dependency if the dependency
  is renamed. If set, the git metadata should have this value in :name and
  the old/real name in :package"
  [dep]
  (if-let [new-name (:explicit_name_in_toml dep)]
    (assoc dep
           :name new-name
           :package (:name dep))
    dep))

(defn raw-metadata->metadata
  "Converts the raw metadata from `cargo publish` into metadata which will be
  stored in the Git repository."
  [raw-metadata]
  (-> (select-keys raw-metadata git-metadata-keys)
      (update :deps (fn [deps] (map handler-crate-renaming deps)))
      (update :deps (fn [deps] (map #(select-keys % deps-metadata-keys) deps)))
      (update :deps (fn [deps] (map #(set/rename-keys % deps-keys-renamed) deps)))))

(defn request->crate
  "Takes an HTTP publish request. Converts the payload to a map containing
  the raw metadata from the request, the git metadata and the crate file."
  [request]
  (let [byte-array (bs/to-byte-array (:body request))
        _ (check-size byte-array 8)
        metadata-size (+ (bit-shift-left (bit-and (aget byte-array 3) 0xFF) 24)
                         (bit-shift-left (bit-and (aget byte-array 2) 0xFF) 16)
                         (bit-shift-left (bit-and (aget byte-array 1) 0xFF) 8)
                         (bit-and (aget byte-array 0) 0xFF))
        _ (check-size byte-array (+ 4 metadata-size))
        metadata (-> (String. (Arrays/copyOfRange byte-array 4 (+ 4 metadata-size)))
                     (json/parse-string true))
        crate-size (+ (bit-shift-left (bit-and (aget byte-array
                                                     (+ metadata-size 4 3))
                                               0xFF)
                                      24)
                      (bit-shift-left (bit-and (aget byte-array
                                                     (+ metadata-size 4 2))
                                               0xFF)
                                      16)
                      (bit-shift-left (bit-and (aget byte-array
                                                     (+ metadata-size 4 1))
                                               0xFF)
                                      8)
                      (bit-and (aget byte-array
                                     (+ metadata-size 4))
                               0xFF))
        _ (check-size byte-array (+ metadata-size 4 crate-size))
        crate-file (Arrays/copyOfRange byte-array
                                       (+ 4 metadata-size 4)
                                       (+ 4 metadata-size 4 crate-size))
        sha256sum (digest/sha-256 crate-file)]
    {:raw-metadata metadata
     :git-metadata (-> metadata
                       (assoc :yanked false)
                       (assoc :cksum sha256sum)
                       raw-metadata->metadata)
     :crate-file crate-file}))

(defn verify-versions
  [crate-file-store metadata-path]
  (fn [state [crate-name versions]]
    (let [crate-versions (set (map :crates_versions/version versions))
          metadata-versions (set (metadata/versions
                                  metadata-path
                                  crate-name))
          files-versions (store/versions
                          crate-file-store
                          crate-name)
          files-missing (remove (fn [[k v]] v) files-versions)
          files-versions-set (set (map first files-versions))

          diff-crate-meta (set/difference crate-versions metadata-versions)
          diff-meta-crate (set/difference metadata-versions crate-versions)

          diff-crate-file (set/difference crate-versions files-versions-set)
          diff-file-crate (set/difference files-versions-set crate-versions)]
      (conj state
            {:crate crate-name
             :errors (cond-> []
                       (seq files-missing)
                       (concat (map #(str "missing crate binary file for version "
                                          (first %))
                                    files-missing))

                       (seq diff-crate-meta)
                       (concat (map #(str "metadata does not exist for version "
                                          %)
                                    diff-crate-meta))

                       (seq diff-meta-crate)
                       (concat (map #(str "metata exists but not in the database for version "
                                          %)
                                    diff-meta-crate))

                       (seq diff-crate-file)
                       (concat (map #(str "crate binary file does not exist for version "
                                          %)
                                    diff-crate-file))

                       (seq diff-file-crate)
                       (concat (map #(str "crate binary file exists but not in the db for version "
                                          %)
                                    diff-file-crate)))}))))

(defn check
  "Verifies if the database, the crate files and the metadata are not out of sync.
  Returns the list of crates and versions which are out of sync."
  [crate-db git-object crate-file-store request]
  (locking (git/get-lock git-object)
    (let [crates-versions (->> (public-crate/get-crates-and-versions
                                crate-db)
                               (group-by :crates/name))]
      (->> (reduce (verify-versions crate-file-store
                                    (get-in request [:config :metadata :path]))
                   []
                   crates-versions)
           (filter #(seq (:errors %)))))))
