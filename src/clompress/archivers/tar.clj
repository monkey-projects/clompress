(ns clompress.archivers.tar
  "Archiver implementation for tarballs"
  (:require [clojure.java.io :as io]
            [clompress.archivers.common :as c])
  (:import [java.nio.file Files LinkOption]
           [org.apache.commons.compress.archivers.tar TarArchiveEntry TarArchiveOutputStream TarConstants]))

(defn- make-tar-archive [output]
  (doto (TarArchiveOutputStream. output)
    (.setLongFileMode TarArchiveOutputStream/LONGFILE_POSIX)
    (.setBigNumberMode TarArchiveOutputStream/BIGNUMBER_POSIX)))

(defrecord TarArchiver [archive]
  c/Archiver
  (make-entry [this entry entry-name]
    (let [path (.toPath entry)]
      (if (Files/isSymbolicLink path)
        (doto (TarArchiveEntry. entry-name TarConstants/LF_SYMLINK)
          (.setLinkName (str (.toRealPath path (make-array LinkOption 0)))))
        (.createArchiveEntry archive entry entry-name))))
  (get-archive [this]
    archive))

(defn make-archiver [output]
  (->TarArchiver (make-tar-archive output)))
