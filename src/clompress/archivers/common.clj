(ns clompress.archivers.common
  "Common functionality shared among archivers"
  (:require [babashka.fs :as fs]
            [clojure.java.io :as io]
            [clompress.core :as cc])
  (:import java.nio.file.Files java.io.File
           org.apache.commons.compress.archivers.ArchiveOutputStream))

(set! *warn-on-reflection* true)

(defn strip-leading-slash
  [path]
  (case (first path)
    \/ (subs path 1)
    path))

(defn- get-entry-name-resolver [{:keys [entry-name-resolver]}]
  (or entry-name-resolver 
      strip-leading-slash))

(defn- add-entry-to-archive [^ArchiveOutputStream archiver ^File entry before-add entry-name]
  (let [path (.toPath entry)
        archive-entry (cc/make-entry archiver entry entry-name)]
    (when before-add
      (before-add archive-entry))
    (.putArchiveEntry archiver archive-entry)
    (try
      (when (and (not (Files/isSymbolicLink path)) (.isFile entry))
        (io/copy entry archiver))
      (catch Exception ex
        (throw ex))
      (finally
        (.closeArchiveEntry archiver)))))

(defn- add-path-to-archive [archiver path {:keys [before-add] :as opts}]
  (let [entry (fs/file path)
        get-entry-name-from-path (get-entry-name-resolver opts)]
    (if (.isDirectory entry) 
      (->> (file-seq entry)
           (map #(->> (.getPath ^File %1)
                      (get-entry-name-from-path)
                      (add-entry-to-archive archiver %1 before-add)))
           (doall))
      (add-entry-to-archive archiver entry before-add (get-entry-name-from-path path)))))

(defn- add-all [archiver opts paths]
  (doseq [path paths]
    (add-path-to-archive archiver path opts)))

(defn archive-paths [^ArchiveOutputStream archiver opts paths]
  (add-all archiver opts paths)
  (.finish archiver))
