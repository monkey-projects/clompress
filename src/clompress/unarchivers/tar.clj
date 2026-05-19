(ns clompress.unarchivers.tar
  "Unarchiver implementation for tarballs"
  (:require [babashka.fs :as fs]
            [clompress.core :as c]
            [clompress.unarchivers.common :as uc])
  (:import (org.apache.commons.compress.archivers.tar TarArchiveInputStream TarArchiveEntry)))

(set! *warn-on-reflection* true)

(extend-type TarArchiveInputStream
  c/Unarchiver
  (extract-entry [this ^TarArchiveEntry e dest]
    (let [f (fs/file dest (.getName e))]
      (cond
        (.isDirectory e)
        (uc/extract-dir e f)

        (.isSymbolicLink e)
        (uc/extract-sym-link e (.getLinkName e) f)
        
        (.isFile e)
        (uc/extract-file e this f (.getMode e))

        :else
        (uc/unsupported-entry e)))))
