(ns clompress.archivers.tar
  "Archiver implementation for tarballs"
  (:require [babashka.fs :as fs]
            [clojure.java.io :as io]
            [clompress.core :as c]
            [clompress.utils :as u])
  (:import [java.nio.file Files LinkOption]
           java.io.File
           [org.apache.commons.compress.archivers.tar TarArchiveEntry TarArchiveOutputStream TarConstants]))

(set! *warn-on-reflection* true)

(defn make-archiver [output]
  (doto (TarArchiveOutputStream. output)
    (.setLongFileMode TarArchiveOutputStream/LONGFILE_POSIX)
    (.setBigNumberMode TarArchiveOutputStream/BIGNUMBER_POSIX)))

(defn set-entry-mode
  "`before-add` handler that sets the TAR entry file mode using the posix file permissions.
   Note that this is applied by default by the `TarArchiver`."
  [^TarArchiveEntry entry]
  (.setMode entry (-> (.getFile entry)
                      (fs/posix-file-permissions)
                      (u/posix->mode))))

(extend-type TarArchiveOutputStream
  c/Archiver
  (make-entry [this ^File entry ^String entry-name]
    (let [path (.toPath entry)]
      (if (Files/isSymbolicLink path)
        (doto (TarArchiveEntry. entry-name TarConstants/LF_SYMLINK)
          (.setLinkName (str (.toRealPath path (make-array LinkOption 0)))))
        (doto (.createArchiveEntry this entry entry-name)
          (set-entry-mode))))))
