(ns meuse.front.pages.category
  (:require [meuse.db.public.category :as public-category]))

(defn page
  [category-db request]
  (let [categories (public-category/get-categories category-db)
        count-crates (->> (public-category/count-crates category-db)
                          (map (fn [c] [(:categories/id c)
                                        (:count c)]))
                          (into {}))]
    [:div {:class "categories"}
     [:h1 "Categories"]
     (for [category-partition (partition-all 3 categories)]
       [:div {:class "row"}
        (for [category category-partition]
          [:div {:class "col-4"}
           (let [count-crate (get count-crates (:categories/id category) 0)]
             [:p [:b (:categories/name category)]
              [:br]
              (or (:categories/description category) "-")
              [:br]
              [:b count-crate]
              (if (#{0 1} count-crate) " crate " " crates ")
              " in this category"
              [:br]
              [:a {:href (str "/front/categories/" (:categories/name category))}
               "List crates"]])])])]))
