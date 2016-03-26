Java Opus and H264 Wrapper
==========================

The Opus wrapper was created with https://code.google.com/p/jnaerator/ and
sligthly adapted. The H264 wrapper is used the webcam API from https://github.com/sarxos/webcam-capture 
and the H264 codec is from https://github.com/jcodec/jcodec. This library requires Java8.

Binaries found here:

* win 32bit: from source using mingw32
 - ./configure --host=i686-w64-mingw32 --target=i686-w64-mingw32 CFLAGS=-Os
* win 64bit: from source using mingw32
 - ./configure --host=x86_64-w64-mingw32 --target=x86_64-w64-mingw32
CFLAGS=-Os
* linux 64bit: http://packages.ubuntu.com/xenial/amd64/libopus0/download
* linux 32bit: http://packages.ubuntu.com/xenial/i386/libopus0/download
* macosx: https://www.macports.org/ports.php?by=library&substr=libopus

How to use the opus/h264 wrapper:

Either get the "jar":http://tomp2p.net/dev/mvn/net/tomp2p/opus-h264-webcam-wrapper/1.0.0/

or use maven with

```
<repositories>
  <repository>
    <id>tomp2p.net</id>
    <url>http://tomp2p.net/dev/mvn/</url>
  </repository>
</repositories>

...

<dependency>
  <groupId>net.tomp2p</groupId>
  <artifactId>opus-h264-webcam-wrapper</artifactId>
  <version>1.0.2</version>
</dependency>
```

or with gradle

```
repositories {
    maven {
        url "http://tomp2p.net/dev/mvn/"
    }
}

...

dependencies {
    compile 'net.tomp2p:opus-h264-webcam-wrapper:1.0.2'
}
```

and run AudioVideoExample.java (main class)
