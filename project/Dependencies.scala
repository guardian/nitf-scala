import sbt._

object Dependencies {
  val scala212Version = "2.12.4"
  val scala211Version = "2.11.12"
  val scalaVersions = Seq(scala211Version, scala212Version)

  val scalaXmlVersion = "1.1.0"
  val scalaXml = "org.scala-lang.modules" %% "scala-xml" % scalaXmlVersion
  val scalaParserCombinators = "org.scala-lang.modules" %% "scala-parser-combinators" % scalaXmlVersion

  val xmlParsing = Seq(scalaXml, scalaParserCombinators)


  val scalaTestVersion = "3.0.5"
  val scalaTest = "org.scalatest" %% "scalatest" % scalaTestVersion
  val scalactic = "org.scalactic" %% "scalactic" % scalaTestVersion

  val xmlDiff = "com.github.andyglow" %% "scala-xml-diff" % "2.0.3"

  val testing = Seq(scalaTest, xmlDiff).map(_ % Test)
}
