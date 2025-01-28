# :file_folder: clompress

[![Clojars Project](https://img.shields.io/clojars/v/com.monkeyprojects/clompress.svg)](https://clojars.org/com.monkeyprojects/clompress)

Clompress is easy to use library for working with archives, compression and
decompression in Clojure. Currently acts like wrapper for library _Apache
Commons Compress_

## Features

Directories are readed **recursively.**

### Supported archives

- [x] TAR `"tar"`
- [x] ZIP `"zip"`

### Supported compressions

- [x] BZIP2
- [x] GZIP
- [x] DEFLATE
- [x] LZ4 (BLOCK, FRAMED)
- [x] LZMA
- [x] XZ

For available compressions:

```clj
clompress.compression/available-compressions
```

## Installation

### Lein/Boot

```
[com.monkeyprojects/clompress "0.1.1"]
```

### Clojure CLI/deps.edn

```
com.monkeyprojects/clompress {:mvn/version "0.1.1"}
```

## Examples

### Creating archive without compression

```clj
(clompress/archive {
	:output-stream (clojure.java.io/output-stream "my-archive.tar")
	:archive-type "tar"} ; for zip set :archive-type "zip"
	"directory1/" "directory2/file1.txt" "file2.txt")
```

### Creating archive with compression `bzip2`

```clj
(clompress/archive {
	:output-stream (clojure.java.io/output-stream "my-archive.tar.bz2")
	:compression "bzip2"
	:archive-type "tar"}
	"directory1/" "directory2/file1.txt" "file2.txt")
```

### Compressing file

```clj
(clompress.compression/compress
	(clojure.java.io/input-stream "file-to-compress.txt")
	(clojure.java.io/output-stream "compressed-file.txt.gz")
	"gz")
```

### Decompressing file

```clj
(clompress.compression/decompress
	(clojure.java.io/input-stream "compressed-file.txt.gz")
	(clojure.java.io/output-stream "decompressed-file.txt")
	"gz")
```

### Compressing string

```clj
(with-open [input-stream (java.io.ByteArrayInputStream.
                           (.getBytes "test-data"))]
  (with-open [output-stream (java.io.ByteArrayOutputStream.)]
    (clompress.compression/compress input-stream output-stream "bzip2")))
```

### Decompressing string

```clj
(with-open [input-stream (java.io.ByteArrayInputStream.
                           (.getBytes compressed-string))]
  (with-open [output-stream (java.io.ByteArrayOutputStream.)]
    (clompress.compression/decompress input-stream output-stream "bzip2")
		(.toString output-stream)))
```

## Hooks

In the archiver options, you can pass to additional hook functions to influence
the way files are added to the archives.

 - `entry-name-resolver` takes the input path and returns the path to use in the archive
 - `before-add` takes the `ArchiveEntry` object an allows you to do some modifications to it before it is put into the archive.

The `before-add` is especially useful to set file permissions on the archive entry, since
this information is system dependent and not stored automatically.

## License

[Clompress is licensed under MIT license.](./LICENSE)
