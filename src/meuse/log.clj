(ns meuse.log
  "Functions for structured logging"
  (:require [unilog.context :as context]
            [clojure.tools.logging :as log]))

(defmacro info
  [data & args]
  `(context/with-context~ data
    (log/info ~@args)))

(defmacro infof
  [data & args]
  `(context/with-context~ data
    (log/infof ~@args)))

(defmacro debug
  [data & args]
  `(context/with-context~ data
    (log/debug ~@args)))

(defmacro error
  [data & args]
  `(context/with-context~ data
    (log/error ~@args)))

(defn req-ctx
  [request]
  (cond-> {:request-id (:id request)}
    (get-in request [:auth :user-name]) (assoc :user (get-in request [:auth :user-name]))
    (get-in request [:auth :user-id]) (assoc :user (get-in request [:auth :user-id]))))
