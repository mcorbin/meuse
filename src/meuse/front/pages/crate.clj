(ns meuse.front.pages.crate
  (:require [meuse.db.public.crate :as public-crate]))

(defn page
  [crates-db request]
  (let [crate-name (get-in request [:route-params :name])
        crates-versions (->> (public-crate/get-crate-and-versions
                              crates-db crate-name)
                             (sort-by :crates_versions/created_at)
                             reverse)]
    [:h1 (:crates/name (first crates-versions))]))
