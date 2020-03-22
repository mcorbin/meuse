(ns meuse.log
  "Functions for structured logging"
  (:require [unilog.context :as context]
            [clojure.tools.logging :as log]))

(defmacro info
  [data & args]
  `(context/with-context~ data
    (log/info ~@args)))

(defmacro debug
  [data & args]
  `(context/with-context~ data
    (log/debug ~@args)))

(defmacro error
  [data & args]
  `(context/with-context~ data
    (log/error ~@args)))
