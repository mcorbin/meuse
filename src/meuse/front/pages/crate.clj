(ns meuse.front.pages.crate
  (:require [meuse.db.public.crate :as public-crate]))

(defn page
  [crates-db request]
  (let [crate-name (get-in request [:route-params :name])
        crates-versions (->> (public-crate/get-crate-and-versions
                              crates-db crate-name)
                             (sort-by :version-created-at)
                             reverse)]
    [:h1 (:crate-name (first crates-versions))]
    )
  )
