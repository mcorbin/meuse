(ns meuse.helpers.db
  (:require [meuse.db.category :as category-db]
            [meuse.db.crate :as crate-db]
            [meuse.db.user :as user-db]
            [clojure.test :refer :all]))


;; users

(def db-state
  {:users [{:role "admin" :name "user1" :password "user1" :description "desc1"}
           {:role "tech" :name "user2" :password "user2" :description "desc2"}
           {:role "tech" :name "user3" :password "user3" :description "desc3"}]
   :categories [{:name "email"
                 :description "the email category"}
                {:name "system"
                 :description "the system category"}]
   :crates [{:name "crate1"
             :vers "1.1.0"
             :yanked false
             :description "the crate1 description, this crate is for foobar"
             :categories ["email" "system"]}
            {:name "crate1"
             :vers "1.1.4"
             :yanked false
             :description "the crate1 description, this crate is for foobar"
             :categories ["email" "system"]}
            {:name "crate1"
             :vers "1.1.5"
             :yanked true
             :description "the crate1 description, this crate is for foobar"
             :categories ["email" "system"]}
            {:name "crate2"
             :vers "1.3.0"
             :yanked false
             :description "the crate2 description, this crate is for barbaz"}]
   :crates-users [{:crate "crate1"
                   :users ["user2" "user3"]}]})

;; creates_users

(defn create-test-db!
  "Create entities in the test database."
  [database]
  (doseq [user (:users db-state)]
    (user-db/create-user database user))
  (doseq [category (:categories db-state)]
    (category-db/create-category database
                                 (:name category)
                                 (:description category)))
  (doseq [crate (:crates db-state)]
    (crate-db/create-crate database crate))
  (doseq [crate-user (:crates-users db-state)]
    (user-db/create-crate-users database (:crate crate-user) (:users crate-user))))

(defn test-crate-version
  [database expected]
  (let [crate (crate-db/get-crate-version database
                                          (:crate-name expected)
                                          (:version-version expected))]
      (is (uuid? (:crate-id crate)))
      (is (uuid? (:version-id crate)))
      (is (inst? (:version-created-at crate)))
      (is (inst? (:version-updated-at crate)))
      (are [x y] (= x y)
        (:crate-name expected) (:crate-name crate)
        (:version-version expected) (:version-version crate)
        (:version-yanked expected) (:version-yanked crate)
        (:version-description expected) (:version-description crate)
        (:crate-id crate) (:version-crate-id crate))))

(defn test-crate
  [database expected]
  (let [crate (crate-db/get-crate-by-name database
                                          (:crate-name expected))]
    (is (uuid? (:crate-id crate)))
    (is (= (:crate-name expected) (:crate-name crate)))))
