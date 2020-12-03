(ns meuse.api.crate)

;; the regex for the crates names and versions in path
(def crate-regex #"[a-zA-Z0-9~._+~-]+")

(def crate-name-path [crate-regex :crate-name])
(def crate-version-path [crate-regex :crate-version])
