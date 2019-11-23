(ns meuse.front.pages.crates
  (:require [meuse.db.public.crate :as public-crate]
            [clojure.string :as string]))


(def letters
  [:div {:class "row" :id "letters"}
   [:div {:class "col-12"}
    (for [letter ["A" "B" "C" "D" "E" "F" "G" "H" "I" "J" "K" "L" "M" "N" "O" "P"
                  "Q" "R" "S" "T" "U" "V" "W" "X" "Y" "Z"]]
      [:a {:href (str "/front/crates?letter=" letter)} letter])]])


(defn pages
  []
  [:div {:class "pages-next"}
   [:a {:href ""} "previous"]
   " - "
   [:a {:href ""} "next"]
   ]
  )

(defn page
  [crates-db request]
  (let [letter (string/lower-case (get-in request [:params :letter] "a"))
        start (get-in request [:route-params :start] 0)
        end (get-in request [:route-params :start] 10)
        crates (public-crate/get-crates-range crates-db start end letter)
        nb-crates (:crates-count (public-crate/count-crates crates-db))
        nb-crates-prefix (:crates-count (public-crate/count-crates-prefix crates-db letter))]
    [:div {:id "crates"}
     [:h1 "All Crates"]
     letters
     [:p [:span {:class "bold"} nb-crates-prefix] " crates starting by "
      [:span {:class "bold"} letter]
      " on a total of "[:span {:class "bold"} nb-crates] " crates"]
     (pages)
     (for [crate crates]
       [:div {:class "row search-result-crate"}
        [:div {:class "col-7"}
         [:p [:span {:class "bold"} (:crate-name crate)]]
         "ID: " [:span {:class "bold"} (:crate-id crate)]
         [:p (:version-description crate)]
         [:a {:href (str "/front/crates/" (:crate-name crate))}
          "More informations"]]
        [:div {:class "col-5"}
         [:p [:span {:class "stat-num"} (:crate-versions-count crate)] " Releases"]]
        ]
       )
     ])
  )
