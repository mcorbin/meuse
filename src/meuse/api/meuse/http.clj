(ns meuse.api.meuse.http
  (:require [meuse.api.default :as default]
            [clojure.tools.logging :refer [info warn error]]))

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
   #"/token/?" {:get ::list-token}
   [#"/token/?"] {:delete ::delete-token}
   #"/crate/?" {:get ::list-crates}
   [#"/crate/?" :name] {:get ::get-crate}
   #"/check/?" {:get ::check-crates}})

(defmulti meuse-api!
  "Handle meuse API calls"
  :action)

(defmethod meuse-api! :default
  [request]
  (info "meuse uri not found:" (:request-method request) (:uri request))
  default/not-found)
