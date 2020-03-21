(ns meuse.api.public.http
  (:require [meuse.api.default :as default]))

(defmulti public-api!
  "Handle meuse API calls"
  :action)

(defmethod public-api! :default
  [request]
  (default/not-found request))
