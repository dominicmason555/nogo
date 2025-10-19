(ns html-test
  (:require [clojure.test :refer [is deftest testing use-fixtures]]
            [clojure.java.io :as io]
            [clojure.pprint :as pp]
            [main]
            [html]))

(def test-page-html (io/resource "test-page.html"))

(def ^:dynamic *test-page* nil)

(defn test-page-fixture "Loads the test HTML page"
  [test-fn]
  (let [test-page (slurp test-page-html)]
    (binding [*test-page* test-page]
      (test-fn))))

(use-fixtures :once test-page-fixture)

(deftest extract-multi-test
  (testing html/extract-multi
    (let [parsed {:tree (html/parse-html-page *test-page*)}
          extracted (html/extract-multi parsed)]
      (and (is (= (extracted :title) "Test Page"))
           (is (= (extracted :path) "/Posts/Test-Page.html"))
           (is (= (extracted :category) "Posts"))
           (is (= (extracted :published) "2025-01-01T00:00Z"))
           (is (= (extracted :summary) "This page is a test"))
           (is (= (extracted :styles) ["base.css" "posts/base.css"]))
           (is (= (extracted :fragments) ["posts/bigtable.html"]))
           (is (= (extracted :feed-excluded) false))
           (is (= (extracted :page-list-excluded) true))))))

