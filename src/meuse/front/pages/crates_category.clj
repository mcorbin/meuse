(ns meuse.front.pages.crates-category
  (:require [meuse.api.crate.search :as api-search]
            [meuse.db.public.crate :as public-crate]))

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
       [:div {:class "row crate-list-element"}
        [:div {:class "col-7"}
         [:p [:span {:class "bold"} (:crates/name crate)]
          [:br]
          "Last version: " [:span {:class "bold"} (:crates_versions/version crate)]]
         [:p (:crates_versions/description crate)]
         [:a {:href (str "/front/crates/" (:crates/name crate))}
          "More information"]]
        [:div {:class "col-5"}
         [:p [:span {:class "bold"} (:crates_versions/download_count crate)]
          " downloads"
          [:br]
          "Created on " (:crates_versions/created_at crate)
          [:br]
          "Last update " (:crates_versions/updated_at crate)]]])]))
