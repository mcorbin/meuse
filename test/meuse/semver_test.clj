(ns meuse.semver-test
  (:require [meuse.semver :refer :all]
            [clojure.test :refer :all]))

(deftest valid?-test
  (is (valid? "1.0.1"))
  (is (valid? "1.0.10"))
  (is (valid? "3.4.10"))
  (is (not (valid? "3.4.a")))
  (is (not (valid? "3.4")))
  (is (not (valid? "3.3.4.2")))
  (is (not (valid? "3")))
  (is (not (valid? "baaa"))))

(deftest version->int-vec-test
  (is (= [1 0 1] (version->int-vec "1.0.1"))
      (= [2 3 10] (version->int-vec "2.3.10"))))

(deftest compare-versions-test
  (is (= 0 (compare-versions "1.0.1" "1.0.1")))
  (is (= -1 (compare-versions "1.0.1" "2.0.1")))
  (is (= -1 (compare-versions "1.0.1" "1.1.1")))
  (is (= -1 (compare-versions "1.0.1" "1.0.2")))
  (is (= -1 (compare-versions "1.2.2" "1.3.1")))
  (is (= -1 (compare-versions "4.0.10" "4.1.1")))
  (is (= 1 (compare-versions "2.0.1" "1.0.1")))
  (is (= 1 (compare-versions "1.1.1" "1.0.1")))
  (is (= 1 (compare-versions "1.0.2" "1.0.1")))
  (is (= 1 (compare-versions "1.3.1" "1.2.2")))
  (is (= 1 (compare-versions "4.1.10" "4.1.1"))))
