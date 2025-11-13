(ns main-test
  (:require [clojure.test :refer [is deftest testing use-fixtures]]
            [clojure.java.io :as io]
            [clojure.pprint :as pp]
            [me.raynes.fs :as fs]
            [main]
            [html]))

(def test-page-html (io/resource "test-page.html"))

(def test-contents-folder (io/resource "test-site"))

(def ^:dynamic *test-page* nil)

(defn test-page-fixture "Loads the test HTML page"
  [test-fn]
  (let [test-page (slurp test-page-html)]
    (binding [*test-page* test-page]
      (test-fn))))

(def ^:dynamic *test-contents* nil)

(defn test-contents-fixture "Loads the test folder to a temp folder"
  [test-fn]
  (let [test-folder (fs/ephemeral-dir ".")]
    (fs/copy-dir test-contents-folder test-folder)
    (binding [*test-contents* test-folder]
      (test-fn))))

(use-fixtures :once test-page-fixture)
(use-fixtures :each test-contents-fixture)

(deftest extract-meta-test
  (testing main/extract-meta
    (let [parsed {:tree (html/parse-html-page *test-page*)}
          data {:pages [parsed]}
          data-meta (main/extract-meta data)
          extracted (first (data-meta :pages))]
      (and (is (= (extracted :title) "Test Page"))
           (is (= (extracted :path) "/Posts/Test-Page.html"))
           (is (= (extracted :category) "Posts"))
           (is (= (extracted :published) "2025-01-01T00:00Z"))
           (is (= (extracted :summary) "This page is a test"))))))

(deftest consolidate-pieces-test
  (testing main/consolidate-pieces
    (let [parsed {:tree (html/parse-html-page *test-page*)}
          extracted (html/extract-multi parsed)
          data {:pages [extracted]}
          consolidated (main/consolidate-pieces data)]
      (is (= (consolidated :pieces) #{"posts/bigtable.html" "posts/base.css" "base.css"})))))

(deftest gen-page-test
  (let [folder *test-contents*]
    (pp/pprint folder)
    (pp/pprint (fs/list-dir folder))))
