(ns meuse.front.pages.tokens
  (:require [meuse.db.public.token :as token]))

(defn page
  [token-db request]
  [:div {:id "tokens"}
   [:h1 "Your tokens"]
   (if-let [user-name (get-in request [:auth :user-name])]
     (let [tokens (->> (token/by-user token-db user-name)
                       (map #(select-keys % [:tokens/id
                                             :tokens/name
                                             :tokens/last_used_at
                                             :tokens/created_at
                                             :tokens/expired_at])))]
       (for [token tokens]
         [:div {:class "row crate-list-element"}
          [:div {:class "col-12"}
           [:p [:span {:class "bold"} (:tokens/name token)]]
           "ID: " [:span {:class "bold"} (:tokens/id token)]
           [:br]
           "Created at " [:span {:class "bold"} (:tokens/created_at token)]
           [:br]
           "Expired at " [:span {:class "bold"} (:tokens/expired_at token)]
           [:br]
           "Last used at " [:span {:class "bold"} (or (:tokens/last_used_at token)
                                                      "never")]
           ]]))
     [:p [:span {:class "bold"} "You must be logged it to see this page"]])])
