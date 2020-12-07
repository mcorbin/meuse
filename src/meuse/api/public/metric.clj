(ns meuse.api.public.metric
  (:require [meuse.api.public.http :refer [public-api!]]
            [meuse.metric :as metric]))

(defmethod public-api! :metrics
  [_]
  {:status 200
   :headers {"Content-Type" "text/plain"}
   :body (.getBytes (.scrape metric/registry))})
