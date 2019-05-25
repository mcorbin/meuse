(ns meuse.api.meuse.token-test
  (:require [meuse.api.meuse.http :refer :all]
            [meuse.db :refer [database]]
            [meuse.db.token :as token-db]
            [meuse.helpers.fixtures :refer :all]
            [clojure.test :refer :all]))

(use-fixtures :once db-fixture)
(use-fixtures :each table-fixture)

(deftest ^:integration delete-token-test
  (token-db/create-token database {:user "user2"
                                   :validity 10
                                   :name "mytoken"})
  (is (= 1 (count (token-db/get-user-tokens database "user2"))))
  (= {:status 200} (meuse-api! {:database database
                                :action :delete-token
                                :body {:name "mytoken"
                                       :user-name "user2"}}))
  (is (= 0 (count (token-db/get-user-tokens database "user2")))))

