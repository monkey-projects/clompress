(ns clompress.unarchivers.common
  (:require [babashka.fs :as fs]
            [clojure.java.io :as io]
            [clompress.utils :as u]))

(defn- valid-entry [type e dest]
  (zipmap [:type :dest :entry] [type dest e]))

(def dir-entry (partial valid-entry :dir))
(def sym-link-entry (partial valid-entry :sym-link))
(def file-entry (partial valid-entry :file))

(defn unsupported-entry [e]
  {:type :unsupported
   :entry e})

(defn extract-dir [e dest]
  (dir-entry e (fs/create-dirs dest)))

(defn extract-sym-link [e link dest]
  (sym-link-entry e (fs/create-sym-link dest link)))

(defn extract-file [e src dest mode]
  (with-open [os (io/output-stream dest)]
    (io/copy src os))
  ;; Mode field contains file permissions in octal
  (file-entry e (cond-> dest
                  (pos? mode) (fs/set-posix-file-permissions (u/mode->posix mode)))))
