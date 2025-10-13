(ns html
  (:require
   [hickory.select :as hcks]
   [hickory.core :as hck]))

(defn parse-html-page
  "Parses an HTML page from a string into a Hickory map"
  [html]
  (hck/as-hickory (hck/parse html)))

(defn select-first-content
  "Returns a function that takes a parsed HTML tree and returns the first
   element of the content of the HTML element that is selected by the
   [[hickory.select]] `selector` argument."
  [selector]
  #(first ((first (hcks/select selector (% :tree))) :content)))

(defn select-attr
  "Returns a function that takes a parsed HTML tree and returns an attribute
   of the HTML element  that is selected by the [[hickory.select]] `selector`
   argument, the attribute is specified as a keyword by the `attr` argument."
  [selector attr]
  #(((first (hcks/select selector (% :tree))) :attrs) attr))

(def extractors
  "Map of keys to selector functions that are used to parse metadata out of an
   HTML file. The keys in this map become keys in the map of the page also."
  {:title (select-first-content (hcks/child (hcks/tag :header) (hcks/class :p-name)))
   :category (select-first-content (hcks/child (hcks/tag :header) (hcks/tag :post-category)))
   :published (select-attr (hcks/child (hcks/tag :header) (hcks/class :dt-published)) :datetime)
   :summary (select-first-content (hcks/child (hcks/tag :header) (hcks/class :p-summary)))})

(defn extract-multi
  "Uses [[extractors]] map to extract metadata from a parsed page, associating
   the keys of [[extractors]] to the values returned by its functions on the
   page map. E.g. given a page map and `{:title get-title-fn}`, it will
   associate the return value of `get-title-fn` called on the page map with the
   key `:title` in the page map."
  [page]
  (letfn [(extract-reducer [page ext]
            (assoc page (first ext) ((second ext) page)))]
    (reduce extract-reducer page extractors)))

