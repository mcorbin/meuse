(ns meuse.api.default)

(def not-found {:status 404
                :body {:errors
                       [{:detail "not found"}]}})

(defmulti default-api!
  :action)

(defmethod default-api! :not-found
  [_]
  not-found)

(defmethod default-api! :me
  [_]
  {:status 200
   :body "Please consult the documentation to find how to generate a token."})
