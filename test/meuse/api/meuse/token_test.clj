(ns meuse.api.meuse.token-test
  (:require [meuse.db.token :as token-db]
            [meuse.helpers.fixtures :refer :all]
            [clojure.test :refer :all]))

(use-fixtures :once db-fixture)
(use-fixtures :each table-fixture)

(deftest ^:integration new-token-test
  )

