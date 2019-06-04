(ns meuse.helpers.request)

(defn add-auth
  ([request] (add-auth request "user2" "tech"))
  ([request user] (add-auth request "user2" "tech"))
  ([request user role] (assoc request
                              :auth
                              {:user-name user
                               :role-name role})))
