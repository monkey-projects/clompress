(ns clompress.archivers.zip
  "Archiver implementation for zipfiles"
  (:require [clojure.java.io :as io]
            [clompress.archivers.common :as c])
  (:import [org.apache.commons.compress.archivers.zip ZipArchiveOutputStream]))

(defrecord ZipArchiver [archive]
  c/Archiver
  (make-entry [this entry entry-name]
    (.createArchiveEntry archive entry entry-name))
  (get-archive [this]
    archive))

(defn make-archiver [output]
  (ZipArchiveOutputStream. output))
