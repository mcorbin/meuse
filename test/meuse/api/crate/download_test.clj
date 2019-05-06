(ns meuse.api.crate.download-test
  (:require [meuse.api.crate.download :refer :all]
            [meuse.api.crate.http :refer [crates-api!]]
            [meuse.crate-file :as crate-file]
            [meuse.helpers.fixtures :refer :all]
            [clojure.test :refer :all])
  (:import clojure.lang.ExceptionInfo))

(use-fixtures :each tmp-fixture)

(deftest crate-api-download-test
  (testing "success"
    (crate-file/write-crate-file tmp-dir
                                 {:raw-metadata {:name "foo"
                                                 :vers "1.0.0"}
                                  :crate-file (.getBytes "file content")}))
  (is (= (slurp (:body (crates-api! {:action :download
                                     :config {:crate {:path tmp-dir}}
                                     :route-params {:crate-name "foo"
                                                    :crate-version "1.0.0"}})))
         "file content"))
  (testing "error"
    (is (thrown-with-msg? ExceptionInfo
                            #"the file test/resources/tmp/bar/1.0.0/download does not exist"
                            (crates-api! {:action :download
                                          :config {:crate {:path tmp-dir}}
                                          :route-params {:crate-name "bar"
                                                         :crate-version "1.0.0"}})))))
