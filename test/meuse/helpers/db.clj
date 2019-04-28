(ns meuse.helpers.db
  (:require [meuse.db.crate :as crate-db]
            [clojure.test :refer :all]))

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
  (let [crate (crate-db/get-crate database
                                  (:crate-name expected))]
    (is (uuid? (:crate-id crate)))
    (is (= (:crate-name expected) (:crate-name crate)))))
