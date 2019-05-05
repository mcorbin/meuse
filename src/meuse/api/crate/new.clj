(ns meuse.api.crate.new
  (:require [meuse.api.crate.http :refer (crates-api!)]
            [meuse.db.crate :as crate-db]
            [meuse.crate :as crate]
            [meuse.crate-file :as crate-file]
            [meuse.git :as git]
            [meuse.metadata :as metadata]
            [meuse.message :as msg]
            [clojure.java.io :as io]
            [clojure.tools.logging :refer [debug info error]]))

(defmethod crates-api! :new
  [request]
  (info "received new crate request" (:request-id request))
  (let [{:keys [git-metadata raw-metadata crate-file] :as crate}
        (crate/request->crate request)]
    (info "publishing crate" (:name raw-metadata)
          "version" (:vers raw-metadata))
    ;; create the crate in the db
    (crate-db/create-crate (:database request) raw-metadata)
    ;; write the metadata file
    (metadata/write-metadata (get-in request [:config :metadata :path]) git-metadata)
    ;; write the crate binary file
    (crate-file/write-crate-file (get-in request [:config :crate :path]) crate)
    ;; git add
    (git/add (:git request))
    ;; git commit
    (apply git/commit (:git request) (msg/publish-commit-msg raw-metadata))
    ;; git push
    (git/push (:git request))
    {:status 200
     :body {:warning {:invalid_categories []
                      :invalid_badges []
                      :other []}}}))

(defmethod crates-api! :andouillette
  [request]
  {:status 200
   :body (-> "static/andouillette.png" io/resource io/file)})
