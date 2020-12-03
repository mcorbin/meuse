(ns meuse.api.mirror.http
  (:require [meuse.api.crate :as mac]
            [meuse.api.default :as default]))

(def skip-auth
  "Skip token auth for these calls."
  #{:download})

(def mirror-routes
  {["/" mac/crate-name-path "/" mac/crate-version-path "/download"] {:get ::download}
   ["/" mac/crate-name-path "/" mac/crate-version-path "/cache"] {:post ::cache}})

(defmulti mirror-api!
  "Handle crates.io api calls"
  :action)

(defmethod mirror-api! :default
  [request]
  (default/not-found request))
