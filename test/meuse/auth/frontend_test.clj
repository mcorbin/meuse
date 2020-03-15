(ns meuse.auth.frontend-test
  (:require [meuse.auth.frontend :refer :all]
            [clj-time.core :as time]
            [clj-time.coerce :as coerce]
            [clojure.test :refer :all]
            [crypto.random :as random]))

(def secret-key "2s5u8x/A?D(G+KbPeShVmYq3t6w9y$B&")
(def spec (secret-key-spec secret-key))
(def user-id (java.util.UUID/randomUUID))

(deftest encrypt-decrypt-test
  (doseq [s (take 10 (repeatedly #(random/base64 20)))]
    (is (= s (decrypt (encrypt s spec) spec)))))

(deftest generate-token-test
  (let [token (generate-token user-id spec)
        decrypted (decrypt token spec)
        data (extract-data decrypted)]
    (is (= (count decrypted) (+ uuid-size timestamp-size (* 2 random-str-byte-size))))
    (is (= (str user-id) (:user/id data)))
    (is (time/within? (time/interval (time/minus (:timestamp data)
                                                 (time/seconds 3))
                                     (time/now))
                      (:timestamp data)))))

(deftest expired?-test
  (is (expired? {:timestamp (time/minus (time/now)
                                        (time/hours (inc expired-hours)))}))
  (is (expired? {:timestamp (time/plus (time/now)
                                       (time/hours 1))}))
  (is (not (expired? {:timestamp (time/minus (time/now)
                                             (time/hours (dec expired-hours)))}))))

