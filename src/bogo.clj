(ns bogo
  (:require [clojure.string :as string]
            [clojure.edn :as edn]
            [clojure.pprint :refer [pprint]]
            [me.raynes.fs :as fs]
            [hickory.core :as hick]
            [hickory.render :refer [hickory-to-html]]
            [cljstache.core :as stache]))

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
          (println (str "Rendering " outfile))
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
    (println (str "Outputting Feed: " outfile))
    (spit outfile twtxt)))

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
