(ns meuse.error
  (:require [meuse.metric :as metric]
            meuse.spec
            [cheshire.core :as json]
            [qbits.ex :as ex]
            [clojure.spec.alpha :as s]
            [clojure.tools.logging :refer [error]]))

(def default-msg
  "Internal error. Please checks the logs.")

(def default-problem-map
  {:meuse.spec/non-empty-string "the value should be a non empty string"
   :meuse.spec/null-or-non-empty-string "the value should be a non empty string"
   :meuse.spec/semver "the value should be a valid semver string"
   :meuse.spec/boolean "the value should be a boolean"
   :meuse.spec/uuid "the value should be an uuid"
   :meuse.spec/pos-int "the value should be a positive integer"
   :meuse.spec/inst "the value should be a date"

   :user/password "the password should have at least 8 characters"
   :user/role "the role should be 'admin' or 'tech'"

   :http/tls "invalid tls options"
   :meuse.spec/file "the file does not exist"})

(defn last-keyword
  [coll]
  (-> (filter keyword? coll)
      last))

(defn problem->message
  "Turn an explain-data map into a human-friendly message."
  [problem problem-map]
  (let [{:keys [in pred via val]} problem
        spec (last-keyword via)
        field (last-keyword in)
        problem-map (merge default-problem-map problem-map)
        value (when val (json/generate-string val))]
    (or

     ;; try to get an error message by a spec from a dict
     (when spec
       (when-let [message (get problem-map spec)]
         (if field
           (format "field %s: %s" (name field)  message)
           ;; return the invalid value if the field is empty
           (if value
             (format "invalid value %s: %s" value message)
             message))))

     ;; missing field, but :in exists.
     ;; the field is missing in the :in map
     ;; the predicate would be a lazy seq of:
     ;; (clojure.core/fn [%] (clojure.core/contains? % :field))
     (when (and field
                (seq? pred)
                (> (count pred) 1)
                (seq? (last pred))
                (> (count (last pred)) 0)
                (= 'clojure.core/contains? (-> pred
                                               last
                                               first)))
       (format "field %s missing in %s"
               (-> pred last last name)
               (name field)))

     ;; no a message in a dict, but at least specify a field
     (when field
       (format "field %s is incorrect" (name field)))

     ;;
     ;; A case of a missing field: `:in` would be empty,
     ;; the predicate would be a lazy seq of:
     ;; (clojure.core/fn [%] (clojure.core/contains? % :field))
     ;; Try to get the nested field name.
     ;;
     (when (seq? pred)
       (when-let [field (-> pred last last)]
         (format "field %s is missing" (name field))))

     ;; :via is not a spec, return the invalid parameter
     (when value
       (format "invalid value %s" value))

     ;; default error message
     "invalid parameter")))

(defn problems->message
  [problems problem-map]
  (let [messages (map #(problem->message % problem-map) problems)]
    (with-out-str
      (println "Wrong input parameters:")
      (doseq [msg messages]
        (println (format " - %s"  msg))))))

(defn explain->message
  "
  Produce an error message out from an explain data.
  "
  ([explain] (explain->message explain {}))
  ([explain problem-map]
   (when-let [problems (get explain ::s/problems)]
     (problems->message problems problem-map))))

(ex/derive ::unavailable ::user)
(ex/derive ::interrupted ::user)
(ex/derive ::incorrect ::user)
(ex/derive ::forbidden ::user)
(ex/derive ::unsupported ::user)
(ex/derive ::not-found ::user)
(ex/derive ::conflict ::user)
(ex/derive ::fault ::user)
(ex/derive ::busy ::user)
(ex/derive ::unauthorized ::user)

(def ex-type->status
  {::unavailable 500
   ::interrupted 500
   ::incorrect 400
   ::forbidden 403
   ::unauthorized 401
   ::unsupported 400
   ::not-found 404
   ::conflict 500
   ::fault 500
   ::busy 500})

(defn handle-user-error
  [request data]
  (let [exception (-> data meta ::ex/exception)
        message (.getMessage exception)
        ;; cargo expects a status 200 OK even for errors x_x
        status (if (= (:subsystem request) :meuse.api.crate.http)
                 200
                 (get ex-type->status (:type data) 500))]
    (when (= 500 status)
      (error "unknown error " (pr-str data)))
    (error (:request-id request)
           exception
           "http error"
           (pr-str (ex-data exception))
           status)
    (metric/http-errors request status)
    {:status status
     :body {:errors [{:detail message}]}}))

(defn handle-unexpected-error
  [request ^Exception e]
  (error (:request-id request) e "http error")
  (metric/http-errors request 500)
  {:status 500
   :body {:errors [{:detail default-msg}]}})
