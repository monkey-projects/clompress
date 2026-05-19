(ns clompress.unarchivers.zip
  "Unarchiver implementation for zipfiles"
  (:require [babashka.fs :as fs]
            [clompress.core :as cc]
            [clompress.unarchivers.common :as uc])
  (:import [org.apache.commons.compress.archivers.zip ZipArchiveInputStream ZipArchiveEntry]))

(set! *warn-on-reflection* true)

(extend-type ZipArchiveInputStream
  cc/Unarchiver
  (extract-entry [this ^ZipArchiveEntry e dest]
    (let [f (fs/file dest (.getName e))]
      (cond
        (.isDirectory e)
        (uc/extract-dir e f)

        (.isUnixSymlink e)
        (uc/extract-sym-link e (slurp this) f)
        
        :else
        (uc/extract-file e this f (.getUnixMode e))))))
