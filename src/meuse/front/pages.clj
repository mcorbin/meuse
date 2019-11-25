(ns meuse.front.pages
  (:require [meuse.db.public.category :as public-category]
            [meuse.db.public.crate :as public-crate]
            [meuse.db.public.crate-version :as public-crate-version]
            [meuse.db.public.user :as public-user]))

;; (defn index-page
;;   [category-db request]
;;   (let [categories (public-category/get-categories category-db)
;;         count-crates (->> (public-category/count-crates category-db)
;;                           (map (fn [c] [(:category-id c)
;;                                         (:count c)]))
;;                           (into {}))]
;;     [:div {:class "categories"}
;;      [:h2 "Categories"]
;;      [:ul
;;       (for [category categories]
;;         (let [count-crate (get count-crates (:category-id category) 0)]
;;           [:li
;;            [:p [:b (:category-name category)]
;;             [:br]
;;             (or (:category-description category) "-")
;;             [:br]
;;             [:b count-crate]
;;             (if (#{0 1} count-crate) " crate " " crates ")
;;             " in this category"]]))]]))
