(ns meuse.api.crates.http
  (:require [meuse.db.crate :as dbc]
            [meuse.crate :as crate]
            [meuse.api.default :as default]
            [meuse.git :as git]
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
    (dbc/new-crate request crate)
    (git/add-crate (:git request) crate)
    (git/add (:git request))
    (apply git/commit (:git request) (crate/publish-commit-msg crate))
    (git/push (:git request))
    (crate/save-crate-file (get-in request [:crate-config :path]) crate)
    {:status 200
     :body {:warning {:invalid_categories []
                      :invalid_badges []
                      :other []}}}))

(defn update-yank
  [request yanked?]
  (let [{:keys [crate-name crate-version]} (:route-params request)]
    (dbc/update-yank request crate-name crate-version yanked?)
    (git/update-yank (:git request) crate-name crate-version yanked?)
    (git/add (:git request))
    (apply git/commit (:git request) (crate/yank-commit-msg
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
