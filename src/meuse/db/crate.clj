(ns meuse.db.crate
  (:require [clojure.java.jdbc :as jdbc]
            [clojure.tools.logging :refer [debug info error]]
            [honeysql.core :as sql]
            [honeysql.helpers :as h])
  (:import java.sql.Timestamp
           java.util.Date
           java.util.UUID))

(defn new-crate
  [request {:keys [metadata]}]
  (jdbc/with-db-transaction [db-tx (:database request)]
    (let [create-crate (-> (h/insert-into :crates)
                           (h/columns :id :name)
                           (h/values [[(UUID/randomUUID)
                                       (:name metadata)]])
                           sql/format)
          create-version (-> (h/insert-into :crate_versions)
                             (h/columns :id
                                        :version
                                        :description
                                        :yanked
                                        :created_at
                                        :document_vectors
                                        :crate_name)
                             (h/values [[(UUID/randomUUID)
                                         (:vers metadata)
                                         (:description metadata)
                                         (:yanked metadata)
                                         (new Timestamp (.getTime (new Date)))
                                         (sql/raw (format
                                                   (str
                                                    "("
                                                    "to_tsvector('%s') || "
                                                    "to_tsvector('%s')"
                                                    ")")
                                                   (:name metadata)
                                                   (:description metadata)
                                                   ))
                                         (:name metadata)
                                         ]])
                             sql/format
                             )
          ]
      (info create-version)
      (jdbc/execute! db-tx create-crate)
      (jdbc/execute! db-tx create-version)
      )

    )
  )

(comment
  (-> (h/insert-into :crate_versions)
      (h/columns :id
                 :version
                 :description
                 :yanked
                 :created_at
                 :crate_name)
      (h/values [["foo" "abar" (sql/call "foo")]])
      sql/format)
  )
