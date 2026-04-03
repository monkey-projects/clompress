(ns clompress.archivers-test
  (:require [clojure.test :refer [deftest testing is]]
            [babashka.fs :as fs]
            [clojure.java.io :as io]
            [clompress
             [archivers :as sut]
             [compression :as cc]])
  (:import [java.io ByteArrayInputStream ByteArrayOutputStream]
           org.apache.commons.compress.archivers.ArchiveStreamFactory))

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
              "invoked for directory and for file")))))

  (testing "stores symlinks"
    (let [data "this is test data"
          subdir "input"]
      (with-tmp-dir dir
        (let [sd (fs/create-dir (fs/path dir subdir))
              a (fs/path dir "archive.tgz")]
          (is (some? sd))
          (is (nil? (spit (fs/file sd "test.txt") data)))
          (is (some? (fs/create-sym-link (fs/path sd "testlink") (fs/path sd "test.txt"))))
          (with-open [output-stream (io/output-stream (fs/file a))]
            (is (nil? (sut/archive {:output-stream output-stream
                                    :compression "gz"
                                    :archive-type "tar"}
                                   (str sd)))))
          (is (fs/exists? a))
          ;; Inspect
          (with-open [s (io/input-stream (fs/file a))]
            (let [i (-> (ArchiveStreamFactory.)
                        (.createArchiveInputStream ArchiveStreamFactory/TAR (cc/with-decompression s "gz")))
                  items (take-while some? (repeatedly #(.getNextEntry i)))]
              (is (= 3 (count items)))
              (is (= 1 (->> items
                            (filter (memfn isSymbolicLink))
                            (count)))))))))))
