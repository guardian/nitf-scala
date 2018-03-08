# nitf-scala
[![License: Apache-2.0](https://img.shields.io/github/license/guardian/nitf-scala.svg)](https://github.com/guardian/nitf-scala/blob/master/LICENSE)
[![Build Status](https://travis-ci.org/guardian/nitf-scala.svg?branch=master)](https://travis-ci.org/guardian/nitf-scala)
<!--
![Maven Metadata](https://img.shields.io/maven-metadata/v/http/central.maven.org/maven2/com/gu/nitf-scala/maven-metadata.xml.svg)
-->

Scala library to parse and generate [News Industry Text Format](https://iptc.org/standards/nitf/) files,
based on [ScalaXB](http://scalaxb.org).

This library supports versions 3.3 to 3.6.  
(Previous versions don't have an XSD.)

## Usage

To use this library, add the following dependencies to your project:
```scala
val scalaXmlVersion = "1.1.0"
libraryDependencies ++= Seq(
  "org.scala-lang.modules" %% "scala-xml" % scalaXmlVersion,
  "org.scala-lang.modules" %% "scala-parser-combinators" % scalaXmlVersion
)
```

Example usage:
```scala
import com.gu.nitf.model._
import com.gu.nitf.scalaxb._

val doc = scalaxb.fromXML[Nitf](
  <nitf>
    <head><title>Hello</title></head>
    <body><body.content>World</body.content></body>
  </nitf>
)
// or
val xml = scalaxb.toXML(doc, namespace = None, elementLabel = Some("nitf"), scope = defaultScope)
```

## Specifications

NITF schema, documentation, and examples are available from IPTC.
[This archive](http://www.iptc.org/std/NITF/NITF.zip) contains all of this for versions 2.5 to 3.6.

The schemas used to generate the classes in this project are available in the [schema](schema) folder.

## Generated Sources

The source files were generated using an
[unreleased version of ScalaXB](https://github.com/eed3si9n/scalaxb/archive/5d0eea5a6c4d713976c9b86cc2cb691d0f83e137.zip)
that was built from source. Hopefully, it will be released in the main repository soon.

The following command was used to generate the files for each version:
```bash
for v in 3.{3..6}; do
  scalaxb "schema/nitf-$v.xsd" \
    --outdir "$v" \
    --symbol-encoding-strategy=discard \
    --capitalize-words \
    --no-dispatch-client \
    --default-package com.gu.nitf.model \
    --protocol-package com.gu.nitf.scalaxb
done
```

## Building

To build this project from source, run:
```bash
sbt clean +compile +test
```
Note that a clean build may take up to 10 minutes.
You may also need to increase the memory available to sbt (e.g. using `-mem`).  
(The full compilation has more than 24k classes files!)

The project is set up to build against Scala 2.11 and Scala 2.12.
