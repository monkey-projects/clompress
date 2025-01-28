(ns clompress.archivers
  (:require
    [clojure.java.io :as io]
    [clojure.tools.logging :as log]
    [clompress.compression :refer [with-compression]])
  (:import
    [org.apache.commons.compress.archivers.tar TarArchiveOutputStream]
    [org.apache.commons.compress.archivers.zip ZipArchiveOutputStream]
    [org.apache.commons.io IOUtils])
  (:gen-class))

(defn- default-entry-name-resolver [path]
  (case (first path)
    \/ (subs path 1)
    :else path))

(defn- get-entry-name-resolver [{:keys [entry-name-resolver]}]
  (or entry-name-resolver 
      default-entry-name-resolver))

(defn- write-file-to-archive [archive entry]
  (IOUtils/copy (io/input-stream entry) archive)) 

(defn- add-entry-to-archive [archive entry before-add entry-name]
  (let [archive-entry (.createArchiveEntry archive entry entry-name)]
    (when before-add
      (before-add archive-entry))
    (.putArchiveEntry archive archive-entry)
    (try
      (when (.isFile entry)
        (write-file-to-archive archive entry))
      (catch Exception ex
        (log/error "Failed to write archive entry" entry-name ex)
        (throw ex))
      (finally
        (.closeArchiveEntry archive)))))

(defn- add-path-to-archive [archive path {:keys [before-add] :as opts}]
  (let [entry (io/file path)
        get-entry-name-from-path (get-entry-name-resolver opts)]
    (if (.isDirectory  entry) 
      (->> (file-seq entry)
           (map #(->> (.getPath %1)
                      (get-entry-name-from-path)
                      (add-entry-to-archive archive %1 before-add)))
           (doall))
      (add-entry-to-archive archive entry before-add (get-entry-name-from-path path)))))

(defn- add-all [archive opts paths]
  (doseq [path paths]
    (add-path-to-archive archive path opts)))

(defn- archiver [maker outputstream opts paths]
  (with-open [archive (maker outputstream)]
    (add-all archive opts paths)
    (.finish archive)))

(defn- make-tar-archive [output]
  (doto (TarArchiveOutputStream. output)
    (.setLongFileMode TarArchiveOutputStream/LONGFILE_POSIX)
    (.setBigNumberMode TarArchiveOutputStream/BIGNUMBER_POSIX)))

(def tar-archiver
  (partial archiver make-tar-archive))

(def zip-archiver
  (partial archiver #(ZipArchiveOutputStream. %)))

(defn- get-output-stream [{:keys [output-stream compression]}] 
    (if (nil? compression)
      output-stream
      (with-compression output-stream compression)))

(def ^:private get-archiver
  { "tar" tar-archiver
    "zip" zip-archiver})

(defn archive
  "Archives specified files in paths.
   Options:
     - `output-stream`: where to write the archived bytes to
     - `compression`: type of compression, e.g. `gz`
     - `archive-type`: type of archive, e.g. `tar`
     - `entry-name-resolver`: 1-arity fn that takes the input path and outputs path to use in the archive
     - `before-add`: 1-arity fn that can do some changes on the archive entry before storing.  Useful to set file permissions for example."
  [options & paths]
  (if-let [archiver (get-archiver (options :archive-type))]
    (archiver 
     (get-output-stream options) 
     options
     paths)
    (throw (ex-info "Archiver is not recognized" {:options options :paths paths}))))

(comment archive {:archive-type "tar" 
                  :output-stream (io/output-stream "my-test.tar")} 
         "<absolute-path>")
