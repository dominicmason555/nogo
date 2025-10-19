(ns feeds-test
  (:require [clojure.test :refer [is deftest testing use-fixtures]]
            [clojure.java.io :as io]
            [clojure.string :as string]
            [clojure.pprint :as pp]
            [hickory.core :as hck]
            [hickory.render :as hckr]
            [feeds]))

(def test-page-html (io/resource "test-page.html"))

(def ^:dynamic *test-page* nil)

(defn test-page-fixture "Loads the test HTML page"
  [test-fn]
  (let [test-page (slurp test-page-html)]
    (binding [*test-page* test-page]
      (test-fn))))

(use-fixtures :once test-page-fixture)

(def page-meta [{:title "Post page 1"
                 :category "Posts"
                 :published "2020-01-01T00:00Z"
                 :summary "First post page"
                 :feed-excluded false
                 :page-list-excluded false}
                {:title "Post page 2"
                 :category "Posts"
                 :published "2021-01-01T00:00Z"
                 :summary "Second post page"
                 :feed-excluded false
                 :page-list-excluded false}
                {:title "Blog page 1"
                 :category "Blog"
                 :published "2022-01-01T00:00Z"
                 :summary "First blog page"
                 :feed-excluded false
                 :page-list-excluded true}
                {:title "Blog page 2"
                 :category "Blog"
                 :published "2023-01-01T00:00Z"
                 :summary "This page is a test"
                 :feed-excluded false
                 :page-list-excluded false}
                {:title "Index"
                 :category "Index"
                 :published "2000-01-01T00:00Z"
                 :summary "Page 0"
                 :feed-excluded false
                 :page-list-excluded true}])

(def hfeed "<div class=\"hfeed\"> <h3>My Posts:</h3> <div class=\"h-feed\"><ul><li><article class=\"h-entry\"><time class=\"dt-published\" datetime=\"2024-01-14T00:00Z\">2024-01-14</time>: <span class=\"p-category\">Projects</span> - <a class=\"u-url p-name plain\" href=\"https://domson.dev/projects/shelf.html\">Desk Shelf</a><p class=\"p-summary\">Making a custom shelf for my desk, getting started with woodworking and CAD</p></article></li><li><article class=\"h-entry\"><time class=\"dt-published\" datetime=\"2023-09-02T00:00Z\">2023-09-02</time>: <span class=\"p-category\">Projects</span> - <a class=\"u-url p-name plain\" href=\"https://domson.dev/projects/epaper.html\">E-Paper</a><p class=\"p-summary\">Building a simple e-paper clock that also shows the weather, with a Pi Zero</p></article></li><li><article class=\"h-entry\"><time class=\"dt-published\" datetime=\"2023-07-31T00:00Z\">2023-07-31</time>: <span class=\"p-category\">Projects</span> - <a class=\"u-url p-name plain\" href=\"https://domson.dev/projects/lightweather.html\">Light Weather</a><p class=\"p-summary\">Building an air-quality sensor and I/O PCB with an ESP32-C3 and MQTT</p></article></li></ul></div> </div>")

(def posts-1-hfeed
  (string/join
   ["<li><article class=\"h-entry\">"
    "<time class=\"dt-published\" datetime=\"2020-01-01T00:00Z\">2020-01-01"
    "</time>: <span class=\"p-category\">Posts</span> - "
    "<a class=\"u-url p-name plain\" href=\"\">Post page 1</a>"
    "<p class=\"p-summary\">First post page</p></article></li>"]))

(deftest page-to-hfeed-test
  (testing feeds/page-to-hfeed
    (let [templated (feeds/page-to-hfeed (first page-meta))
          rendered (hckr/hickory-to-html templated)]
      (is (= rendered posts-1-hfeed)))))

(def posts-1-jsonld
  (string/join
   ["{\"url\":null,\"headline\":null,\"@context\":\"https://schema.org\","
    "\"@type\":\"Blog\",\"author\":{\"@type\":\"Person\",\"name\":null,\"url\""
    ":null},\"blogPost\":[{\"description\":\"First post page\",\"name\":\""
    "Post page 1\",\"articleSection\":\"Posts\",\"datePublished\":\""
    "2020-01-01T00:00Z\",\"keywords\":[\"Posts\"],\"id\":null,\"url\":null,\""
    "dateCreated\":\"2020-01-01T00:00Z\",\"@type\":\"BlogPosting\"}]}"]))

(deftest generate-jsonld-test
  (testing feeds/generate-jsonld
    (let [input {:pages (take 1 page-meta)}
          rendered (feeds/generate-jsonld input)]
      (is (= rendered posts-1-jsonld)))))
