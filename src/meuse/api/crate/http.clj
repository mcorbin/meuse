(ns meuse.api.crate.http
  (:require [meuse.api.default :as default]
            [meuse.api.crate :as mac]))

(def skip-auth
  "Skip token auth for these calls."
  #{:search :download})

(def crates-routes
  {#"/new/?" {:put ::new}
   #"/?" {:get ::search}
   ["/" mac/crate-name-path #"/owners/?"] {:put ::add-owner}
   ["/" mac/crate-name-path #"/owners/?"] {:delete ::remove-owner}
   ["/" mac/crate-name-path #"/owners/?"] {:get ::list-owners}
   ["/" mac/crate-name-path "/" mac/crate-version-path "/yank"] {:delete ::yank}
   ["/" mac/crate-name-path "/" mac/crate-version-path "/unyank"] {:put ::unyank}
   ["/" mac/crate-name-path "/" mac/crate-version-path "/download"] {:get ::download}
   #"/andouillette" ::andouillette})

(defmulti crates-api!
  "Handle crates API calls"
  :action)

(defmethod crates-api! :default
  [request]
  (default/not-found request))
