(ns meuse.api.crate.yank
  (:require [meuse.api.crate.http :refer (crates-api!)]
            [meuse.db.crate :as crate-db]
            [meuse.git :as git]
            [meuse.metadata :as metadata]
            [meuse.message :as msg]
            [clojure.tools.logging :refer [debug info error]]))

(defn update-yank
  [request yanked?]
  (let [{:keys [crate-name crate-version]} (:route-params request)]
    (crate-db/update-yank (:database request) crate-name crate-version yanked?)
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
