(ns meuse.metric
  (:require [clojure.string :as string]
            [mount.core :refer [defstate]])
  (:import io.micrometer.core.instrument.binder.jvm.ClassLoaderMetrics
           io.micrometer.core.instrument.binder.jvm.JvmGcMetrics
           io.micrometer.core.instrument.binder.jvm.JvmMemoryMetrics
           io.micrometer.core.instrument.binder.jvm.JvmThreadMetrics
           io.micrometer.core.instrument.binder.system.FileDescriptorMetrics
           io.micrometer.core.instrument.binder.system.UptimeMetrics
           io.micrometer.core.instrument.binder.system.ProcessorMetrics
           io.micrometer.core.instrument.Counter
           io.micrometer.core.instrument.Gauge
           io.micrometer.core.instrument.MeterRegistry
           io.micrometer.core.instrument.Metrics
           io.micrometer.core.instrument.Timer
           io.micrometer.prometheus.PrometheusConfig
           io.micrometer.prometheus.PrometheusMeterRegistry
           java.util.function.Supplier))

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

(defn ->tags
  "Converts a map of tags to an array of string"
  [tags]
  (into-array String
              (->> tags
                   (map (fn [[k v]] [(name k) (name v)]))
                   flatten)))

(defn ^Supplier gauge-fn-wrapper [gauge-fn]
  (reify Supplier
    (get [this]
      (gauge-fn))))

(defn create-gauge!
  "Create a gauge."
  [n tags gauge-fn]
  (if (started?)
    (.register (doto (Gauge/builder (name n) (gauge-fn-wrapper gauge-fn))
                 (.tags (->tags tags)))
               registry)))

(defn get-timer!
  "get a timer by name and tags"
  [n tags]
  (.register (doto (Timer/builder (name n))
               (.publishPercentiles (double-array [0.5 0.75 0.98 0.99]))
               (.tags (->tags tags)))
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
  ([counter tags]
   (increment! counter tags 1))
  ([counter tags n]
   (when (started?)
     (let [builder (doto (Counter/builder (name counter))
                     (.tags (->tags tags)))
           counter (.register builder registry)]
       (.increment counter n)))))

(defn http-response
  "updates the http response counter"
  [ctx]
  (when (started?)
    (let [request (:request ctx)
          status (str (:status (:response ctx)))
          uri (cond
                (= "404" status) "?"

                ;; avoid generating 1 metric/crate to download
                (and (string/includes? (:uri request) "/download")
                     (string/includes? (:uri request) "/mirror/"))
                "/api/v1/mirror/download"

                (string/includes? (:uri request) "/download")
                "/api/v1/crate/download"

                :else (str (:uri request)))

          method (str (or (some-> request
                                  :request-method
                                  name)
                          "null"))]
      (increment! :http.responses.total
                  {"uri" uri
                   "method" method
                   "status" status}))))
