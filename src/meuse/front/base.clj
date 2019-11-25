(ns meuse.front.base
  (:require [hiccup.element :refer [link-to]]
            [hiccup.page :as page]))

(def head
  [:head
   [:meta {:content "text/html;charset=utf-8"}]
   [:meta {:name "viewport"
           :content "width=device-width, initial-scale=1, shrink-to-fit=no"}]
   [:title "Meuse"]
   (page/include-css "/css/bootstrap.min.css")
   (page/include-css "/css/style.css")])

(def menu
  [:div {:id "menu"
         :class "container"}
   [:div {:class "row"}

    [:div {:class "col-2"}
     [:div
      (link-to {} "/front"
               [:h1 "Meuse"])]]

    [:div {:class "col-3" :id "menu-search"}
     [:form {:action "/front/search" :method "get"}
      [:input {:type "text"
               :id "menu-search-input"
               :autofocus "autofocus"
               :name "q"
               :placeholder "Search"}]]]

    [:div {:class "col-7" :id "menu-links"}
     [:span {:class "menu-element"} [:a {:href "/front/crates"} "Browse All Crates"]]
     [:span {:class "menu-element"} "|"]
     [:span {:class "menu-element"} [:a {:href "/front/categories"} "Browse Categories"]]
     ]]])

(def footer
  [:div {:id "footer"
         :class "container"}
   [:footer {:class "row"}
    [:div {:class "col-12 center"}
     [:p
      [:a {:href "https://meuse.mcorbin.fr/"} "Documentation"] " | "
      [:a {:href "https://github.com/mcorbin/meuse"} "Github"] " | "
      "Made by " [:a {:href "https://mcorbin.fr"} "mcorbin"]]]]])


(defn html
  [body]
  (page/html5
   head
   [:body
    menu
    [:div {:id "core"
           :class "container"}
     body]
    (page/include-js "/js/jquery-3.3.1.slim.min.js")
    (page/include-js "/js/popper.min.js")
    (page/include-js "/js/bootstrap.min.js")]
    footer
   ))
