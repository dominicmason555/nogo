(ns main
  (:require [clojure.string :as string]
            [clojure.pprint :as pp]
            [clojure.edn :as edn]
            [me.raynes.fs :as fs]
            [hickory.core :as hck]
            [clojure.java.io :as io]
            [html]
            [feeds]))

(defn in-parse-config
  "Reads the configuration file from `folderpath/nogo.edn`
   Returns: `{:config <The config> :rootpath <folder path>}`"
  [folderpath]
  (println "Parsing config file from" folderpath)
  (let [filepath (fs/file folderpath "nogo.edn")
        parsed (edn/read (java.io.PushbackReader. (io/reader filepath)))]
    {:config parsed :rootpath (fs/file folderpath)}))

(defn in-parse-html-page
  "Reads and parses a single HTML page"
  [filepath]
  (let [parsed (html/parse-html-page (slurp filepath))]
    (println "Parsing" (.getName filepath))
    {:tree parsed :filepath filepath}))

(defn in-parse-pages
  "Identifies all HTML files under the `:pages` directory, reads and parses them
   with [[in-parse-html-file]] and adds a map of file paths and their parsed
   files to the given `data` argument"
  [data]
  (let [pages-folder (io/file (data :rootpath) ((data :config) :pages))
        page-files (filter #(and (.isFile %) (.endsWith (.getName %) ".html"))
                           (file-seq pages-folder))
        pages (map in-parse-html-page page-files)]
    (assoc data :pages pages)))

(defn extract-meta
  "Extracts and associates all metadata from each page in `(data :pages)`"
  [data]
  (println "Extracting Metadata")
  (html/extract-all data))

(defn consolidate-pieces
  "Takes all pieces found by [[extract-meta]] across all pages and consolidates
   them in a single map in `data`, called `:pieces`, de-duplicating file paths"
  [data]
  (println "Consolidating file paths of pieces")
  (let [page-piece-sets (map #(reduce into #{} [(% :styles) (% :fragments)])
                             (data :pages))
        consolidated (reduce into #{} page-piece-sets)]
    (assoc data :pieces consolidated)))

(defn in-read-pieces
  "Reads each file specified by the file paths in the `:pieces` set, and
   replaces the set with a map of piece file paths to the contents of each
   file. The set of paths is used to prevent reading a file more than once."
  [data]
  (println "Reading" (count (data :pieces)) "pieces")
  (assoc data :pieces (into {} (map #(vec [% (slurp %)]) (data :pieces)))))

(defn create-feeds ""
  [data]
  (println data))

(defn transform-pages
  "Applies all transforms to the trees of the pages, such as applying titles,
   in-place, using [[html/transform-page]]."
  [data]
  (println "Transforming" (count (data :pages)) "pages")
  (assoc data :pages (map #(html/transform-page data %) (data :pages))))

(defn out-render-to-files ""
  [args]
  (println args))

(defn generate-everything "All logic for static-site generation"
  [folderpath]
  (->> folderpath
       in-parse-config
       in-parse-pages
       extract-meta
       consolidate-pieces
       in-read-pieces
       create-feeds
       transform-pages
       out-render-to-files))

(defn -main [folderpath]
  (println "Nogo Static Site Generator")
  (generate-everything folderpath))
