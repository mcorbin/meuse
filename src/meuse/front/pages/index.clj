(ns meuse.front.pages.index
  (:require [meuse.db.public.category :as public-category]
            [meuse.db.public.crate :as public-crate]
            [meuse.db.public.crate-version :as public-crate-version]
            [meuse.db.public.user :as public-user]))

(defn last-updated-crates
  [crate-version-db]
  [:div {:class "col-4"}
   [:h2 {:class "center"} "Just pushed"]
   (let [last-updated (public-crate-version/last-updated
                       crate-version-db 10)]
     (for [crate-version last-updated]
       [:div {:class "index-last-updated-block"}
        (:crate-name crate-version) " ("
        [:span {:class "updated-crate-version"}
         (:version-version crate-version)] ") "
        [:a {:type "button" :class "btn btn-secondary btn-sm float-right" :role "button" :href (str "/front/crates/" (:crate-name crate-version))}
         "see"]]))])

(defn index-page
  [category-db crate-db crate-version-db user-db request]
  [:div {:id "index"}

   [:div {:class "row"}
    [:div {:class "col-12 center"}
     [:h1 "Your private crate registry"]]]

   [:div {:class "row"}
    [:div {:class "col-12 center index-links"}
     [:a {:type "button" :class "btn btn-secondary btn-lg" :role "button" :href "https://meuse.mcorbin.fr/"}
      "Documentation"]
     [:a {:type "button" :class "btn btn-secondary btn-lg" :role "button" :href "https://github.com/mcorbin/meuse"}
      "Github"]]]

   [:div {:class "row index-stats-row"}
    [:div {:class "col-7"}
     [:p "Meuse is a free private registry. It allows you to publish and "
      "share private crates."]
     [:p "You can interact with it using "
      [:a {:href "https://github.com/rust-lang/cargo"} "Cargo"] ", "
      "but it also provides an API to manage things like users, tokens, crates, categories..."]
     [:p "You can check the documentation to have more informations about "
      "this project."]]
    [:div {:class "col-5 "}
     [:span {:class "stat-num"} (:crates-count
                                   (public-crate/count-crates crate-db))]
     " Crates in stock"
     [:br]
     [:span {:class "stat-num"} (:crates-versions-count
                                   (public-crate-version/count-crates-versions
                                    crate-version-db))]
     " Crates versions uploaded"
     [:br]
     [:span {:class "stat-num"} (:users-count
                                   (public-user/count-users
                                    user-db))]
     " Users"]]


   [:div {:class "row"}
    (last-updated-crates crate-version-db)
    ]
   ])
