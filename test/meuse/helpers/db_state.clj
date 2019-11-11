(ns meuse.helpers.db-state
  (:require [meuse.db.actions.crate :as crate-db]
            [clojure.test :refer :all]))

(defn test-crate-version
  "Takes a crate with its version, checks if the crate/version exists in the database."
  [database expected]
  (let [crate (crate-db/by-name-and-version database
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
  (let [crate (crate-db/by-name database
                                (:crate-name expected))]
    (is (uuid? (:crate-id crate)))
    (is (= (:crate-name expected) (:crate-name crate)))))
