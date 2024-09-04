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

(def expected-page-to-jsonld-test
  {"@type" "BlogPosting"
   :id "https://url/blog/blog1.html"
   :name "Blog title 1"
   :url "https://url/blog/blog1.html"
   :description "Blog summary 1"
   :articleSection "blog name"
   :keywords ["blog name"]
   :dateCreated "1970-01-02"
   :datePublished "1970-01-02"})

(deftest page-to-jsonld-test
  (testing "page-to-jsonld"
    (let [page (second (nogo/get-meta {:data *test-config*}))]
      (is (= expected-page-to-jsonld-test (nogo/page-to-jsonld page))))))

(deftest index-maps-test
  (testing "index-maps"
    (is (= (nogo/index-maps [{:a "a"} {:b "b"} {}])
           [{:a "a" :index 0} {:b "b" :index 1} {:index 2}]))))

(deftest get-prev-next-test
  (testing "get-prev-next"
    (let [test-input [{:a "a"} {:b "b"} {:c "c"}]]
      (is (= (nogo/get-prev-next test-input 0) {:next {:b "b"}}))
      (is (= (nogo/get-prev-next test-input 1) {:prev {:a "a"} :next {:c "c"}}))
      (is (= (nogo/get-prev-next test-input 2) {:prev {:b "b"}})))))
