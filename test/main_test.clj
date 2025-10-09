(ns main-test
  (:require [clojure.test :refer [is deftest testing use-fixtures]]
            [clojure.java.io :as io]
            [clojure.pprint :as pp]
            [clojure.edn :as edn]
            [hickory.core :as hck]
            [hickory.zip :as hckz]
            [hickory.select :as hcks]
            [main]))

(def test-page-html (io/resource "test-page.html"))

(def ^:dynamic *test-page* nil)

(defn test-page-fixture "Loads a fake nogo.edn config file"
  [test-fn]
  (let [test-page (slurp test-page-html)]
    (binding [*test-page* test-page]
      (test-fn))))

(use-fixtures :once test-page-fixture)

(deftest select-title-test
  (testing "Nothing"
    (let [parsed (main/parse-html-page *test-page*)
          date (hcks/select-next-loc
                (hcks/child (hcks/tag :about) (hcks/tag :date))
                (hckz/hickory-zip parsed))]
      (println "select-title-test" (first date) "\n\n"))))

(deftest select-title-test2
  "Replace a date element"
  (testing "Nothing"
    (let [parsed (main/parse-html-page "<about><date></about>")
          hzip (hckz/hickory-zip parsed)
          date-loc (hcks/select-next-loc
                    (hcks/child (hcks/tag :about) (hcks/tag :date))
                    hzip)]
      (second date-loc))))
