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

(deftest select-title-test
  (testing html/extract-title
    (let [parsed {:tree (main/parse-html-page *test-page*)}
          extracted (html/extract-title parsed)
          title (extracted :title)]
      (is (= title "Test Page")))))

