(ns meuse.api.meuse.http
  (:require [meuse.api.default :as default]))

(def meuse-routes
  {#"/category/?" {:post ::new-category}
   [#"/category/?" :crate-name] {:delete ::delete-category}}
  #"/user/?" {:post ::new-user}
   [#"/user/?" :user-name] {:delete ::delete-user}})

(defmulti meuse-api!
  "Handle meuse API calls"
  :action)

(defmethod meuse-api! :default
  [request]
  default/not-found)
