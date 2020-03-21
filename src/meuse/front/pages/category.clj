(ns meuse.front.pages.category
  (:require [meuse.db.public.category :as public-category]))

(defn page
  [category-db _]
  (let [categories (public-category/get-categories category-db)
        count-crates (->> (public-category/count-crates category-db)
                          (map (fn [c] [(:crates_categories/category_id c)
                                        (:count c)]))
                          (into {}))]
    [:div.categories
     [:h1 "Categories"]
     [:div.row.category-row
      (for [category categories]
        [:a.category.col-12.col-md-4.col-sm-6
         {:href (str "/front/categories/" (:categories/name category))}
         [:h2 (:categories/name category)]
         [:p.description (or (:categories/description category) "-")]
         (let [crate-count (get count-crates (:categories/id category) 0)]
           [:p.crate-count
            [:b crate-count]
            (if (= 1 crate-count) " crate" " crates")
            " in this category"])])]]))
