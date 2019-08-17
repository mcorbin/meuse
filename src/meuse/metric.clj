(ns meuse.metric
  (:require [mount.core :refer [defstate]])
  (:import io.micrometer.core.instrument.Metrics
           io.micrometer.core.instrument.Counter
           io.micrometer.core.instrument.Timer
           io.micrometer.core.instrument.MeterRegistry
           io.micrometer.prometheus.PrometheusConfig
           io.micrometer.prometheus.PrometheusMeterRegistry))

(defn registry-component
  []
  (let [registry (PrometheusMeterRegistry. PrometheusConfig/DEFAULT)]
    (Metrics/addRegistry registry)
    registry))

(defstate registry
  :start (registry-component))

(defn started?
  "is the registry started?"
  []
  (instance? MeterRegistry registry))

(defn get-sample!
  "creates a sample"
  []
  (when (started?)
    (Timer/start registry)))

(defn stop-sample!
  "takes a sample, its name and a list of tags.
  Sop the sample."
  [sample n tags]
  (when (started?)
    (.stop sample
           (.timer registry
                   (name n)
                   (into-array tags)))))

(defmacro with-time
  [n tags & body]
  `(when (started?)
     (let [sample# (get-sample!)]
       (try
         (do ~@body)
         (finally
           (stop-sample! sample# ~n ~tags))))))

(defn increment!
  "increments a counter"
  ([counter tags]
   (increment! counter tags 1))
  ([counter tags n]
   (when (started?)
     (let [builder (doto (Counter/builder "http.errors")
                     (.baseUnit "errors")
                     (.description "http errors")
                     (.tags (into-array tags)))
           counter (.register builder registry)]
       (.increment counter n)))))

(defn http-errors
  "updates the http error counter"
  [request status]
  (when (started?)
    (increment! :http.errors
                ["uri" (str (:uri request))
                 "method"  (str (some-> request
                                        :request-method
                                        name))
                 "status" (str status)])))
