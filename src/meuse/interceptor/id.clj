(ns meuse.interceptor.id)

(def request-id
  {:name ::request-id
   :enter (fn [ctx]
            (update-in ctx [:request :id] (fn [_] (java.util.UUID/randomUUID))))})
