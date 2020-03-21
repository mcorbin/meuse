(ns meuse.db.queries.user
  (:require [meuse.auth.password :as password]
            [honeysql.core :as sql]
            [honeysql.helpers :as h])
  (:import java.util.UUID))

(defn get-user
  [where-clause]
  (-> (h/select :u.id
                :u.name
                :u.password
                :u.description
                :u.active
                :u.role_id)
      (h/from [:users :u])
      (h/where where-clause)
      sql/format))

(defn get-users-join-role
  []
  (-> (h/select :u.id
                :u.name
                :u.description
                :u.active
                :r.name)
      (h/from [:users :u])
      (h/join [:roles :r] [:= :u.role_id :r.id])
      sql/format))

(defn by-name
  [user-name]
  (get-user [:= :u.name user-name]))

(defn by-id
  [user-id]
  (get-user [:= :u.id (UUID/fromString user-id)]))

(defn create
  [user role-id]
  (-> (h/insert-into :users)
      (h/columns :id
                 :name
                 :password
                 :description
                 :active
                 :role_id)
      (h/values [[(UUID/randomUUID)
                  (:name user)
                  (password/encrypt (:password user))
                  (:description user)
                  (:active user false)
                  role-id]])
      sql/format))

(defn delete
  [user-id]
  (-> (h/delete-from :users)
      (h/where [:= :id user-id])
      sql/format))

(defn users-join-crates-users
  [crate-id]
  (-> (h/select :u.id
                :u.name
                :u.cargo_id
                :c.crate_id)
      (h/from [:users :u])
      (h/join [:crates_users :c] [:= :c.user_id :u.id])
      (h/where [:= :c.crate_id crate-id])
      sql/format))

(defn update-user
  [user-id fields]
  (-> (h/update :users)
      (h/sset fields)
      (h/where [:= :id user-id])
      sql/format))

(defn count-users
  []
  (-> (h/select :%count.*)
      (h/from [:users :c])
      sql/format))
