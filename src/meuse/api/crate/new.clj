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
  (let [{:keys [metadata crate-file] :as crate}
        (crate/request->crate request)]
    (info "publishing crate" (:name metadata)
          "version" (:vers metadata))
    (crate-db/create-crate (:database request) (:metadata crate))
    (metadata/write-metadata (get-in request [:config :metadata :path]) crate)
    ;; create the categories
    (crate-file/save-crate-file (get-in request [:config :crate :path]) crate)
    (git/add (:git request))
    (apply git/commit (:git request) (msg/publish-commit-msg crate))
    (git/push (:git request))
    {:status 200
     :body {:warning {:invalid_categories []
                      :invalid_badges []
                      :other []}}}))

(defmethod crates-api! :andouillette
  [request]
  {:status 200
   :body (-> "static/andouillette.png" io/resource io/file)})
