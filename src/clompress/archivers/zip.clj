(ns clompress.archivers.zip
  "Archiver implementation for zipfiles"
  (:require [babashka.fs :as fs]
            [clojure.java.io :as io]
            [clompress
             [core :as cc]
             [utils :as u]])
  (:import [org.apache.commons.compress.archivers.zip ZipArchiveOutputStream ZipArchiveEntry]
           [java.io File OutputStream]))

(set! *warn-on-reflection* true)

(extend-type ZipArchiveOutputStream
  cc/Archiver
  (make-entry [this ^File entry ^String entry-name]
    (doto (.createArchiveEntry this entry entry-name)
      (.setUnixMode (-> (fs/file entry)
                        (fs/posix-file-permissions)
                        (u/posix->mode))))))

(defn make-archiver [^OutputStream output]
  (ZipArchiveOutputStream. output))
