(ns nogo
  (:require [clojure.string :as string]
            [me.raynes.fs :as fs]
            [hickory.core :as hick]
            [hickory.render :refer [hickory-to-html]]
            [cljstache.core :as stache]))


;;;; Static site generator: takes a folder of HTML files and turns them into a
;;;; folder of HTML files. Also creates a TWTXT feed.


(def templates_dir "./site/templates/")
(def content_dir "./site/input/")
(def output_dir "./site/output/")
(def feeds_dir "./site/feeds/")


;(html/deftemplate templ-article templates_dir_dir
;  [templates article frontmatter]
;  [:head :title] (html/content (str "Domson.dev - " (:data-title frontmatter)))
;  [:head :style] (html/html-content (templates "style"))
;  [:body :div.header] (html/html-content (templates "header"))
;  [:body :main] (html/content article)
;  [:body :div.footer] (html/html-content (templates "footer")))


(defn get-templates "Gets a map of template file names to paths" []
  (let [files (map str (fs/list-dir templates_dir))
        names (map fs/name files)]
    (zipmap names (map slurp files))))


(defn find-frontmatter "Extracts the front matter from HTML"
  [frags]
  (:attrs (first (filter #(= (:content (:attrs %)) "front-matter") frags))))


(defn find-article "Extracts the article from HTML"
  [frags]
  (:content (first (filter #(= (:tag %) :article) frags))))


(defn process-page "Parses HTML into a map of frontmatter and article"
  [page]
  (let [frags (->> page hick/parse-fragment (map hick/as-hickory))]
    {:frontmatter (find-frontmatter frags)
     :article (find-article frags)}))


(defn render-project-page "Associates the :rendered template in the page map"
  [templates page]
  (let [rendered (stache/render
                   (templates "template_base")
                   {:title (str "Domson.dev - " (:data-title (:frontmatter page)))
                    :style (templates "style")
                    :header (templates "header")
                    :main (mapv hickory-to-html (:article page))
                    :footer (templates "footer")})]
    (assoc page :rendered rendered)))


(defn render-page "Calls the different page rendering functions by article-type"
  [templates page]
  (let [processed-page (process-page page)
        type (:data-article-type (:frontmatter processed-page))]
    (cond
      (= type "project") (render-project-page templates processed-page))))


(defn output-html "Creates the output HTML file for a page"
  [rendered-page]
  (let [frontmatter (:frontmatter rendered-page)
        filepath (str output_dir (:data-slug frontmatter) ".html")]
    (prn (str "Outputting " (:data-slug frontmatter)))
    (spit filepath (:rendered rendered-page))))


(defn page-to-twtxt "Creates a TWTXT entry from a page"
  [page]
  (let [frontmatter (:frontmatter page)]
    (str (:data-date frontmatter)
         "T00:00\t"
         (:data-title frontmatter)
         " https://domson.dev/"
         (:data-slug frontmatter)
         ".html")))


(defn output-twtxt "Creates a TWTXT feed of the posts"
  [pages]
  (let [entries (sort (map page-to-twtxt pages))
        all-entries (str (string/join "\n" entries) "\n")]
    (spit (str feeds_dir "twtxt.txt") (str all-entries))))


(defn output-all "Outputs a page in all forms"
  [rendered-pages]
  (println (count rendered-pages) "pages")
  (output-twtxt rendered-pages)
  (mapv output-html rendered-pages))


(defn -main []
  (println "Nogo Static Site Generator")
  (println (str fs/*cwd* "/site/templates/template_base.html"))
  (let [pages (map slurp (fs/list-dir content_dir))
        templates (get-templates)
        rendered (map #(render-page templates %) pages)]
    (output-all rendered)))


(-main)

