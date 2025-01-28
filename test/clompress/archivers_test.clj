(ns clompress.archivers-test
  (:require [clojure.test :refer [deftest testing is]]
            [babashka.fs :as fs]
            [clompress.archivers :as sut])
  (:import [java.io ByteArrayInputStream ByteArrayOutputStream]))

(defn with-tmp-dir* [f]
  (let [dir (fs/create-temp-dir)]
    (try
      (f dir)
      (finally
        (fs/delete-tree dir)))))

(defmacro with-tmp-dir [f & body]
  `(with-tmp-dir* (fn [~f] ~@body)))

(deftest archive
  (testing "invokes `before-add` on the archive entry when file is added to archive"
    (let [data "this is test data"
          inv (atom [])
          subdir "input"]
      (with-tmp-dir dir
        (is (some? (fs/create-dir (fs/path dir subdir))))
        (is (nil? (spit (fs/file dir subdir "test.txt") data)))
        (with-open [output-stream (ByteArrayOutputStream.)]
          (is (nil? (sut/archive {:output-stream output-stream
                                  :compression "gz"
                                  :archive-type "tar"
                                  :before-add (partial swap! inv conj)}
                                 (str (fs/path dir subdir)))))
          (is (= 2 (count @inv))
              "invoked for directory and for file"))))))
