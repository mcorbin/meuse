(ns meuse.api.crate.http
  (:require [meuse.api.default :as default]))

(def crates-routes
  {#"/new/?" {:put ::new}
   ["/" :crate-name "/" :crate-version "/yank"] {:delete ::yank}
   ["/" :crate-name "/" :crate-version "/unyank"] {:put ::unyank}
   #"/andouillette" ::andouillette})

(defmulti crates-api!
  "Handle crates API calls"
  :action)

(defmethod crates-api! :default
  [request]
  default/not-found)
