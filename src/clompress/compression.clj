(ns clompress.compression
  (:require [clojure.java.io :as io])
  (:import [org.apache.commons.compress.compressors CompressorStreamFactory CompressorInputStream
            CompressorOutputStream]
           [java.io InputStream OutputStream]))

(set! *warn-on-reflection* true)

(defn ^CompressorOutputStream with-compression [^OutputStream stream ^String compressor]
  '"Returns output stream that is wrapped with compression."
  (let [factory (CompressorStreamFactory.)]
    (.createCompressorOutputStream factory compressor stream)))

(defn ^CompressorInputStream with-decompression [^InputStream stream ^String compressor]
  '"Returns input stream that is wrapped with decompression."
  (let [factory (CompressorStreamFactory.)]
    (.createCompressorInputStream factory compressor stream)))

(defn compress [^InputStream input-stream ^OutputStream output-stream ^String compression]
  '"Redirects input stream to output stream with compression."
  (with-open [compressed-stream (with-compression output-stream compression)]
    (io/copy input-stream compressed-stream)))

(defn decompress [^InputStream input-stream ^OutputStream output-stream ^String compression]
  '"Redirects input stream to output stream with decompression."
  (with-open [decompressed-stream (with-decompression input-stream compression)]
    (io/copy decompressed-stream output-stream)))

(def available-compressions ["bzip2" 
                             "xz"
                             "lzma"
                             "lz4-framed"
                             "lz4-block"
                             "gz"])
                              
