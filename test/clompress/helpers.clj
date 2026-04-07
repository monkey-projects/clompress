(ns clompress.helpers
  "Test helpers"
  (:require [babashka.fs :as fs]))

(defn with-tmp-dir* [f]
  (let [dir (fs/create-temp-dir)]
    (try
      (f dir)
      (finally
        (fs/delete-tree dir)))))

(defmacro with-tmp-dir [f & body]
  `(with-tmp-dir* (fn [~f] ~@body)))

