(ns meuse.front.http
  (:require [meuse.front.pages.login :as login-page]
            [ring.middleware.content-type :as ct]
            [ring.middleware.head :as head]
            [ring.util.codec :as codec]
            [ring.util.request :as request]
            [ring.util.response :as response]))

(def front-routes
  {#"/?" {:get ::index}
   #"/login/?" {:get ::loginpage}
   #"/logout/?" {:post ::logout}
   #"/login/?" {:post ::login}
   #"/search" {:get ::search}
   #"/categories" {:get ::categories}
   #"/crates" {:get ::crates}
   #"/tokens" {:get ::tokens}
   [#"/categories/?" :category] {:get ::crates-category}
   [#"/crates/?" :name] {:get ::crate}})

(defmulti front-api!
  "Handle crates API calls"
  :action)

(defmulti front-page!
  "Handle crates calls for HTML pages"
  :action)

(defmethod front-page! :default
  [_]
  {:status 404
   :headers {"Content-Type" "text/html; charset=utf-8"}
   :body ["NOT FOUND"]})

(defmethod front-api! :logout
  [_]
  {:status 302
   :headers {"Location" "/front/login"}
   :cookies {"session-token" {:value "logout"
                              :max-age 1}}})

(defmethod front-api! :loginpage
  [request]
  {:status 200
   :headers {"Content-Type" "text/html; charset=utf-8"}
   :body (login-page/page request)})

(defmethod front-api! :static
  [request]
  (when (#{:head :get} (:request-method request))
    (let [path (subs (codec/url-decode (request/path-info request)) 1)]
      (-> (response/resource-response path {})
          (ct/content-type-response request)
          (head/head-response request)))))
