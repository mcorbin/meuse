(ns meuse.api.public.healthz
  (:require [meuse.api.public.http :refer [public-api!]]))

(def healthz-msg "Meuse is running.")

(defmethod public-api! :healthz
  [_]
  {:status 200
   :body healthz-msg})
