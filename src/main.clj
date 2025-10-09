(ns main
  (:require [clojure.string :as string]
            [clojure.pprint :as pp]
            [clojure.edn :as edn]
            [me.raynes.fs :as fs]
            [hickory.core :as hck]
            [hickory.select :as hcks]
            [clojure.java.io :as io]))

(defn in-parse-config
  "Reads the configuration file from `folderpath/nogo.edn`
   Returns: `{:config <The config> :rootpath <folder path>}`"
  [folderpath]
  (println "Parsing config file from" folderpath)
  (let [filepath (fs/file folderpath "nogo.edn")
        parsed (edn/read (java.io.PushbackReader. (io/reader filepath)))]
    {:config parsed :rootpath (fs/file folderpath)}))

(defn parse-html-page
  "Parses an HTML page from a string into a Hickory map"
  [html]
  (hck/as-hickory (hck/parse html)))

(defn in-parse-html-file
  "Reads and parses a single HTML page"
  [filepath]
  (let [parsed (parse-html-page (slurp filepath))]
    (println "Parsing" (.getName filepath))
    (pp/pprint (hcks/select (hcks/tag :h2) parsed))
    parsed))

(defn in-parse-html
  "Identifies all HTML files under the `:pages` directory, reads and parses them
   with [[in-parse-html-file]] and adds a map of file paths and their parsed
   files to the given `data` argument"
  [data]
  (let [pages-folder (io/file (data :rootpath) ((data :config) :pages))
        page-files (filter #(and (.isFile %) (.endsWith (.getName %) ".html"))
                           (file-seq pages-folder))
        page-map (zipmap page-files (map in-parse-html-file page-files))]
    (assoc data :pages page-map)))

(defn in-read-assets ""
  [args])
  ;(pp/pprint args))

(defn transform-pages ""
  [args]
  (println args))

(defn create-feeds ""
  [args]
  (println args))

(defn out-render-to-files ""
  [args]
  (println args))

(defn generate-everything "All logic for static-site generation"
  [folderpath]
  (->> folderpath
       in-parse-config
       in-parse-html
       in-read-assets
       transform-pages
       create-feeds
       out-render-to-files))

(defn -main [folderpath]
  (println "Nogo Static Site Generator")
  (generate-everything folderpath))
