(ns meuse.api.crate.download-test
  (:require [meuse.api.crate.download :refer :all]
            [meuse.api.crate.http :refer [crates-api!]]
            [meuse.crate-file :as crate-file]
            [meuse.helpers.fixtures :refer :all]
            [clojure.test :refer :all]))

(use-fixtures :each tmp-fixture)

(deftest crate-api-download-test
  (crate-file/save-crate-file tmp-dir
                              {:raw-metadata {:name "foo"
                                              :vers "1.0.0"}
                               :crate-file (.getBytes "file content")})
  (is (= (slurp (:body (crates-api! {:action :download
                                     :config {:crate {:path tmp-dir}}
                                     :route-params {:crate-name "foo"
                                                    :crate-version "1.0.0"}})))
         "file content")))
