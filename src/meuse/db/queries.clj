(ns meuse.db.queries
  "Database queries."
  (:require [honeysql.core :as sql]
            [honeysql.helpers :as h]
            [honeysql.format :refer [fn-handler to-sql expand-binary-ops]]
            [meuse.auth.password :as password])
  (:import java.sql.Timestamp
           java.util.Date
           java.util.UUID))

;; todo: split in multiple ns
;; crate

(defn update-yanked
  [version-id yanked?]
  (-> (h/update :crate_versions)
      (h/sset {:yanked yanked?})
      (h/where [:= :id version-id])
      sql/format))

(defn get-crate-by-name
  [crate-name]
  (-> (h/select [:c.id "crate_id"]
                [:c.name "crate_name"])
      (h/from [:crates :c])
      (h/where [:= :c.name crate-name])
      sql/format))

(defn get-crate-join-version
  [crate-name crate-version]
  (-> (h/select [:c.id "crate_id"]
                [:c.name "crate_name"]
                [:v.id "version_id"]
                [:v.version "version_version"]
                [:v.description "version_description"]
                [:v.yanked "version_yanked"]
                [:v.created_at "version_created_at"]
                [:v.updated_at "version_updated_at"]
                [:v.document_vectors "version_document_vectors"]
                [:v.crate_id "version_crate_id"])
      (h/from [:crates :c])
      (h/left-join [:crate_versions :v] [:and
                                         [:= :c.id :v.crate_id]
                                         [:= :v.version crate-version]])
      (h/where [:= :c.name crate-name])
      sql/format))

(defn create-crate
  [metadata crate-id]
  (-> (h/insert-into :crates)
      (h/columns :id :name)
      (h/values [[crate-id
                  (:name metadata)]])
      sql/format))

(defn create-version
  [metadata crate-id]
  (let [now (new Timestamp (.getTime (new Date)))]
    (-> (h/insert-into :crate_versions)
        (h/columns :id
                   :version
                   :description
                   :yanked
                   :created_at
                   :updated_at
                   :crate_id
                   :document_vectors
                   )
        (h/values [[(UUID/randomUUID)
                    (:vers metadata)
                    (:description metadata)
                    (:yanked metadata false)
                    now
                    now
                    crate-id
                    (sql/raw (str
                              "("
                              "to_tsvector(?) || "
                              "to_tsvector(?)"
                              ")")
                              )]])
        sql/format
        (conj (:name metadata))
        (conj (:description metadata)))))

;; categories

(defn get-category-by-name
  [category-name]
  (-> (h/select [:c.id "category_id"]
                [:c.name "category_name"]
                [:c.description "category_description"])
      (h/from [:categories :c])
      (h/where [:= :c.name category-name])
      sql/format))

(defn get-crate-category
  [crate-id category-id]
  (-> (h/select [:c.crate_id "crate_id"]
                [:c.category_id "category_id"])
      (h/from [:crate_categories :c])
      (h/where [:and
                [:= :c.crate_id crate-id]
                [:= :c.category_id category-id]])
      sql/format))

(defn get-crate-categories
  [crate-id]
  (-> (h/select [:c.id "category_id"]
                [:c.name "category_name"]
                [:c.description "category_description"])
      (h/from [:categories :c])
      (h/left-join [:crate_categories :cc]
                   [:and
                    [:= :cc.category_id :c.id]
                    [:= :cc.crate_id crate-id]])
      sql/format))

(defn create-category
  [category-name description]
  (-> (h/insert-into :categories)
      (h/columns :id
                 :name
                 :description)
      (h/values [[(UUID/randomUUID)
                  category-name
                  description]])
      sql/format))

(defn create-crate-category
  [crate-id category-id]
  (-> (h/insert-into :crate_categories)
      (h/columns :crate_id
                 :category_id)
      (h/values [[crate-id
                  category-id]])
      sql/format))

;; users and roles and owners

(defn get-role-by-name
  [role-name]
  (-> (h/select [:r.id "role_id"]
                [:r.name "role_name"])
      (h/from [:roles :r])
      (h/where [:= :r.name role-name])
      sql/format))

(defn get-user-by-name
  [user-name]
  (-> (h/select [:u.id "user_id"]
                [:u.name "user_name"]
                [:u.password "user_password"]
                [:u.description "user_description"]
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
                 :role_id)
      (h/values [[(UUID/randomUUID)
                  (:name user)
                  (password/encrypt (:password user))
                  (:description user)
                  role-id]])
      sql/format))

(defn create-crate-user
  [crate-id user-id]
  (-> (h/insert-into :crate_users)
      (h/columns :crate_id
                 :user_id)
      (h/values [[crate-id
                  user-id]])
      sql/format))

(defn delete-crate-user
  [crate-id user-id]
  (-> (h/delete-from :crate_users)
      (h/where [:and
                [:= :crate_id crate-id]
                [:= :user_id user-id]])
      sql/format))

(defn get-crate-user
  [crate-id user-id]
  (-> (h/select [:c.crate_id "crate_id"]
                [:c.user_id "user_id"])
      (h/from [:crate_users :c])
      (h/where [:and
                [:= :c.crate_id crate-id]
                [:= :c.user_id user-id]])
      sql/format))

(defn get-crate-users
  [crate-id]
  (-> (h/select [:u.id "user_id"]
                [:u.name "user_name"]
                [:u.cargo_id "user_cargo_id"]
                [:c.crate_id "crate_id"])
      (h/from [:users :u])
      (h/join [:crate_users :c] [:= :c.user_id :u.id])
      (h/where [:= :c.crate_id crate-id])
      sql/format))

;; search

;; tsvector_matches
(defmethod fn-handler "@@"
  [_ a b & more]
  (if (seq more)
    (expand-binary-ops "@@" a b more)
    (str (to-sql a) " @@ " (to-sql b))))

(defn search-crates
  [query]
  (-> (h/select [:c.id "crate_id"]
                [:c.name "crate_name"]
                [:v.id "version_id"]
                [:v.version "version_version"]
                [:v.description "version_description"]
                [:v.yanked "version_yanked"]
                [:v.created_at "version_created_at"]
                [:v.updated_at "version_updated_at"]
                [:v.document_vectors "version_document_vectors"]
                [:v.crate_id "version_crate_id"])
      (h/from [:crates :c])
      (h/join [:crate_versions :v] [:and
                                    [:= :c.id :v.crate_id]
                                    (sql/raw "document_vectors @@ to_tsquery(?)")])
      sql/format
      (conj query)))

