(ns meuse.store.s3-test
  (:require [meuse.store.s3 :refer :all]
            [amazonica.aws.s3 :as s3]
            [clojure.test :refer :all]
            [spy.core :as spy]))

(deftest validate-crate-version-test
  (is (= {"2.3.2" true
          "2.3.4" true}
         (validate-crate-version {"2.3.2" true} "foo/2.3.4/download")))
  (is (= {"2.3.4" true}
         (validate-crate-version {} "foo/2.3.4/download")))
  (is (= {}
         (validate-crate-version {} "foo/")))
  (is (= {"2.3.4" false}
         (validate-crate-version {} "foo/2.3.4/")))
  (is (= {"2.3.4" false}
         (validate-crate-version {} "foo/2.3.4/typo"))))

(deftest s3-versions-test
  (with-redefs [s3/list-objects-v2
                (spy/stub {:object-summaries
                           [{:key "foo/2.3.2/download"}
                            {:key "foo/2.3.4/download"}]})]
    (is (= {"2.3.2" true
            "2.3.4" true}
           (s3-versions {} "bucket" "foo"))))
  (with-redefs [s3/list-objects-v2
                (spy/stub {:object-summaries
                           [{:key "foo/2.3.2/download"}]})]
    (is (= {"2.3.2" true}
           (s3-versions {} "bucket" "foo"))))
  (with-redefs [s3/list-objects-v2
                (spy/stub {:object-summaries
                           [{:key "foo/2.3.2/"}]})]
    (is (= {"2.3.2" false}
           (s3-versions {} "bucket" "foo"))))
  (with-redefs [s3/list-objects-v2
                (spy/stub {:object-summaries
                           [{:key "foo/2.3.2/a"}]})]
    (is (= {"2.3.2" false}
           (s3-versions {} "bucket" "foo"))))
  (with-redefs [s3/list-objects-v2
                (spy/stub {:object-summaries
                           [{:key "foo/"}]})]
    (is (= {}
           (s3-versions {} "bucket" "foo")))))
