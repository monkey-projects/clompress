(ns clompress.archivers-test
  (:require [clojure.test :refer [deftest testing is]]
            [babashka.fs :as fs]
            [clojure.java.io :as io]
            [clompress
             [archivers :as sut]
             [compression :as cc]
             [helpers :as h]
             [unarchivers :as cu]])
  (:import [java.io ByteArrayInputStream ByteArrayOutputStream]
           java.nio.file.attribute.PosixFilePermission
           org.apache.commons.compress.archivers.ArchiveStreamFactory))

(defn verify-archive [default-opts]
  (testing "invokes `before-add` on the archive entry when file is added to archive"
    (let [data "this is test data"
          inv (atom [])
          subdir "input"]
      (h/with-tmp-dir dir
        (is (some? (fs/create-dir (fs/path dir subdir))))
        (is (nil? (spit (fs/file dir subdir "test.txt") data)))
        (with-open [output-stream (ByteArrayOutputStream.)]
          (is (nil? (sut/archive (merge default-opts
                                        {:output-stream output-stream
                                         :before-add (partial swap! inv conj)})
                                 (str (fs/path dir subdir)))))
          (is (= 2 (count @inv))
              "invoked for directory and for file")))))

  (h/with-tmp-dir dir
    (let [src (fs/create-dir (fs/path dir "src"))
          a (fs/path dir (str "archive." (:archive-type default-opts)))
          dest (fs/create-dir (fs/path dir "dest"))]
      (is (nil? (spit (fs/file src "test.txt") "This is a test")))

      (testing "without name resolver"
        (with-open [os (io/output-stream (fs/file a))]
          (is (nil? (sut/archive (assoc default-opts :output-stream os)
                                 src))))
        
        (testing "extracts full paths from archive in destination dir"
          (with-open [i (io/input-stream (fs/file a))]
            (let [r (cu/unarchive (assoc default-opts :input-stream i)
                                  dest)
                  p (fs/path dest src "test.txt")]
              (is (= 2 (count r)))
              (is (fs/exists? p))
              (is (= "This is a test" (slurp (fs/file p))))))))

      (testing "when stripping full path"
        (with-open [os (io/output-stream (fs/file a))]
          (is (nil? (sut/archive (merge default-opts {:output-stream os
                                                      :entry-name-resolver (sut/strip-dir src)})
                                 src))))
        
        (testing "extracts files from archive in destination dir"
          (with-open [i (io/input-stream (fs/file a))]
            (let [r (cu/unarchive (assoc default-opts :input-stream i)
                                  dest)
                  p (fs/path dest "test.txt")]
              (is (= 2 (count r)))
              (is (fs/exists? p))
              (is (= "This is a test" (slurp (fs/file p)))
                  (str p)))))

        (testing "when predicate, only extracts file that match it"
          (fs/delete (fs/path dest "test.txt"))
          (with-open [i (io/input-stream (fs/file a))]
            (let [r (cu/unarchive (merge default-opts {:input-stream i
                                                       :filter-fn (constantly false)})
                                  dest)
                  p (fs/path dest "test.txt")]
              (is (= 0 (count r)))
              (is (not (fs/exists? p)))))))

      (testing "extracts subdirs"
        (let [sub (fs/create-dirs (fs/path src "subdir"))]
          (is (fs/exists? sub))
          (is (nil? (spit (fs/file sub "sub.txt") "This is in a subdir")))
          (is (fs/exists? (fs/path sub "sub.txt")))
          (with-open [os (io/output-stream (fs/file a))]
            (is (nil? (sut/archive (merge default-opts {:output-stream os
                                                        :entry-name-resolver (sut/strip-dir src)})
                                   src))))
          (with-open [i (io/input-stream (fs/file a))]
            (let [r (cu/unarchive (assoc default-opts :input-stream i)
                                  dest)
                  p (fs/path dest "subdir" "sub.txt")]
              (is (fs/exists? p)))))))))

(defn verify-permissions [default-opts]
  (h/with-tmp-dir dir
    (let [src (fs/create-dir (fs/path dir "src"))
          a (fs/path dir (str "archive." (:archive-type default-opts)))
          dest (fs/create-dir (fs/path dir "dest"))]
      (testing "retains file permissions"
        (let [p (fs/path src "test.sh")]
          (is (nil? (spit (fs/file p) "this is an executable file")))
          ;; Make file executable
          (is (= p (fs/set-posix-file-permissions
                    p
                    (conj (set (fs/posix-file-permissions p)) PosixFilePermission/OWNER_EXECUTE))))
          (is (fs/executable? p)))
        (with-open [os (io/output-stream (fs/file a))]
          (is (nil? (sut/archive (merge default-opts {:output-stream os
                                                      :entry-name-resolver (sut/strip-dir src)})
                                 src))))
        (with-open [i (io/input-stream (fs/file a))]
          (let [r (cu/unarchive (assoc default-opts :input-stream i)
                                dest)
                p (fs/path dest "test.sh")]
            (is (pos? (count r)))
            (is (fs/exists? p))
            (is (fs/executable? p)))))

      (testing "extracts symlinks correctly"
        (is (nil? (spit (fs/file src "test.txt") "This is a test")))
        (let [l (fs/create-sym-link (fs/path src "testlink") "test.txt")]
          (with-open [os (io/output-stream (fs/file a))]
            (is (nil? (sut/archive (merge default-opts {:output-stream os
                                                        :entry-name-resolver (sut/strip-dir src)})
                                   src))))
          (with-open [i (io/input-stream (fs/file a))]
            (let [r (cu/unarchive (assoc default-opts :input-stream i)
                                  dest)
                  p (fs/path dest "testlink")]
              (is (pos? (count r)))
              (is (fs/exists? p))
              (is (fs/sym-link? p)))))))))

(deftest archive
  (testing "tar files"
    (let [opts {:compression "gz"
                :archive-type "tar"}]
      (verify-archive opts)
      (verify-permissions opts)))

  (testing "zip files"
    ;; Even though zip files technically support symlinks and file permissions, the
    ;; information doesn't seem to be loaded by commons compress.
    (verify-archive {:archive-type "zip"})))
