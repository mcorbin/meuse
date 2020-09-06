(ns meuse.mocks.db
  (:require [meuse.db.public.category :refer [ICategoryDB]]
            [meuse.db.public.crate :refer [ICrateDB]]
            [meuse.db.public.crate-user :refer [ICrateUserDB]]
            [meuse.db.public.crate-version :refer [ICrateVersionDB]]
            [meuse.db.public.search :refer [ISearchDB]]
            [meuse.db.public.token :refer [ITokenDB]]
            [meuse.db.public.user :refer [IUserDB]]
            [spy.core :as spy]
            [spy.protocol :as protocol]))

(defn category-mock
  [{:keys [by-crate-id create update-category get-categories count-crates]}]
  (protocol/spy ICategoryDB
                (reify ICategoryDB
                  (by-crate-id [this crate-id] by-crate-id)
                  (create [this category-name description] create)
                  (update-category [this category-name fields] update-category)
                  (get-categories [this] get-categories)
                  (count-crates [this] count-crates))))

(defn crate-mock
  [{:keys [create get-crate-and-versions get-crates-and-versions get-crates-for-category
           get-crates-range count-crates count-crates-prefix]}]
  (protocol/spy ICrateDB
                (reify ICrateDB
                  (create [this metadata user-id] create)
                  (get-crate-and-versions [this crate-name] get-crate-and-versions)
                  (get-crates-and-versions [this] get-crates-and-versions)
                  (get-crates-for-category [this category-name] get-crates-for-category)
                  (get-crates-range [this start end prefix] get-crates-range)
                  (count-crates [this] count-crates)
                  (count-crates-prefix [this prefix] count-crates-prefix))))

(defn crate-user-mock
  [{:keys [create-crate-users delete delete-crate-users owned-by?]}]
  (protocol/spy ICrateUserDB
                (reify ICrateUserDB
                  (create-crate-users [this crate-name users] create-crate-users)
                  (delete [this crate-name user-name] delete)
                  (delete-crate-users [this crate-name users] delete-crate-users)
                  (owned-by? [this crate-name user-id] owned-by?))))

(defn crate-version-mock
  [{:keys [count-crates-versions inc-download last-updated
           top-n-downloads update-yank sum-download-count delete]}]
  (protocol/spy ICrateVersionDB
                (reify ICrateVersionDB
                  (count-crates-versions [this] count-crates-versions)
                  (inc-download [this crate-name version] inc-download)
                  (last-updated [this n] last-updated)
                  (top-n-downloads [this n] top-n-downloads)
                  (update-yank [this crate-name crate-version yanked?] update-yank)
                  (sum-download-count [this] sum-download-count)
                  (delete [this crate-name version] delete))))

(defn search-mock
  [{:keys [search]}]
  (protocol/spy ISearchDB
                (reify ISearchDB
                  (search [this query-string] search))))

(defn token-mock
  [{:keys [by-user create delete get-token-user-role set-last-used]}]
  (protocol/spy ITokenDB
                (reify ITokenDB
                  (by-user [this user-name] by-user)
                  (create [this token] create)
                  (delete [this user-name token-name] delete)
                  (set-last-used [this token-id] set-last-used)
                  (get-token-user-role [this token] get-token-user-role))))

(defn user-mock
  [{:keys [by-name crate-owners create delete get-users update-user count-users by-id]}]
  (protocol/spy IUserDB
                (reify IUserDB
                  (by-id [this user-id] by-id)
                  (by-name [this user-name] by-name)
                  (crate-owners [this crate-name] crate-owners)
                  (create [this user] create)
                  (delete [this user-name] delete)
                  (get-users [this] get-users)
                  (update-user [this user-name fields] update-user)
                  (count-users [this] count-users))))
