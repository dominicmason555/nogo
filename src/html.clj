(ns html
  (:require
   [hickory.select :as hcks]))

(defn extract-title
  [page]
  (let [title-selector (hcks/child (hcks/tag :header) (hcks/tag :h2))
        selected (first (hcks/select title-selector (page :tree)))
        title (first (selected :content))]
    (assoc page :title title)))

(defn extract-page-meta
  [page]
  (println "extracting")
  page)
