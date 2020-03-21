(ns meuse.front.pages.login
  (:require [meuse.front.base :as base]
            [hiccup.page :as page]))

(defn page
  [request]
  (page/html5
   {:lang "en"}
   base/head
   [:body
    [:div {:id "content"}
     [:div {:id "login" :class "center"}
      [:h1 "Login"]
      [:form {:action "/front/login" :method "post"}
       [:input {:id "menu-username-input"
                :name "username"
                :placeholder "Username"}]
       " - "
       [:input {:type "password"
                :id "menu-password-input"
                :name "password"
                :placeholder "password"}]
       " - "
       [:button {:type "submit"} "Log in"]]]
     base/footer]]))
