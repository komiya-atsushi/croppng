CropPNG
=======

[![CircleCI](https://circleci.com/gh/komiya-atsushi/croppng.svg?style=svg)](https://circleci.com/gh/komiya-atsushi/croppng)
[![Download](https://api.bintray.com/packages/komiya-atsushi/maven/croppng/images/download.svg)](https://bintray.com/komiya-atsushi/maven/croppng/_latestVersion)

Fast PNG cropping and resizing (upscaling) library.


Features
--------

- Fast. See [Benchmark result](#throughput) for details.
- Does not depend on AWT (`java.awt.image.*`).
- Supports transparent/translucent PNG.


Prerequisites
-------------

- Java 11 or later


Getting started
---------------

### Download

You can download artifacts from [Bintray](https://bintray.com/beta/#/komiya-atsushi/maven/croppng?tab=files). 

### Using with Maven

If you are using Maven, you have to configure the additional repository in the `repositories` element as follows: 

```xml
<project>
  <repositories>
    <repository>
      <id>bintray-komiya-atsushi-maven</id>
      <url>http://dl.bintray.com/komiya-atsushi/maven</url>
    </repository>
  </repositories>

  <dependencies>
    <dependency>
      <groupId>me.k11i</groupId>
      <artifactId>croppng</artifactId>
      <version>0.1.1</version>
    </dependency>
  </dependencies>
</project>
```

### Using with Gradle

Similarly, if you are using Gradle, you have to declare custom Maven repository as follows:

```gradle
repositories {
  mavenCentral()
  maven {
    url 'http://dl.bintray.com/komiya-atsushi/maven'
  }
}

dependencies {
  implementation 'me.k11i:croppng:0.1.1'
}
```

### API

CropPNG provides static methods that return newly-constructed or cached `CropPng` instance in the `ThreadLocal<SoftReference<CropPng>>`. 

```java
public static CropPng defaultLevel();
public static CropPng compressionLevel(int level);
```

Also you can call constructors directly.

```java
public CropPng();
public CropPng(int level);
public CropPng(Deflater deflater);
```

To crop (and resize) subimage from PNG image represented by byte array, call `CropPng#crop()` method
and you can get `ByteArray` object that contains extracted and resized subimage.
    
```java
public ByteBuffer crop(byte[] src, int x, int y, int width, int height, int scaleFactor);
```

 
### Example

```java
import java.io.*;
import java.nio.ByteBuffer;
import java.nio.file.*;
import java.util.Arrays;

public class Example {
  public static void main(String[] args) throws IOException {
    byte[] src = Files.readAllBytes(Path.of("path/to/image.png"));

    // Crop rectangle (10, 20, 10 + 100, 20 + 200) and scale to 4x.
    // Compression level is 6 (default).
    ByteBuffer cropped = CropPng.defaultLevel()
            .crop(src,
                    10,   // x
                    20,   // y
                    100,  // width
                    120,  // height
                    4);   // scale factor

    // Specify compression level 1 (fastest).
    cropped = CropPng.compressionLevel(1)
            .crop(src,
                    10,   // x
                    20,   // y
                    100,  // width
                    120,  // height
                    4);   // scale factor

    // Get raw bytes.
    byte[] bytes = Arrays.copyOfRange(cropped.array(), cropped.arrayOffset(), cropped.limit());

    // Write to file.
    try (FileOutputStream out = new FileOutputStream("out.png")) {
      out.write(
              cropped.array(),
              cropped.arrayOffset(),
              cropped.limit() - cropped.arrayOffset());
    }
  }
}
```


Benchmark
---------

To run JMH benchmark, execute `make bench`.

### Throughput

![Throughput](https://docs.google.com/spreadsheets/d/e/2PACX-1vSBkU-Y8JfNnomckAptVQd6Itbk4qpX68p4Zh-4izBjzm1P195vEB3sZIhzdX-rcvdhZqs98jRCab8P/pubchart?oid=974146540&format=image)

- `CropPng` outperforms by at least 3 times AWT-based method.
- Using a cached `CropPng` instance that is provided by `CropPng.compressionLevel(1)` is over 10 times faster than AWT-based method.  

### File size

![File size](https://docs.google.com/spreadsheets/d/e/2PACX-1vSBkU-Y8JfNnomckAptVQd6Itbk4qpX68p4Zh-4izBjzm1P195vEB3sZIhzdX-rcvdhZqs98jRCab8P/pubchart?oid=1619670959&format=image)


Limitation
----------

The current version has some limitations:

- Only supports bit depth of 8.
- Only supports indexed color type.
- Does not support interlaced PNG.


License
-------

CropPNG is licensed under MIT License. See [LICENSE](LICENSE) for details.
