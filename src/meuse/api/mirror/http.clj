(ns meuse.api.mirror.http
  (:require [meuse.api.default :as default]))

(def skip-auth
  "Skip token auth for these calls."
  #{:download})

(def mirror-routes
  {["/" :crate-name "/" :crate-version "/download"] {:get ::download}
   ["/" :crate-name "/" :crate-version "/cache"] {:post ::cache}})

(defmulti mirror-api!
  "Handle crates.io api calls"
  :action)

(defmethod mirror-api! :default
  [request]
  (default/not-found request))
