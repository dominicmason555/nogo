(ns nogo-test
  (:require [clojure.test :refer [is deftest testing use-fixtures]]
            [clojure.java.io :as io]
            [clojure.edn :as edn]
            [nogo]))

(def test-config-file (io/resource "test-nogo.edn"))

(def ^:dynamic *test-config* nil)

(defn nogo-config-fixture "Loads a fake nogo.edn config file"
  [test-fn]
  (let [test-config (edn/read-string (slurp test-config-file))]
    (binding [*test-config* test-config]
      (test-fn))))

(use-fixtures :once nogo-config-fixture)

(def expected-flat-pages
  '({:title "index", :date "1970-01-01", :file "index.html", :summary "Index Summary"}
    {:title "Blog title 1", :date "1970-01-02", :file "blog1.html", :summary "Blog summary 1"}
    {:title "Blog title 2", :date "1970-01-2", :file "blog2.html", :summary "Blog summary 2"}))

(deftest get-flat-pages-test
  (testing "get-flat-pages"
    (let [flat-pages (nogo/get-flat-pages *test-config*)]
      (is (= flat-pages expected-flat-pages)))))

(deftest page-to-jsonld-test
  (testing "page-to-jsonld"
    (let [])))
