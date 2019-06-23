(ns meuse.db.queries.user
  (:require [honeysql.core :as sql]
            [honeysql.helpers :as h]
            [meuse.auth.password :as password])
  (:import java.util.UUID))

(defn get-user
  [where-clause]
  (-> (h/select [:u.id "user_id"]
                [:u.name "user_name"]
                [:u.password "user_password"]
                [:u.description "user_description"]
                [:u.active "user_active"]
                [:u.role_id "user_role_id"])
      (h/from [:users :u])
      (h/where where-clause)
      sql/format))

(defn by-name
  [user-name]
  (get-user [:= :u.name user-name]))

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
  (-> (h/select [:u.id "user_id"]
                [:u.name "user_name"]
                [:u.cargo_id "user_cargo_id"]
                [:c.crate_id "crate_id"])
      (h/from [:users :u])
      (h/join [:crates_users :c] [:= :c.user_id :u.id])
      (h/where [:= :c.crate_id crate-id])
      sql/format))
