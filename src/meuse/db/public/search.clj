(ns meuse.db.public.search
  (:require [meuse.db :refer [database]]
            [meuse.db.actions.search :as search]
            [mount.core :refer [defstate]]))

(defprotocol ISearchDB
  (search [this query-string]))

(defrecord SearchDB [database]
  ISearchDB
  (search [this query-string]
    (search/search database query-string)))

(defstate search-db
  :start (SearchDB. database))
