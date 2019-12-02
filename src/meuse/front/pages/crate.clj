(ns meuse.front.pages.crate
  (:require [meuse.db.public.crate :as public-crate]))

(defn page
  [crates-db request]
  (let [crate-name (get-in request [:route-params :name])
        crates-versions (->> (public-crate/get-crate-and-versions
                              crates-db crate-name)
                             (sort-by :crates_versions/created_at)
                             reverse)
        first-version (first crates-versions)]
    [:div {:id "crate-page"}
     [:h1 (:crates/name first-version)]
     [:div {:class "row"}
      [:div {:class "col-6"}
       [:p
        "id: " [:span {:class "bold"} (:crates/id first-version)]]]
      [:div {:class "col-6"}]]
     [:div {:class "row"}
      [:div {:class "col-12"}
       [:h2 (str (count crates-versions) " versions")]]]
     [:div {:class "row"}
      [:div {:class "col-12"}
       (for [crate crates-versions]
       [:div {:class "row crate-list-element"}
        [:div {:class "col-7"}
         [:p [:span {:class "bold"} (:crates/name crate)]
          [:br]
          "Last version: " [:span {:class "bold"} (:crates_versions/version crate)]]
         [:p (:crates_versions/description crate)]
         (when-let [dependencies (:deps (:crates_versions/metadata crate))]
           [:p "Dependencies: " (->>  dependencies
                                      (map #(str (:name %) " - " (:version_req %)))
                                      (clojure.string/join ", "))])]
        [:div {:class "col-5"}
         [:p [:span {:class "bold"} (:crates_versions/download_count crate)]
          " downloads"
          [:br]
          "Created on " (:crates_versions/created_at crate)
          [:br]
          "Last update " (:crates_versions/updated_at crate)
          (when-let [categories (:categories (:crates_versions/metadata crate))]
            [:span
             [:br]
             "Categories: "
             (for [category categories]
               [:span
                [:a {:href (str "/front/categories/" category)} category]
                (when-not (= (last categories) category) ", ")])])]]])]]]))
