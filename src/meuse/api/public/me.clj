(ns meuse.api.public.me
  (:require [meuse.api.public.http :refer [public-api!]]))

(def me-msg "Please consult the documentation to find how to generate a token. https://meuse.mcorbin.fr/")

(defmethod public-api! :me
  [_]
  {:status 200
   :body me-msg})
