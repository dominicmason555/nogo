(ns bogo
  (:require [clojure.string :as string]
            [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.pprint :refer [pprint]]
            [me.raynes.fs :as fs]
            [hickory.core :as hick]
            [hickory.render :refer [hickory-to-html]]
            [cljstache.core :as stache]))

(def atom-template (io/resource "atom_templ.xml"))

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
          outfolder (fs/file (data :rootpath) "output" (folder :outfolder))]
      (fs/mkdirs outfolder)
      (doseq [page (folder :pages)]
        (let [outfile (fs/file outfolder (page :file))
              main (slurp (fs/file infolder (folder :infolder) (page :file)))
              rendered (stache/render basefile {:style stylefile
                                                :header header
                                                :footer footer
                                                :main main})]
          (println (str "Outputting page: " outfile))
          (spit outfile rendered))))))

(defn page-to-twtxt "Creates a TWTXT entry from a page"
  [page]
  (str (page :date) "T00:00\t" (page :title) " " (page :url)))

(defn get-meta "Makes a list of metadata for each page, for feeds"
  [data]
  (for [folder (vals ((data :data) :folders))
        page (folder :pages)]
    (let [root ((data :data) :url)
          folderpath (folder :outfolder)
          folderstr (if-not (empty? folderpath) (str folderpath "/") "")
          url (str root "/" folderstr (page :file))]
      (merge page {:folder (folder :name) :url url}))))

(defn generate-twtxt "Generates and outputs the TWTXT feed"
  [data]
  (let [outfolder (fs/file (data :rootpath) "output" ((data :data) :feedsdir))
        outfile (fs/file outfolder "twtxt.txt")
        twtxt (string/join "\n" (map page-to-twtxt (get-meta data)))]
    (fs/mkdirs outfolder)
    (println (str "Outputting feed: " outfile))
    (spit outfile twtxt)))

(defn generate-atom "Generates and outputs the ATOM feed"
  [data time]
  (let [outfolder (fs/file (data :rootpath) "output" ((data :data) :feedsdir))
        outfile (fs/file outfolder "atom.xml")
        template (slurp atom-template)
        posts (get-meta data)
        authorname (get-in data [:data :authorname])
        feedid (get-in data [:data :feedid])
        feedtitle (get-in data [:data :title])
        feedlink (get-in data [:data :url])
        selflink (string/join "/" [(get-in data [:data :url])
                                   (get-in data [:data :feedsdir]) "atom.xml"])
        rendered (stache/render template {:entries posts
                                          :feedtitle feedtitle
                                          :feedlink feedlink
                                          :selflink selflink
                                          :authorname authorname
                                          :feedid feedid
                                          :updated time})]
    (fs/mkdirs outfolder)
    (println (str "Outputting feed: " outfile))
    (spit outfile rendered)))

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
    (generate-twtxt data)
    (generate-atom data (.. (java.time.ZonedDateTime/now)
                            (format java.time.format.DateTimeFormatter/ISO_INSTANT)))
    (println "Generating pages")
    (output-rendered data))
  (println "Generation complete"))

(defn usage []
  (println "Usage:")
  (println "\tNot like that"))

(defn -main []
  (println "Bogo Static Site Generator")
  (if-let [args *command-line-args*]
    (generate-everything (fs/normalized (first args)))
    (usage)))

(-main)
