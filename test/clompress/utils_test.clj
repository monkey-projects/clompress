(ns clompress.utils-test
  (:require [clojure.test :refer [deftest testing is]]
            [clompress.utils :as sut]))

(deftest file-mode
  (testing "can convert from posix permissions to file mode and back"
    (let [mode (Integer/parseInt "755" 8)
          posix (sut/mode->posix mode)]
      (is (= 7 (count posix)))
      (is (contains? posix java.nio.file.attribute.PosixFilePermission/OWNER_READ))
      (is (contains? posix java.nio.file.attribute.PosixFilePermission/OWNER_WRITE))
      (is (contains? posix java.nio.file.attribute.PosixFilePermission/OWNER_EXECUTE))
      (is (not (contains? posix java.nio.file.attribute.PosixFilePermission/OTHERS_WRITE)))
      (is (contains? posix java.nio.file.attribute.PosixFilePermission/GROUP_EXECUTE))
      (is (= mode (sut/posix->mode posix)))))

  (testing "can process extended modes"
    (is (not-empty (sut/mode->posix (Integer/parseInt "100644" 8))))))
