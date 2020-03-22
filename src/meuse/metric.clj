(ns meuse.metric
  (:require [mount.core :refer [defstate]])
  (:import io.micrometer.core.instrument.binder.jvm.ClassLoaderMetrics
           io.micrometer.core.instrument.binder.jvm.JvmGcMetrics
           io.micrometer.core.instrument.binder.jvm.JvmMemoryMetrics
           io.micrometer.core.instrument.binder.jvm.JvmThreadMetrics
           io.micrometer.core.instrument.binder.system.FileDescriptorMetrics
           io.micrometer.core.instrument.binder.system.UptimeMetrics
           io.micrometer.core.instrument.binder.system.ProcessorMetrics
           io.micrometer.core.instrument.Counter
           io.micrometer.core.instrument.MeterRegistry
           io.micrometer.core.instrument.Metrics
           io.micrometer.core.instrument.Timer
           io.micrometer.prometheus.PrometheusConfig
           io.micrometer.prometheus.PrometheusMeterRegistry))

(defn registry-component
  []
  (let [registry (PrometheusMeterRegistry. PrometheusConfig/DEFAULT)]
    (Metrics/addRegistry registry)
    (.bindTo (ClassLoaderMetrics.) registry)
    (.bindTo (JvmGcMetrics.) registry)
    (.bindTo (JvmMemoryMetrics.) registry)
    (.bindTo (JvmThreadMetrics.) registry)
    (.bindTo (FileDescriptorMetrics.) registry)
    (.bindTo (UptimeMetrics.) registry)
    (.bindTo (ProcessorMetrics.) registry)
    registry))

(defstate registry
  :start (registry-component))

(defn started?
  "is the registry started?"
  []
  (instance? MeterRegistry registry))

(defn get-timer!
  "get a timer by name and tags"
  [n tags]
  (.register (doto (Timer/builder (name n))
               (.publishPercentiles (double-array [0.5 0.75 0.98 0.99]))
               (.tags (into-array tags)))
             registry))

(defmacro with-time
  [n tags & body]
  `(if (started?)
     (let [timer# (get-timer! ~n ~tags)
           current# (java.time.Instant/now)]
       (try
         (do ~@body)
         (finally
           (let [end# (java.time.Instant/now)]
             (.record timer# (java.time.Duration/between current# end#))))))
     (do ~@body)))

(defn increment!
  "increments a counter"
  ([counter config]
   (increment! counter config 1))
  ([counter {:keys [tags unit description]} n]
   (when (started?)
     (let [builder (doto (Counter/builder (name counter))
                     (.baseUnit unit)
                     (.description description)
                     (.tags (into-array tags)))
           counter (.register builder registry)]
       (.increment counter n)))))

(defn http-errors
  "updates the http error counter"
  [request status]
  (when (started?)
    (increment! :http.errors
                {:unit "errors"
                 :description "http errors"
                 :tags ["uri" (str (:uri request))
                        "method"  (str (some-> request
                                               :request-method
                                               name))
                        "status" (str status)]})))
