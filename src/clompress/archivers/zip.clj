(ns clompress.archivers.zip
  "Archiver implementation for zipfiles"
  (:require [clojure.java.io :as io]
            [clompress.core :as cc])
  (:import [org.apache.commons.compress.archivers.zip ZipArchiveOutputStream]))

(extend-type ZipArchiveOutputStream
  cc/Archiver
  (make-entry [this entry entry-name]
    (.createArchiveEntry this entry entry-name)))

(defn make-archiver [output]
  (ZipArchiveOutputStream. output))
