(ns clompress.archivers.common
  "Common functionality shared among archivers"
  (:require [clojure.java.io :as io])
  (:import java.nio.file.Files))

(defprotocol Archiver
  (make-entry [this entry entry-name]
    "Creates a new archive entry in the archive for the path")
  (get-archive [this]
    "Returns the underlying archive stream for this archiver"))

(defn- default-entry-name-resolver [path]
  (case (first path)
    \/ (subs path 1)
    :else path))

(defn- get-entry-name-resolver [{:keys [entry-name-resolver]}]
  (or entry-name-resolver 
      default-entry-name-resolver))

#_(defn- write-file-to-archive [archive entry]
  (with-open [in (io/input-stream entry)]
    (io/copy in archive))) 

(defn- add-entry-to-archive [archiver entry before-add entry-name]
  (let [path (.toPath entry)
        archive-entry (make-entry archiver entry entry-name)
        archive (get-archive archiver)]
    (when before-add
      (before-add archive-entry))
    (.putArchiveEntry archive archive-entry)
    (try
      (when (and (not (Files/isSymbolicLink path)) (.isFile entry))
        (io/copy entry archive))
      (catch Exception ex
        (throw ex))
      (finally
        (.closeArchiveEntry archive)))))

(defn- add-path-to-archive [archiver path {:keys [before-add] :as opts}]
  (let [entry (io/file path)
        get-entry-name-from-path (get-entry-name-resolver opts)]
    (if (.isDirectory  entry) 
      (->> (file-seq entry)
           (map #(->> (.getPath %1)
                      (get-entry-name-from-path)
                      (add-entry-to-archive archiver %1 before-add)))
           (doall))
      (add-entry-to-archive archiver entry before-add (get-entry-name-from-path path)))))

(defn- add-all [archiver opts paths]
  (doseq [path paths]
    (add-path-to-archive archiver path opts)))

(defn archive-paths [archiver opts paths]
  (with-open [archive (get-archive archiver)]
    (add-all archiver opts paths)
    (.finish archive)))
