(ns meuse.api.crate
  (:require [meuse.db.crate :as crate-db]
            [meuse.crate :as crate]
            [meuse.crate-file :as crate-file]
            [meuse.api.default :as default]
            [meuse.git :as git]
            [meuse.metadata :as metadata]
            [meuse.message :as msg]
            [clojure.tools.logging :refer [debug info error]]))

(def crates-routes
  {#"/new/?" {:put ::new}
   ["/" :crate-name "/" :crate-version "/yank"] {:delete ::yank}
   ["/" :crate-name "/" :crate-version "/unyank"] {:put ::unyank}})

(defmulti crates-api!
  "Handle crates API calls"
  :action)

(defmethod crates-api! :new
  [request]
  (info "received new crate request" (:request-id request))
  (let [{:keys [metadata crate-file] :as crate}
        (crate/request->crate request)]
    (info "publishing crate" (:name metadata)
          "version" (:vers metadata))
    (crate-db/new-crate request crate)
    (metadata/write-metadata (get-in request [:config :metadata :path]) crate)
    (crate-file/save-crate-file (get-in request [:config :crate :path]) crate)
    (git/add (:git request))
    (apply git/commit (:git request) (msg/publish-commit-msg crate))
    (git/push (:git request))
    {:status 200
     :body {:warning {:invalid_categories []
                      :invalid_badges []
                      :other []}}}))

(defn update-yank
  [request yanked?]
  (let [{:keys [crate-name crate-version]} (:route-params request)]
    (crate-db/update-yank request crate-name crate-version yanked?)
    (metadata/update-yank (get-in request [:config :metadata :path])
                          crate-name
                          crate-version
                          yanked?)
    (git/add (:git request))
    (apply git/commit (:git request) (msg/yank-commit-msg
                                      crate-name
                                      crate-version
                                      yanked?))
    (git/push (:git request))
    {:status 200
     :body {:ok true}}))

(defmethod crates-api! :yank
  [request]
  (update-yank request true))

(defmethod crates-api! :unyank
  [request]
  (update-yank request false))

(defmethod crates-api! :default
  [request]
  default/not-found)
