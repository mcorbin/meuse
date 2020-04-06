(ns meuse.front.pages.index
  (:require [meuse.db.public.crate :as public-crate]
            [meuse.db.public.crate-version :as public-crate-version]
            [meuse.db.public.user :as public-user]))

(defn last-updated-crates
  [crate-version-db]
  [:div {:class "col-12 col-md-6"}
   [:h2 {:class "center"} "Last updated"]
   (let [last-updated (public-crate-version/last-updated
                       crate-version-db 10)]
     (for [crate-version last-updated]
       [:div {:class "index-crate-block"}
        (:crates/name crate-version) " ("
        (:crates_versions/version crate-version) ") "
        [:a {:class "btn btn-secondary btn-sm float-right" :role "button" :href (str "/front/crates/" (:crates/name crate-version))}
         "see"]]))])

(defn top-downloaded-crates
  [crate-version-db]
  [:div {:class "col-12 col-md-6"}
   [:h2 {:class "center"} "Most downloaded"]
   (let [top-n-downloads (public-crate-version/top-n-downloads
                          crate-version-db 10)]
     (for [crate-version top-n-downloads]
       [:div {:class "index-crate-block"}
        (:crates/name crate-version) " ("
        (:crates_versions/version crate-version) ") "
        ": " [:span {:class "bold"} (:crates_versions/download_count crate-version)]
        [:a {:class "btn btn-secondary btn-sm float-right" :role "button" :href (str "/front/crates/" (:crates/name crate-version))}
         "see"]]))])

(defn index-page
  [category-db crate-db crate-version-db user-db request]
  [:div {:id "page-index"}

   [:div {:class "row"}
    [:div {:class "col-12 center"}
     [:h1 "Your private crate registry"]]]

   [:div {:class "row"}
    [:div {:class "col-12 center index-links"}
     [:a {:class "btn btn-secondary btn-lg" :role "button" :href "https://meuse.mcorbin.fr/"}
      "Documentation"]
     [:a {:class "btn btn-secondary btn-lg" :role "button" :href "https://github.com/mcorbin/meuse"}
      "Github"]]]

   [:div {:class "row index-stats-row"}
    [:div {:class "col-12 col-md-7"}
     [:p "Meuse is a free private registry. It allows you to publish and "
      "share private crates."]
     [:p "You can interact with it using "
      [:a {:href "https://github.com/rust-lang/cargo"} "Cargo"] ", "
      "but it also provides an API to manage things like users, tokens, crates, categories..."]
     [:p "You can check the documentation to have more information about "
      "this project."]]
    [:div {:class "col-12 col-md-5"}
     [:span {:class "stat-num"} (:count
                                 (public-crate/count-crates crate-db))]
     " Crates in stock"
     [:br]
     [:span {:class "stat-num"} (:count
                                 (public-crate-version/count-crates-versions
                                  crate-version-db))]
     " Crate versions uploaded"
     [:br]
     [:span {:class "stat-num"} (:sum
                                 (public-crate-version/sum-download-count
                                  crate-version-db))]
     " Crates downloaded"
     [:br]
     [:span {:class "stat-num"} (:count
                                 (public-user/count-users
                                  user-db))]
     " Users"]]

   [:div {:class "row"}
    (last-updated-crates crate-version-db)
    (top-downloaded-crates crate-version-db)]])
