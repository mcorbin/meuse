(ns meuse.front.http
  (:require [meuse.api.default :as default]
            [ring.middleware.head :as head]
            [ring.util.codec :as codec]
            [ring.util.request :as request]
            [ring.util.response :as response]
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

(defmethod front-api! :static
  [request]
  (when (#{:head :get} (:request-method request))
    (let [path (subs (codec/url-decode (request/path-info request)) 1)]
      (-> (response/resource-response path {})
          (head/head-response request)))))
