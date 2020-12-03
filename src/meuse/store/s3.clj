(ns meuse.store.s3
  "Manipulates the crate files in s3."
  (:require [meuse.store.protocol :refer [ICrateStore]]
            [amazonica.aws.s3 :as s3]
            [exoscale.cloak :as cloak]
            [clojure.string :as string])
  (:import java.io.ByteArrayInputStream
           org.apache.commons.codec.digest.DigestUtils
           org.apache.commons.codec.binary.Base64
           org.apache.commons.io.IOUtils))

(defn file-key
  "Get a key for a crate file for a crate name and a version"
  [crate-name version prefix]
  (if (string/blank? prefix)
    (format "%s/%s/download" crate-name version)
    (format "%s/%s/%s/download" prefix crate-name version)))

(defn file-exists?
  "Checks if a crate file exists for a crate name and a version"
  [credentials bucket crate-name version prefix]
  (s3/does-object-exist (cloak/unmask credentials)
                        bucket
                        (file-key crate-name version prefix)))

(defn get-file-from-s3
  "Get a file from s3."
  [credentials bucket crate-name version prefix]
  (IOUtils/toByteArray
   (:object-content
    (s3/get-object (cloak/unmask credentials)
                   :bucket-name bucket
                   :key (file-key crate-name version prefix)))))

(defn write-file-on-s3
  "Put a crate file on S3."
  [credentials bucket crate-name version crate-file prefix]
  (let [input-stream (ByteArrayInputStream. crate-file)
        digest (DigestUtils/md5 crate-file)]
    (s3/put-object (cloak/unmask credentials)
                   :bucket-name bucket
                   :key (file-key crate-name version prefix)
                   :input-stream input-stream
                   :metadata {:content-length (alength crate-file)
                              :content-md5 (String. (Base64/encodeBase64
                                                     digest))})))

(defn validate-crate-version
  [state crate-path]
  (let [[_ version download] (string/split crate-path #"/")]
    (if version
      (assoc state version (= download "download"))
      state)))

(defn s3-versions
  "Returns the versions stored on s3 for a crate."
  [credentials bucket crate-name]
  (->> (s3/list-objects-v2 (cloak/unmask credentials)
                           {:bucket-name bucket
                            :prefix (str crate-name "/")})
       :object-summaries
       (map :key)
       (reduce validate-crate-version {})))

(defrecord S3CrateStore [credentials bucket prefix]
  ICrateStore
  (exists [this crate-name version]
    (file-exists? credentials bucket crate-name version prefix))
  (get-file [this crate-name version]
    (get-file-from-s3 credentials bucket crate-name version prefix))
  (versions [this crate-name]
    (s3-versions credentials bucket crate-name))
  (write-file [this raw-metadata crate-file]
    (write-file-on-s3 credentials
                      bucket
                      (:name raw-metadata)
                      (:vers raw-metadata)
                      crate-file
                      prefix))
  (delete-file [this crate-name version]
    (s3/delete-object credentials
                      :bucket-name bucket
                      :key (file-key crate-name version prefix))))
