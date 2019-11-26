(ns meuse.helpers.db
  (:require [meuse.db.actions.category :as category-db]
            [meuse.db.actions.crate :as crate-db]
            [meuse.db.actions.user :as user-db]
            [meuse.db.actions.crate-user :as crate-user-db]
            [next.jdbc :as jdbc]
            [clojure.java.shell :as shell]
            [clojure.test :refer :all]))

;; pg_dump -a -h localhost -p 5432 -U meuse
(comment
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
               :keywords ["keyword1"]
               :description "blablabla"}]
     :crates-users [{:crate "crate1"
                     :users ["user2" "user3"]}]})

  (defn create-test-db!
    "Create entities in the test database."
    [database]
    (doseq [user (:users db-state)]
      (user-db/create database user))
    (let [user1 (user-db/by-name database "user1")]
      (doseq [category (:categories db-state)]
        (category-db/create database
                            (:name category)
                            (:description category)))
      (doseq [crate (:crates db-state)]
        (crate-db/create database crate (:user-id user1)))
      (doseq [crate-user (:crates-users db-state)]
        (crate-user-db/create-crate-users database (:crate crate-user) (:users crate-user))))))

(def dump-file "test/resources/db/db_test.sql")

(defn load-test-db!
  "Load the SQL dump."
  [database]
  (let [result (shell/sh "psql" "-h" "localhost" "-d" "meuse" "-p" "5432" "-U" "meuse" "-f" dump-file
                         :env {"PGPASSWORD" "meuse"})]
    (when-not (= 0 (:exit result))
      (throw (ex-info "error executing psql command"
                      {:status 500
                       :exit-code (:exit result)
                       :stdout (:out result)
                       :stderr (:err result)})))))

(defn clean!
  [database]
  (jdbc/execute! database ["TRUNCATE TABLE CRATES CASCADE;"])
  (jdbc/execute! database ["TRUNCATE TABLE ROLES CASCADE;"])
  (jdbc/execute! database ["TRUNCATE TABLE CATEGORIES CASCADE;"])
  (jdbc/execute! database ["TRUNCATE TABLE TOKENS CASCADE;"])
  (jdbc/execute! database ["TRUNCATE TABLE USERS CASCADE;"])
  (jdbc/execute! database ["TRUNCATE TABLE CRATES_USERS CASCADE;"]))
