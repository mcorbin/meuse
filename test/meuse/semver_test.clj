(ns meuse.semver-test
  (:require [meuse.semver :refer :all]
            [clojure.test :refer :all]))

(deftest valid?-test
  (is (valid? "1.0.1"))
  (is (valid? "1.0.10"))
  (is (valid? "3.4.10"))
  (is (valid? "7.0.0-alpha.4"))
  (is (valid? "7.0.0-alpha+110"))
  (is (valid? "7.0.0-alpha+11acea0"))
  (is (valid? "7.0.0+10abc"))
  (is (not (valid? "3.4.a")))
  (is (not (valid? "3.4")))
  (is (not (valid? "3.3.4.2")))
  (is (not (valid? "3")))
  (is (not (valid? "baaa"))))

(deftest extract-number-test
  (is (= "10" (extract-number "10-alpha")))
  (is (= "10" (extract-number "10+alpha"))))

(deftest version->int-vec-test
  (is (= [1 0 1] (version->int-vec "1.0.1")))
  (is (= [2 3 10] (version->int-vec "2.3.10")))
  (is (= [2 3 10] (version->int-vec "2.3.10-alpha.4"))))

(deftest string-number-size-test
  (is (= 5 (string-number-size [1 1 2])))
  (is (= 9 (string-number-size [12 130 20]))))

(deftest compare-versions-test
  (is (= 0 (compare-versions "1.0.1" "1.0.1")))
  (is (= 0 (compare-versions "1.0.1-alpha.4" "1.0.1-alpha.4")))
  (is (= -1 (compare-versions "1.0.1-alpha.4" "1.0.1-alpha.5")))
  (is (= -1 (compare-versions "1.0.1+alpha.40" "1.0.1+alpha.50")))
  (is (= -1 (compare-versions "1.0.1" "2.0.1")))
  (is (= -1 (compare-versions "1.0.1" "1.1.1")))
  (is (= -1 (compare-versions "1.0.1" "1.0.2")))
  (is (= -1 (compare-versions "1.2.2" "1.3.1")))
  (is (= -1 (compare-versions "4.0.10" "4.1.1")))
  (is (= 1 (compare-versions "2.0.1-alpha+10" "1.0.1-alpha+9")))
  (is (= 1 (compare-versions "2.0.1-alpha+110" "1.0.1-alpha+9")))
  (is (= 1 (compare-versions "2.0.1-alpha+999" "1.0.1-alpha+9")))
  (is (= 1 (compare-versions "2.0.1-alpha+90" "1.0.1-alpha+9")))
  (is (= 1 (compare-versions "2.0.1+10" "1.0.1+9")))
  (is (= 1 (compare-versions "2.0.1" "1.0.1")))
  (is (= 1 (compare-versions "1.0.10" "1.0.2")))
  (is (= 1 (compare-versions "1.1.1" "1.0.1")))
  (is (= 1 (compare-versions "1.0.2" "1.0.1")))
  (is (= 1 (compare-versions "1.3.1" "1.2.2")))
  (is (= 1 (compare-versions "4.1.10" "4.1.1"))))
