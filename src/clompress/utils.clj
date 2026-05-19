(ns clompress.utils
  (:import [java.nio.file.attribute PosixFilePermission]))

(set! *warn-on-reflection* true)

(def posix-permissions (PosixFilePermission/values))

(defn mode->posix
  "Converts from file mode number (converted from octal) to a set of posix file permissions"
  [mode]
  (let [n (dec (count posix-permissions))]
    (->> (seq posix-permissions)
         (reduce (fn [s ^PosixFilePermission fp]
                   (cond-> s
                     (bit-test mode (- n (.ordinal fp)))
                     (conj fp)))
                 #{}))))

(defn posix->mode
  "Converts a set of posix file permissions to a mode number"
  [posix]
  (let [n (dec (count posix-permissions))]
    (reduce (fn [m ^PosixFilePermission fp]
              (bit-set m (- n (.ordinal fp))))
            0
            posix)))
