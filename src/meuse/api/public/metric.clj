(ns meuse.api.public.metric
  (:require [meuse.api.public.http :refer [public-api!]]
            [meuse.metric :as metric]))

(defmethod public-api! :metrics
  [request]
  {:status 200
   :body (.getBytes (.scrape metric/registry))})
