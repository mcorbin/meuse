(ns meuse.auth.header)

(defn extract-token
  [request]
  (get-in request [:headers "Authorization"]))
