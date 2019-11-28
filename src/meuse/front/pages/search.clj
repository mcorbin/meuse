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
       [:div {:class "row search-result-crate"}
        [:div {:class "col-7"}
         [:p [:span {:class "bold"} (:crate-name crate)]]
         "Last version: " [:span {:class "bold"} (:version-version crate)]
         [:p (:version-description crate)]
         [:a {:href (str "/front/crates/" (:crate-name crate))}
          "More informations"]]
        [:div {:class "col-5"}
         [:p "Created on " (:version-created-at crate)]
         [:p "Last update " (:version-updated-at crate)]]])]))
