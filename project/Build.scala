import sbt._
import sbt.Keys._
import sbt.internal.BuildDef

import net.vonbuchholtz.sbt.dependencycheck.DependencyCheckPlugin.autoImport.dependencyCheckFailBuildOnCVSS

object Build extends BuildDef with BuildCommon {
  override def projects: Seq[Project] = mainProjects ++ testProjects
  lazy val mainProjects: Seq[Project] = schemaVersions.map(nitfProject)
  lazy val testProjects: Seq[Project] = schemaVersions.map(testProject)

  private val releaseVersion = 0
  private val schemaVersions = Seq("3.3", "3.4", "3.5", "3.6")

  private val commonDependencies = Dependencies.xmlParsing

  private lazy val mainSettings = Seq(
    crossScalaVersions := Dependencies.scalaVersions,
    libraryDependencies ++= commonDependencies,
    dependencyCheckFailBuildOnCVSS := 4
  )

  private lazy val testSettings = Seq(
    fork := true,
    crossVersion := Disabled(),  // scala-xml-diff is released for 2.11 only
    libraryDependencies ++= commonDependencies ++ Dependencies.testing
  )

  private def nitfProject(schemaVersion: String): Project = {
    Project(id = projectId(schemaVersion), base = file(schemaVersion))
      .settings(mainSettings)
      .settings(version := s"$schemaVersion.$releaseVersion")
  }

  private def testProject(schemaVersion: String): Project = {
    val mainProjectId = projectId(schemaVersion)
    Project(id = mainProjectId + "Test", base = file("Tests"))
      .settings(testSettings)
      .settings(
        target := baseDirectory.value / s"target/$schemaVersion",
        javaOptions += s"-Dnitf.schema.version=$schemaVersion"
      )
      .dependsOn(mainProjects.find(_.id == mainProjectId).get)
  }

  private def projectId(schemaVersion: String) = "nitf" + schemaVersion.replaceAll("""\.""", "")
}
