(ns meuse.db.queries.token
  (:require [honeysql.core :as sql]
            [honeysql.helpers :as h])
  (:import java.sql.Timestamp
           java.util.Date
           org.joda.time.DateTime
           java.util.UUID))

(defn create-token
  [token token-name user-id expired-at]
  (let [now (new Timestamp (.getTime (new Date)))]
    (-> (h/insert-into :tokens)
        (h/columns :id
                   :name
                   :token
                   :created_at
                   :expired_at
                   :user_id)
        (h/values [[(UUID/randomUUID)
                    token-name
                    token
                    now
                    (new Timestamp (.getMillis ^DateTime expired-at))
                    user-id]])
        sql/format)))

(defn get-user-token
  [user-id token-name]
  (-> (h/select [:t.id "token_id"]
                [:t.token "token_token"]
                [:t.name "token_name"]
                [:t.created_at "token_created_at"]
                [:t.expired_at "token_expired_at"]
                [:t.user_id "token_user_id"])
      (h/from [:tokens :t])
      (h/where [:and
                [:= :t.name token-name]
                [:= :t.user_id user-id]])
      sql/format))

(defn get-user-tokens
  [user-id]
  (-> (h/select [:t.id "token_id"]
                [:t.token "token_token"]
                [:t.name "token_name"]
                [:t.created_at "token_created_at"]
                [:t.expired_at "token_expired_at"]
                [:t.user_id "token_user_id"])
      (h/from [:tokens :t])
      (h/where [:= :t.user_id user-id])
      sql/format))

(defn delete-token
  [token-id]
  (-> (h/delete-from :tokens)
      (h/where [:= :id token-id])
      sql/format))
