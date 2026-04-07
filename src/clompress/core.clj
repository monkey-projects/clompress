(ns clompress.core)

(defprotocol Archiver
  (make-entry [this entry entry-name]
    "Creates a new archive entry in the archive for the path"))

(defprotocol Unarchiver
  (extract-entry [this entry dest]
    "Extracts the archive entry to the given destination"))
