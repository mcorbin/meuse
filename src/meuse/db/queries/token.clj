(ns meuse.db.queries.token
  (:require [honeysql.core :as sql]
            [honeysql.helpers :as h])
  (:import java.util.Date
           java.util.UUID
           java.sql.Timestamp
           org.joda.time.DateTime))

(defn create
  [identifier token token-name user-id expired-at]
  (let [now (new Timestamp (.getTime (new Date)))]
    (-> (h/insert-into :tokens)
        (h/columns :id
                   :identifier
                   :name
                   :token
                   :created_at
                   :expired_at
                   :user_id)
        (h/values [[(UUID/randomUUID)
                    identifier
                    token-name
                    token
                    now
                    (new Timestamp (.getMillis ^DateTime expired-at))
                    user-id]])
        sql/format)))

(defn get-token
  [where-clause]
  (-> (h/select :t.id
                :t.identifier
                :t.token
                :t.name
                :t.created_at
                :t.expired_at
                :t.user_id)
      (h/from [:tokens :t])
      (h/where where-clause)
      sql/format))

(defn by-user-and-name
  [user-id token-name]
  (get-token [:and
              [:= :t.user_id user-id]
              [:= :t.name token-name]]))

(defn by-user
  [user-id]
  (-> (h/select :t.id
                :t.identifier
                :t.token
                :t.name
                :t.last_used_at
                :t.created_at
                :t.expired_at
                :t.user_id)
      (h/from [:tokens :t])
      (h/where [:= :t.user_id user-id])
      sql/format))

(defn delete
  [token-id]
  (-> (h/delete-from :tokens)
      (h/where [:= :id token-id])
      sql/format))

(defn token-join-user-join-role
  [identifier]
  (-> (h/select :t.id
                :t.identifier
                :t.token
                :t.name
                :t.created_at
                :t.expired_at
                :t.user_id
                :u.name
                :u.active
                :u.role_id
                :r.name)
      (h/from [:tokens :t])
      (h/join [:users :u] [:= :t.user_id :u.id]
              [:roles :r] [:= :u.role_id :r.id])
      (h/where [:= :t.identifier identifier])
      sql/format))

(defn set-last-used
  [token-id]
  (-> (h/update :tokens)
      (h/sset {:last_used_at (new Timestamp (.getTime (new Date)))})
      (h/where [:= :id token-id])
      sql/format))
