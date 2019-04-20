(ns meuse.api.crates.http
  (:require [meuse.crate :as crate]
            [meuse.api.default :as default]
            [meuse.git :as git]
            [clojure.tools.logging :refer [debug info error]]))

(def crates-routes
  {#"/new/?" {:put ::new
              :get {"/" ::aaa}}})

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
    (git/add-crate (:git request) crate)
    (git/add (:git request))
    (git/commit (:git request) crate)
    (git/push (:git request))
    {:status 200
     :body {:warning {:invalid_categories []
                      :invalid_badges []
                      :other []}}}))

(defmethod crates-api! :default
  [request]
  default/not-found)
