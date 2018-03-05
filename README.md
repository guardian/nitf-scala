# nitf-scala

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

The schemas used to generate the classes in this project are available in the [schema](schema/) folder.

## Generated Sources

The source files were generated using a
[custom version of ScalaXB](https://github.com/hosamaly/scalaxb/archive/bd92a411fa863815019a216d23f7b8d9d342b27b.zip)
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

To build this project, run:
```bash
sbt clean +compile test
```
Note that a clean build may take up to 10 minutes.  
(The full compilation has more than 23k classes!)

The compiled sources are cross-built against Scala 2.11 and 2.12.

The tests are built against Scala 2.11 only.
Hopefully, this will change as soon as
[scala-xml-diff](https://github.com/andyglow/scala-xml-diff)
is released for 2.12.
