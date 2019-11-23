(ns meuse.front.http
  (:require [meuse.api.default :as default]
            [clojure.tools.logging :as log]))

(def front-routes
  {#"/?" {:get ::index}
   #"/search" {:get ::search}
   #"/categories" {:get ::categories}
   #"/crates" {:get ::crates}
   [#"/categories/?" :category] {:get ::crates-category}
   [#"/crates/?" :name] {:get ::crate}})

(defmulti front-api!
  "Handle crates API calls"
  :action)

(defmethod front-api! :default
  [request]
  [:p "NOT FOUND"])
