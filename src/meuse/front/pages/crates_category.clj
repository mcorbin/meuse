(ns meuse.front.pages.crates-category
  (:require [meuse.api.crate.search :as api-search]
            [meuse.db.public.crate :as public-crate]
            [clojure.string :as string]))

(defn format-result
  [result]
  (->> (group-by :crate-id result)
       (map api-search/get-crate-max-version)))

(defn page
  [crates-db request]
  (let [category (get-in request [:route-params :category])
        crates (format-result (public-crate/get-crates-for-category crates-db category))
        crate-w (if (#{0 1} (count crates)) "Crate" "Crates")]
    [:div {:id "crates-category"}
     [:h1 (str "Category ") category]
     [:p [:span {:class "stat-num"} (count crates)] (str " " crate-w " in this category")]
     (for [crate crates]
       [:div {:class "row search-result-crate"}
        [:div {:class "col-7"}
         [:p [:span {:class "bold"} (:crate-name crate)]]
         "Last version: " [:span {:class "bold"} (:version-version crate)]
         [:p (:version-description crate)]
         [:a {:href (str "/front/crates/" (:crate-name crate))}
          "More informations"]]
        [:div {:class "col-5"}
         [:p "Created on " (:version-created-at crate)]
         [:p "Last update " (:version-updated-at crate)]]])
     ]
    )
  )
