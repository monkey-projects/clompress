(ns clompress.unarchivers
  "Functions for extracting files from archives"
  (:require [babashka.fs :as fs]
            [clojure.java.io :as io]
            [clojure.tools.logging :as log]
            [clompress
             [compression :as cc]
             [utils :as u]]
            [clompress.core :as c]
            [clompress.unarchivers
             [tar :as tar]])
  (:import [org.apache.commons.compress.archivers ArchiveStreamFactory]))

(def stream-factory (ArchiveStreamFactory.))

(def stream-factories
  {"tar" ArchiveStreamFactory/TAR
   "zip" ArchiveStreamFactory/ZIP})

(def available-unarchivers (keys stream-factories))

(defn make-unarchiver
  "Creates an unarchiver for the given input stream and type (see `available-unarchivers`)."
  [is type]
  (if-let [sf (get stream-factories type)]
    (.createArchiveInputStream stream-factory sf is)
    (throw (ex-info "Unsupported archive type" {:archive-type type}))))

(defn- decompress
  "Decompresses a source file.  Returns an input stream that will contain the
   decompressed archive."
  [src type]
  (cond-> src
    type (cc/with-decompression type)))

(defn entry-seq
  "Given an archive input stream, returns a lazy seq of its entries (as an `ArchiveEntry`)."
  [ai]
  (take-while some? (repeatedly #(.getNextEntry ai))))

(defn- extract-archive
  "Unarchives the given (uncompressed) input stream to the given output location.
   `dest` is supposed to be a directory where the files can be written to.  Only
   files matching the given predicate will be unarchived.  Returns a map that
   contains the destination directory and the names of the extracted entries."
  [is type dest pred]
  (fs/create-dirs dest)
  (with-open [ai (make-unarchiver is type)]
    (->> (entry-seq ai)
         (filter (comp pred (memfn getName)))
         (map #(c/extract-entry ai % dest))
         (doall))))

(defn unarchive
  "'Batteries included' function that unarchives the given `input-stream` into the `dest` 
   directory.  This makes use of other functions in this ns, which may be more suitable
   for specific uses.  Returns a list of extracted entries.

   Options:
    - `input-stream`: the input stream to read from (required)
    - `archive-type`: what type of archive this is (e.g. 'tar')
    - `compression`: what kind of decompression to use, if any (e.g. `gz`)
    - `filter-fn`: predicate to decide which files to extract"
  [{src :input-stream :as opts} dest]
  (with-open [in (io/input-stream src)
              ds (decompress in (:compression opts))]
    (extract-archive ds (:archive-type opts) dest (or (:filter-fn opts) (constantly true)))))
