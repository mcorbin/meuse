(ns meuse.api.mirror.http
  (:require [meuse.api.default :as default]))

(def mirror-routes
  {["/" :crate-name "/" :crate-version "/download"] {:get ::download}})

(defmulti mirror-api!
  "Handle crates.io api calls"
  :action)

(defmethod mirror-api! :default
  [request]
  (default/not-found request))
