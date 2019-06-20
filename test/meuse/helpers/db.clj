(ns meuse.helpers.db
  (:require [meuse.db.category :as category-db]
            [meuse.db.crate :as crate-db]
            [meuse.db.user :as user-db]
            [meuse.db.crate-user :as crate-user-db]
            [clojure.test :refer :all]))

(def db-state
  {:users [{:role "admin" :name "user1" :password "user1user1" :description "desc1" :active true}
           {:role "tech" :name "user2" :password "user2user2" :description "desc2" :active true}
           {:role "tech" :name "user3" :password "user3user3" :description "desc3" :active true}
           {:role "tech" :name "user4" :password "user4user4" :description "desc4" :active false}
           {:role "admin" :name "user5" :password "user5user5" :description "desc5" :active true}]
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
             :description "the crate2 description, this crate is for barbaz"}
            {:name "crate3"
             :vers "1.4.0"
             :yanked false
             :description "blablabla"}]
   :crates-users [{:crate "crate1"
                   :users ["user2" "user3"]}]})

(defn create-test-db!
  "Create entities in the test database."
  [database]
  (doseq [user (:users db-state)]
    (user-db/create-user database user))
  (let [user1 (user-db/get-user-by-name database "user1")]
    (doseq [category (:categories db-state)]
      (category-db/create database
                          (:name category)
                          (:description category)))
    (doseq [crate (:crates db-state)]
      (crate-db/create-crate database crate (:user-id user1)))
    (doseq [crate-user (:crates-users db-state)]
      (crate-user-db/create-crate-users database (:crate crate-user) (:users crate-user)))))

(defn test-crate-version
  "Takes a crate with its version, checks if the crate/version exists in the database."
  [database expected]
  (let [crate (crate-db/get-crate-and-version database
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
  "Takes a crate, checks if the crate exists in the database."
  [database expected]
  (let [crate (crate-db/get-crate-by-name database
                                          (:crate-name expected))]
    (is (uuid? (:crate-id crate)))
    (is (= (:crate-name expected) (:crate-name crate)))))
