(ns meuse.front.pages.crate
  (:require [meuse.db.public.crate :as public-crate]))

; missing metadata:
;   - features
;   - authors
;   - readme
;   - readme_file
;   - keywords
;   - license_file
;   - links

(defn some-crate-version [version-number crate-versions]
  "Returns the first crate version that has the specified version number, or nil"
  (some
   #(when (= (:crates_versions/version %) version-number) %)
   crate-versions))

(defn count-downloads [all-versions]
  (apply + (map #(:crates_versions/download_count %) all-versions)))

(defn crate-header [version]
  "Shows general information about the crate"
  (let [name (:crates/name version)
        metadata (:crates_versions/metadata version)
        version-str (:crates_versions/version version)
        description (:crates_versions/description version)]
    [:div.crate-header
     [:div
      [:span.categories                                     ; categories
       (when-let [categories (:categories metadata)]
         (for [category categories]
           [:span
            [:a {:href (str "/front/categories/" category)}
             category]]))]
      [:span.keywords                                       ; keywords
       (when-let [keywords (:keywords metadata)]
         (for [keyword keywords]
           (list " " [:span \# keyword])))]]
     [:h1
      name                                                  ; name
      [:span.crate-version version-str]]                    ; version
     [:p.description description]                           ; description
     ]))

(defn page
  [crates-db request]
  (let [crate-name (get-in request [:route-params :name])
        crate-versions (->> (public-crate/get-crate-and-versions
                             crates-db crate-name)
                            (sort-by :crates_versions/created_at)
                            reverse)
        latest-version (first crate-versions)
        version-param (get-in request [:params :version])
        version (if (nil? version-param)
                  latest-version
                  (or (some-crate-version version-param crate-versions) latest-version))
        crate-name (:crates/name version)
        version-str (:crates_versions/version version)
        metadata (:crates_versions/metadata version)]
    [:div#crate-page
     (crate-header version)
     [:div.row
      ;; left column
      [:div.col-12.col-md-7.left
       (when-let [authors (:authors metadata)]
         [:p.authors "By "                                  ; TODO: links to authors, contributors, owners
          (for [author authors]
            (list
             [:span author]
             (when-not (= (last authors) author) ", ")))])
       [:div.doc-links
        (when-let [documentation (:documentation metadata)]
          [:a {:href documentation} "API reference"])       ; documentation
        (when-let [repository (:repository metadata)]
          [:a {:href repository} "Repository"])             ; repository
        (when-let [homepage (:homepage metadata)]
          [:a {:href homepage} "Homepage"])]                ; homepage
       [:div.row.add-crate
        [:div "Cargo.toml"]                                 ; add to Cargo.toml
        [:code crate-name " = "
         [:span.hl-string \" version-str \"]]]

       [:div.readme
        ;; TODO: show README
        ]]

      ;; right column
      [:div.col-12.col-md-5.right
       ;; some meta
       [:div.meta
        [:h3 "Meta"]
        [:ul
         [:li "Last updated: "
          [:b (:crates_versions/updated_at version)]]       ; TODO: make human-readable
         [:li "Released: "
          [:b (:crates_versions/created_at version)]]       ; TODO: make human-readable
         (when-let [license (:license metadata)]
           [:li "License: " [:b license]])
         [:li "Total downloads: "
          [:b (count-downloads crate-versions)]]            ; TODO: make human-readable
         [:li "Downloads of this version: "
          [:b (:crates_versions/download_count version)]]   ; TODO: make human-readable
         ;; TODO: dependent crates
         ]]

       ;; versions
       [:div.crate-versions
        [:h3 (str (count crate-versions) " releases")]
        [:ul
         (for [v crate-versions]
           [:li
            (let [v-str (:crates_versions/version v)
                  v-url (str "/front/crates/" (:crates/name version) "?version=" v-str)]
              (if (= v-str version-str)
                version-str
                [:a {:href v-url} v-str]))])]]

       ;; dependencies
       (when-let [dependencies (:deps metadata)]
         [:div.dependencies
          [:h3 "Dependencies"]
          [:ul
           (for [dep dependencies]
             [:li
              [:a {:href (str "/front/crates/" (:name dep))} (:name dep)]
              " "
              (:version_req dep)])]])

       ;; TODO: dev-dependencies

       [:p.uuid "crate: " (:crates/id version)]
       [:p.uuid "version: " (:crates_versions/id version)]]]]))
