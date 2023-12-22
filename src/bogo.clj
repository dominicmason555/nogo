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
    (let [templfolder (fs/file (folder :outfolder) "template")]
      (pprint templfolder))))

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

(defn get-data "Parses the Nogo EDN file"
  [path]
  (let [filepath (fs/file path "input" "nogo.edn")
        nogofile (slurp filepath)
        parsed (edn/read-string nogofile)]
    {:data parsed}))

(defn generate-everything [path]
  (println "Scanning" (str path))
  (let [data (get-data path)]
    (pprint (map page-to-twtxt (get-meta data)))
    (output-rendered data)))

(defn usage []
  (println "Usage:")
  (println "\tNot like that"))

(defn -main []
  (println "Bogo Static Site Generator")
  (if-let [args *command-line-args*]
    (generate-everything (fs/normalized (first args)))
    (usage)))

(-main)
