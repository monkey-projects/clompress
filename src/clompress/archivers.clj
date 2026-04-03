(ns clompress.archivers
  (:require [clompress.archivers
             [common :as c]
             [tar :as tar]
             [zip :as zip]]
            [clompress.compression :refer [with-compression]]))

(defn- get-output-stream [{:keys [output-stream compression]}] 
  (if (nil? compression)
    output-stream
    (with-compression output-stream compression)))

(defmulti make-archiver :archive-type)

(defmethod make-archiver :default [options]
  (throw (ex-info "Archiver is not recognized" {:options options})))

(defmethod make-archiver "tar" [options]
  (tar/make-archiver (get-output-stream options)))

(defmethod make-archiver "zip" [options]
  (zip/make-archiver (get-output-stream options)))

(defn archive
  "Archives specified files in paths.
   Options:
     - `output-stream`: where to write the archived bytes to
     - `compression`: type of compression, e.g. `gz`
     - `archive-type`: type of archive, e.g. `tar`
     - `entry-name-resolver`: 1-arity fn that takes the input path and outputs path to use in the archive
     - `before-add`: 1-arity fn that can do some changes on the archive entry before storing.  Useful to set file permissions for example."
  [options & paths]
  (-> (make-archiver options)
      (c/archive-paths options paths)))

(comment archive {:archive-type "tar" 
                  :output-stream (io/output-stream "my-test.tar")} 
         "<absolute-path>")
