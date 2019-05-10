(ns meuse.db.queries.token
  (:require [honeysql.core :as sql]
            [honeysql.helpers :as h])
  (:import java.sql.Timestamp
           java.util.Date
           org.joda.time.DateTime
           java.util.UUID))

(defn create-token
  [token user-id expired-at]
  (let [now (new Timestamp (.getTime (new Date)))]
    (-> (h/insert-into :tokens)
        (h/columns :id
                   :token
                   :created_at
                   :expired_at
                   :user_id)
        (h/values [[(UUID/randomUUID)
                    token
                    now
                    (new Timestamp (.getMillis ^DateTime expired-at))
                    user-id]])
        sql/format)))

(defn get-user-tokens
  [user-id]
  (-> (h/select [:t.id "token_id"]
                [:t.token "token_token"]
                [:t.created_at "token_created_at"]
                [:t.expired_at "token_expired_at"]
                [:t.user_id "token_user_id"])
      (h/from [:tokens :t])
      (h/where [:= :t.user_id user-id])
      sql/format))
