(ns meuse.front.pages.search
  (:require [meuse.api.crate.search :as api-search]
            [meuse.db.public.search :as public-search]))

(defn format-result
  [result]
  (->> (group-by :crate-id result)
       (map api-search/get-crate-max-version)))

(defn page
  [search-db request]
  (let [{query :q} (:params request)
        result (format-result (public-search/search search-db query))]
    [:div {:id "search-page"}
     [:h1 "Search results"]
     [:p [:span {:class "stat-num"} (count result)] " result found for query "
      [:span {:class "bold"} query]]
     (for [crate result]
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
