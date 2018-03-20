# nitf-scala
[![License: Apache-2.0](https://img.shields.io/github/license/guardian/nitf-scala.svg)](https://github.com/guardian/nitf-scala/blob/master/LICENSE)
[![Build Status](https://travis-ci.org/guardian/nitf-scala.svg?branch=master)](https://travis-ci.org/guardian/nitf-scala)
[![Codacy Quality Rating](https://api.codacy.com/project/badge/Grade/a7f65308a2dd4f38ada2c53234194076)](https://www.codacy.com/app/hosamaly/nitf-scala?utm_source=github.com&amp;utm_medium=referral&amp;utm_content=guardian/nitf-scala&amp;utm_campaign=Badge_Grade)
[![Latest release for Scala 2.11](https://img.shields.io/maven-metadata/v/http/central.maven.org/maven2/com/gu/nitf-scala_2.11/maven-metadata.xml.svg?label=scala%202.11)](https://mvnrepository.com/artifact/com.gu/nitf-scala)
[![Latest release for Scala 2.12](https://img.shields.io/maven-metadata/v/http/central.maven.org/maven2/com/gu/nitf-scala_2.12/maven-metadata.xml.svg?label=scala%202.12)](https://mvnrepository.com/artifact/com.gu/nitf-scala)

Scala library to parse and generate [News Industry Text Format](https://iptc.org/standards/nitf/) files,
based on [ScalaXB](http://scalaxb.org).

This library supports versions 3.3 to 3.6.  
(Previous versions don't have an XSD.)

## Usage

To use this library, add the following dependencies to your project:
```scala
val nitfScalaVersion = "3.6.2"  // one of 3.3, 3.4, 3.5, and 3.6 followed by the release version
val scalaXmlVersion = "1.1.0"
libraryDependencies ++= Seq(
  "org.scala-lang.modules" %% "scala-xml" % scalaXmlVersion,
  "org.scala-lang.modules" %% "scala-parser-combinators" % scalaXmlVersion,
  "com.gu" %% "nitf-scala" % nitfScalaVersion
)
```

This is an example of how to parse XML into NITF:
```scala
import com.gu.nitf.model._
import com.gu.nitf.scalaxb._

val doc = scalaxb.fromXML[Nitf](
  <nitf>
    <head><title>Hello</title></head>
    <body><body.content>World</body.content></body>
  </nitf>
)
```

This is an example of how to create an NITF tree:
```scala
import com.gu.nitf.model.builders._

val doc = new NitfBuilder()
  .withHead(new HeadBuilder().withTitle("News Article"))
  .withBody(new BodyBuilder()
    .withContent(new BodyContentBuilder()
      .withParagraph(new ParagraphBuilder().withText("That's it, really!"))
  ))
  .build

val xml = scalaxb.toXML(doc, None, None, BareNitfNamespace)
```

## Specifications

NITF schema, documentation, and examples are available from IPTC.
[This archive](http://www.iptc.org/std/NITF/NITF.zip) contains all of this for versions 2.5 to 3.6.

The schemas used to generate the classes in this project are available in the [schema](schema) folder.

## Generated Sources

The source files were generated using an
[unreleased version of ScalaXB](https://github.com/hosamaly/scalaxb/archive/451e9c59a3ed347c75e0d1d3924ee1be0e1939c6.zip)
that was built from source. Hopefully, it will be released in the main repository soon.

The following command was used to generate the files:
```bash
for v in 3.{3..6}; do
  scalaxb "src/test/resources/nitf-$v.xsd" \
    --outdir "src/main/$v" \
    --no-dispatch-client \
    --named-attributes \
    --capitalize-words \
    --symbol-encoding-strategy=discard \
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
(The full compilation has more than 24k class files!)

The project is set up to build against Scala 2.11 and Scala 2.12.
