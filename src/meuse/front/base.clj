(ns meuse.front.base
  (:require [hiccup.element :refer [link-to]]
            [hiccup.page :as page]))

(def head
  [:head
   [:meta {:charset "utf-8"}]
   [:meta {:name "viewport"
           :content "width=device-width, initial-scale=1, shrink-to-fit=no"}]
   [:title "Meuse"]
   [:meta {:name "description"
           ;; todo: replace this with the crate's description on crate pages
           :content "A private Rust crate registry"}]
   (page/include-css "https://fonts.googleapis.com/css?family=Work+Sans:300,400,500,700&display=swap")
   (page/include-css "/css/bootstrap.min.css")
   (page/include-css "/css/style.css")])

(def menu
  [:div {:id "menu"}
   [:div {:class "row"}

    (link-to {:id "title"} "/front"
             "Meuse")

    [:div {:id "menu-search"}
     [:form {:action "/front/search" :method "get"}
      [:input {:type "search"
               :id "menu-search-input"
               :name "q"
               :placeholder "Search"
               :alt "Search"}]]]

    [:div {:id "menu-links"}
     [:span {:class "menu-element"} [:a {:href "/front/crates"} "All Crates"]]
     [:span {:class "menu-element"} "·"]
     [:span {:class "menu-element"} [:a {:href "/front/categories"} "Categories"]]]]])

(def footer
  [:footer {:class "container"}
   [:p
    [:a {:href "https://meuse.mcorbin.fr/"} "Documentation"] " · "
    [:a {:href "https://github.com/mcorbin/meuse"} "Github"] " · "
    "Made by " [:a {:href "https://mcorbin.fr"} "mcorbin"]]])

(defn html
  [body]
  (page/html5 {:lang "en"}
   head
   [:body
    [:div {:id "content"}
     menu
     [:div {:id "core"}
      body]
     footer]
    (page/include-js "/js/jquery-3.3.1.slim.min.js")
    (page/include-js "/js/popper.min.js")
    (page/include-js "/js/bootstrap.min.js")]))
