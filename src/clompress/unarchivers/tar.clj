(ns clompress.unarchivers.tar
  "Unarchiver implementation for tarballs"
  (:require [babashka.fs :as fs]
            [clojure.java.io :as io]
            [clompress
             [core :as c]
             [utils :as u]])
  (:import org.apache.commons.compress.archivers.tar.TarArchiveInputStream))

(extend-type TarArchiveInputStream
  c/Unarchiver
  (extract-entry [this e dest]
    (let [f (fs/file dest (.getName e))]
      (cond
        (.isDirectory e)
        {:type :dir
         :dest (fs/create-dirs f)
         :entry e}

        (.isSymbolicLink e)
        (let [p (fs/create-dirs (.getParentFile f))]
          {:type :sym-link
           :dest (fs/create-sym-link f (.getLinkName e))
           :entry e})
        
        (.isFile e)
        (let [p (fs/create-dirs (.getParentFile f))]
          (with-open [os (io/output-stream f)]
            (io/copy this os))
          ;; Mode field contains file permissions in octal
          {:type :file
           :dest (fs/set-posix-file-permissions f (u/mode->posix (.getMode e)))
           :entry e})

        :else
        {:type :unsupported
         :entry e}))))
