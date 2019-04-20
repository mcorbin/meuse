(ns meuse.api.default)

(def not-found {:status 404
                :body {:errors
                       [{:detail "not found"}]}})

(defmulti default-api!
  :action)

(defmethod default-api! :not-found
  [_]
  not-found)
