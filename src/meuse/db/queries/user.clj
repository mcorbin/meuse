(ns meuse.db.queries.user
  (:require [honeysql.core :as sql]
            [honeysql.helpers :as h]
            [meuse.auth.password :as password])
  (:import java.util.UUID))

(defn get-user-by-name
  [user-name]
  (-> (h/select [:u.id "user_id"]
                [:u.name "user_name"]
                [:u.password "user_password"]
                [:u.description "user_description"]
                [:u.active "user_active"]
                [:u.role_id "user_role_id"])
      (h/from [:users :u])
      (h/where [:= :u.name user-name])
      sql/format))

(defn create-user
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

(defn delete-user
  [user-id]
  (-> (h/delete-from :users)
      (h/where [:= :id user-id])
      sql/format))

(defn create-crate-user
  [crate-id user-id]
  (-> (h/insert-into :crates_users)
      (h/columns :crate_id
                 :user_id)
      (h/values [[crate-id
                  user-id]])
      sql/format))

(defn delete-crate-user
  [crate-id user-id]
  (-> (h/delete-from :crates_users)
      (h/where [:and
                [:= :crate_id crate-id]
                [:= :user_id user-id]])
      sql/format))

(defn delete-crates-user
  [user-id]
  (-> (h/delete-from :crates_users)
      (h/where [:= :user_id user-id])
      sql/format))

(defn get-crate-user
  [crate-id user-id]
  (-> (h/select [:c.crate_id "crate_id"]
                [:c.user_id "user_id"])
      (h/from [:crates_users :c])
      (h/where [:and
                [:= :c.crate_id crate-id]
                [:= :c.user_id user-id]])
      sql/format))

(defn get-crate-join-crates-users
  [crate-id]
  (-> (h/select [:u.id "user_id"]
                [:u.name "user_name"]
                [:u.cargo_id "user_cargo_id"]
                [:c.crate_id "crate_id"])
      (h/from [:users :u])
      (h/join [:crates_users :c] [:= :c.user_id :u.id])
      (h/where [:= :c.crate_id crate-id])
      sql/format))
