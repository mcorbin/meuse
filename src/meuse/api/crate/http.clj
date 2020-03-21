(ns meuse.api.crate.http
  (:require [meuse.api.default :as default]))

(def skip-auth
  "Skip token auth for these calls."
  #{:search :download})

(def crates-routes
  {#"/new/?" {:put ::new}
   #"/?" {:get ::search}
   ["/" :crate-name #"/owners/?"] {:put ::add-owner}
   ["/" :crate-name #"/owners/?"] {:delete ::remove-owner}
   ["/" :crate-name #"/owners/?"] {:get ::list-owners}
   ["/" :crate-name "/" :crate-version "/yank"] {:delete ::yank}
   ["/" :crate-name "/" :crate-version "/unyank"] {:put ::unyank}
   ["/" :crate-name "/" :crate-version "/download"] {:get ::download}
   #"/andouillette" ::andouillette})

(defmulti crates-api!
  "Handle crates API calls"
  :action)

(defmethod crates-api! :default
  [request]
  (default/not-found request))
