(ns nogo
  (:require [clojure.string :as string]
            [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.data.json :as json]
            [clojure.data.xml :as xml]
            ;; [clojure.pprint :refer [pprint]]
            ;; [hickory.core :as hick]
            ;; [hickory.render :refer [hickory-to-html]]
            [hiccup2.core :as hc]
            [me.raynes.fs :as fs]
            [cljstache.core :as stache]))

(def atom-template (io/resource "atom_templ.xml"))
(def sitemap-xsl-link "<?xml-stylesheet type=\"text/xsl\" href=\"sitemap.xsl\"?>")

(defn get-meta "Makes a list of metadata for each page, for feeds"
  [data]
  (for [folder (vals ((data :data) :folders))
        page (folder :pages)]
    (let [root ((data :data) :url)
          folderpath (folder :outfolder)
          folderstr (if-not (empty? folderpath) (str folderpath "/") "")
          url (str root "/" folderstr (page :file))]
      (merge page {:folder (folder :name) :url url}))))

(defn page-to-hfeed "Creates an h-feed HTML item from a page"
  [page]
  [:li
   [:article.h-entry
    [:time.dt-published
     {:datetime (str (page :date) " 00:00:00")} (page :date)]
    " - "
    [:a.u-url.plain {:href (page :url)} (page :title)]
    " - "
    [:span.p-summary (page :summary)]]])

(defn output-rendered "Loops over the pages to rendering and outputting them"
  [data]
  (doseq [folder (vals ((data :data) :folders))]
    (println (str "Outputting folder: " (folder :name)))
    (let [infolder (fs/file (data :rootpath) "input")
          templfolder (fs/file infolder (folder :infolder) "template")
          basefile (slurp (fs/file templfolder (folder :template)))
          stylefile (slurp (fs/file templfolder (folder :style)))
          header (slurp (fs/file templfolder (folder :header)))
          footer (slurp (fs/file templfolder (folder :footer)))
          outfolder (fs/file (data :rootpath) "output" (folder :outfolder))
          h-entries (map page-to-hfeed (reverse (get-meta data)))
          h-feed (hc/html [:ul h-entries])]
      (fs/mkdirs outfolder)
      (doseq [page (folder :pages)]
        (let [outfile (fs/file outfolder (page :file))
              main (slurp (fs/file infolder (folder :infolder) (page :file)))
              rendered (stache/render basefile {:style stylefile
                                                :header header
                                                :footer footer
                                                :main main
                                                :hfeed h-feed})]
          (println (str "Outputting page: " outfile))
          (spit outfile rendered))))))

(defn page-to-twtxt "Creates a TWTXT entry from a page"
  [page]
  (str (page :date) "T00:00Z\t" (page :title) " " (page :url)))

(defn generate-twtxt "Generates and outputs the TWTXT feed"
  [info]
  (let [outfile (fs/file (info :outfolder) "twtxt.txt")
        twtxt (string/join "\n" (map page-to-twtxt (info :entries)))]
    (println (str "Outputting feed: " outfile))
    (fs/mkdirs (info :outfolder))
    (spit outfile twtxt)))

(defn page-to-json "Creates a JSON feed item from a page"
  [page]
  {:id (page :url)
   :title (page :title)
   :url (page :url)
   :content_text (page :summary)
   :tags [(page :folder)]
   :date_published (str (page :date) "T00:00Z")})

(defn generate-jsonfeed "Generates and outputs the JSON feed"
  [info]
  (let [outfile (fs/file (info :outfolder) "feed.json")
        entries (map page-to-json (info :entries))
        feedmap {:version "https://jsonfeed.org/version/1.1"
                 :title (info :feedtitle)
                 :home_page_url (info :feedlink)
                 :feed_url (info :selflink)
                 :authors [{:name (info :authorname)}]
                 :items entries}]
    (println (str "Outputting feed: " outfile))
    (spit outfile (json/write-str feedmap))))

(defn add-sitemap-xsl "Adds a link to the XSL sheet for the sitemap"
  [xmlstr]
  (let [pos (+ 2 (string/index-of xmlstr "?>"))]
    (str (subs xmlstr 0 pos) sitemap-xsl-link (subs xmlstr pos))))

(defn generate-sitemap "Generates an XML sitemap"
  [info]
  (let [outfile (fs/file (info :outfolder) "sitemap.xml")
        sitemap (xml/sexp-as-element
                 [:urlset {:xmlns "http://www.sitemaps.org/schemas/sitemap/0.9"}
                  (map (fn [page]
                         [:url [:loc (page :url)] [:lastmod (page :date)]])
                       (info :entries))])]
    (spit outfile (add-sitemap-xsl (xml/emit-str sitemap)))))

(defn generate-atom "Generates and outputs the ATOM feed"
  [info]
  (let [outfile (fs/file (info :outfolder) "atom.xml")
        rendered (stache/render (slurp atom-template)
                                {:entries (info :entries)
                                 :feedtitle (info :feedtitle)
                                 :feedlink (info :feedlink)
                                 :selflink (str (info :selflink) "/atom.xml")
                                 :authorname (info :authorname)
                                 :feedid (info :feedid)
                                 :updated (info :updated)})]
    (println (str "Outputting feed: " outfile))
    (fs/mkdirs (info :outfolder))
    (spit outfile rendered)))

(defn generate-feeds "Generates and outputs the feeds"
  [data time]
  (let [info {:outfolder (fs/file (data :rootpath) "output"
                                  ((data :data) :feedsdir))
              :entries (get-meta data)
              :updated time
              :authorname (get-in data [:data :authorname])
              :feedid (get-in data [:data :feedid])
              :feedtitle (get-in data [:data :title])
              :feedlink (get-in data [:data :url])
              :selflink (string/join "/" [(get-in data [:data :url])
                                          (get-in data [:data :feedsdir])])}]
    (generate-atom info)
    (generate-sitemap info)
    (generate-jsonfeed info)
    (generate-twtxt info)))

(defn get-data "Parses the Nogo EDN file"
  [path]
  (let [filepath (fs/file path "input" "nogo.edn")
        nogofile (slurp filepath)
        parsed (edn/read-string nogofile)]
    {:data parsed :rootpath (fs/file path)}))

(defn generate-everything "Runs functions to generate feeds and pages"
  [path]
  (println "Scanning" (str path))
  (let [data (get-data path)]
    (println "Generating feeds")
    (generate-feeds data (.. (java.time.ZonedDateTime/now)
                             (format java.time.format.DateTimeFormatter/ISO_INSTANT)))
    (println "Generating pages")
    (output-rendered data))
  (println "Generation complete"))

(defn usage []
  (println "Usage:")
  (println "\tNot like that"))

(defn -main []
  (println "Nogo Static Site Generator")
  (if-let [args *command-line-args*]
    (generate-everything (fs/normalized (first args)))
    (usage)))

(-main)
