(ns meuse.api.public.healthz
  (:require [meuse.api.public.http :refer [public-api!]]))

(def healthz-msg "Maizey is running.")

(defmethod public-api! :healthz
  [request]
  {:status 200
   :body healthz-msg})
