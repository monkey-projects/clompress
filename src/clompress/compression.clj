(ns clompress.compression
  (:require [clojure.java.io :as io])
  (:import [org.apache.commons.compress.compressors CompressorStreamFactory]))

(defn with-compression [stream compressor]
  '"Returns output stream that wrapped with compression."
  (let [factory (CompressorStreamFactory.)]
    (.createCompressorOutputStream factory compressor stream)))

(defn with-decompression [stream compressor]
  '"Returns output stream that wrapped with compression."
  (let [factory (CompressorStreamFactory.)]
    (.createCompressorInputStream factory compressor stream)))

(defn compress [input-stream output-stream compression]
  '"Redirects input stream to output stream with compression."
  (with-open [compressed-stream (with-compression output-stream compression)]
    (io/copy input-stream compressed-stream)))

(defn decompress [input-stream output-stream compression]
  '"Redirects input stream to output stream with decompression."
  (with-open [decompressed-stream (with-decompression input-stream compression)]
    (io/copy decompressed-stream output-stream)))

(def available-compressions ["bzip2" 
                             "xz"
                             "lzma"
                             "lz4-framed"
                             "lz4-block"
                             "gz"])
                              
