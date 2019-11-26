(ns meuse.helpers.db-state
  (:require [meuse.db.actions.crate :as crate-db]
            [clojure.test :refer :all]))

(defn test-crate-version
  "Takes a crate with its version, checks if the crate/version exists in the database."
  [database expected]
  (let [crate (crate-db/by-name-and-version database
                                            (:crates/name expected)
                                            (:crates_versions/version expected))]
    (is (uuid? (:crates/id crate)))
    (is (uuid? (:crates_versions/id crate)))
    (is (inst? (:crates_versions/created_at crate)))
    (is (inst? (:crates_versions/updated_at crate)))
    (are [x y] (= x y)
      (:crates/name expected) (:crates/name crate)
      (:crates_versions/version expected) (:crates_versions/version crate)
      (:crates_versions/yanked expected) (:crates_versions/yanked crate)
      (:crates_versions/description expected) (:crates_versions/description crate)
      (:crates/id crate) (:crates_versions/crate_id crate))))

(defn test-crate
  "Takes a crate, checks if the crate exists in the database."
  [database expected]
  (let [crate (crate-db/by-name database
                                (:crates/name expected))]
    (is (uuid? (:crates/id crate)))
    (is (= (:crates/name expected) (:crates/name crate)))))
