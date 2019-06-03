(ns meuse.auth.header)

(defn extract-token
  "Takes a request. Extracts the Authorization header."
  [request]
  (get-in request [:headers "authorization"]))
