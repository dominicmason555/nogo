(ns feeds
  (:require
   [clojure.data.json :as json]))

(defn page-to-hfeed "Creates an h-feed HTML item from a page"
  [page]
  {:type :element, :tag :li
   :content
   [{:type :element, :attrs {:class "h-entry"}, :tag :article
     :content
     [{:type :element,
       :attrs {:class "dt-published", :datetime (page :published)}
       :tag :time, :content [(subs (page :published) 0 10)]}
      ": "
      {:type :element, :attrs {:class "p-category"}, :tag :span
       :content [(page :category)]}
      " - "
      {:type :element, :attrs {:class "u-url p-name plain", :href (page :url)}
       :tag :a, :content [(page :title)]}
      {:type :element, :attrs {:class "p-summary"}, :tag :p
       :content [(page :summary)]}]}]})

(defn page-to-jsonld "Creates a JSON-LD BlogPosting item from a page"
  [page]
  {"@type" "BlogPosting"
   :id (page :url)
   :name (page :title)
   :url (page :url)
   :description (page :summary)
   :keywords [(page :category)]
   :articleSection (page :category)
   :dateCreated (page :published)
   :datePublished (page :published)})

(defn generate-jsonld "Generates a JSON-LD context of the pages"
  [data]
  (json/write-str {:url (data :url)
                   :headline (data :title)
                   "@context" "https://schema.org"
                   "@type" "Blog"
                   :author {"@type" "Person"
                            :name (data :authorname)
                            :url (data :url)}
                   :blogPost (mapv page-to-jsonld (data :pages))}
                  :escape-slash false))

