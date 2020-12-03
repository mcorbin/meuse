(ns meuse.api.meuse.http
  (:require [meuse.api.crate :as mac]
            [meuse.api.default :as default]))

(def skip-auth
  "Skip token auth for these calls."
  #{:create-token})

(def meuse-routes
  {#"/category/?" {:post ::new-category}
   #"/category/?" {:get ::list-categories}
   [#"/category/?" :name] {:post ::update-category}
   #"/user/?" {:post ::new-user}
   #"/user/?" {:get ::list-users}
   [#"/user/?" :name] {:delete ::delete-user}
   [#"/user/?" :name] {:post ::update-user}
   #"/token/?" {:post ::create-token}
   #"/token/?" {:get ::list-tokens}
   [#"/token/?"] {:delete ::delete-token}
   #"/crate/?" {:get ::list-crates}
   [#"/crate/?" [mac/crate-regex :name]] {:get ::get-crate}
   #"/check/?" {:get ::check-crates}
   #"/statistics/?" {:get ::statistics}})

(defmulti meuse-api!
  "Handle meuse API calls"
  :action)

(defmethod meuse-api! :default
  [request]
  (default/not-found request))
