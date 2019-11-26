(ns meuse.db.queries.crate-user
  (:require [honeysql.core :as sql]
            [honeysql.helpers :as h]))

(defn create
  [crate-id user-id]
  (-> (h/insert-into :crates_users)
      (h/columns :crate_id
                 :user_id)
      (h/values [[crate-id
                  user-id]])
      sql/format))

(defn delete
  [crate-id user-id]
  (-> (h/delete-from :crates_users)
      (h/where [:and
                [:= :crate_id crate-id]
                [:= :user_id user-id]])
      sql/format))

(defn delete-for-user
  [user-id]
  (-> (h/delete-from :crates_users)
      (h/where [:= :user_id user-id])
      sql/format))

(defn get-crate-user
  [where-clause]
  (-> (h/select :c.crate_id
                :c.user_id)
      (h/from [:crates_users :c])
      (h/where where-clause)
      sql/format))

(defn by-crate-and-user
  [crate-id user-id]
  (get-crate-user [:and
                   [:= :c.crate_id crate-id]
                   [:= :c.user_id user-id]]))

