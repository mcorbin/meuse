(ns meuse.message)

(defn yanked?->msg
  [yanked?]
  (if yanked?
    "yank"
    "unyank"))

(defn publish-commit-msg
  "Creates a commit message from a crate"
  [{:keys [metadata]}]
  [(format "%s %s" (:name metadata) (:vers metadata))
   (format "meuse published %s %s" (:name metadata) (:vers metadata))])

(defn yank-commit-msg
  "creates a commit message from a crate"
  [crate-name crate-version yanked?]
  [(format "%s %s" crate-name crate-version)
   (format "meuse %s %s %s"
           (yanked?->msg yanked?)
           crate-name
           crate-version)])
