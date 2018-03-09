import sbt._
import sbt.Keys._
import sbt.internal.BuildDef

import com.typesafe.sbt.SbtPgp.autoImport._
import net.vonbuchholtz.sbt.dependencycheck.DependencyCheckPlugin.autoImport._
import sbtrelease.ReleasePlugin.autoImport._
import sbtrelease.ReleaseStateTransformations._
import xerial.sbt.Sonatype.autoImport._

/** Creates a dynamic project for each schema version, and configures the common tests for all projects.
  *
  * The main projects are cross-compiled but the tests aren't because of a test dependency that isn't available for 2.12.
  * Releasing, publishing, and checking for insecure dependencies are disabled for test projects.
  * The root project is just an empty shell to run aggregated commands.
  *
  * The release process is customised to include a step to run ''dependencyCheck''.
  */
object Build extends BuildDef with BuildCommon {
  private val schemaVersions = Seq("3.3", "3.4", "3.5", "3.6")

  lazy val mainProjects: Seq[Project] = schemaVersions.map(nitfProject)
  lazy val testProjects: Seq[Project] = schemaVersions.map(testProject)
  lazy val realProjects: Seq[Project] = mainProjects ++ testProjects

  override def projects: Seq[Project] = rootProject.toSeq ++ realProjects
  override def rootProject = Some(
    Project(id = Metadata.projectName, base = file("."))
      .aggregate(realProjects.map(Project.projectToRef): _*)
      .settings(commonSettings ++ disabledPublishingSettings)
      .settings(
        crossScalaVersions := Dependencies.scalaVersions,
        releaseCrossBuild := true,
        releaseProcess := releasingProcess
      )
  )

  private lazy val commonSettings = Metadata.settings ++ Seq(
    crossScalaVersions := Dependencies.scalaVersions,
    scalaVersion := Dependencies.scalaVersions.min,
    scalacOptions += "-target:jvm-1.8",

    dependencyCheckFailBuildOnCVSS := 4,

    publishTo := sonatypePublishTo.value,
    releasePublishArtifactsAction := PgpKeys.publishSigned.value
  )

  private val commonDependencies = Dependencies.xmlParsing

  private lazy val mainSettings = commonSettings ++ Seq(
    name := "nitf-scala",
    libraryDependencies ++= commonDependencies
  )

  private lazy val testSettings = commonSettings ++ disabledPublishingSettings ++ Seq(
    fork := true,
    libraryDependencies ++= commonDependencies ++ Dependencies.testing,
    dependencyCheckSkip := true
  )

  private lazy val disabledPublishingSettings = { import PgpKeys._; Seq(
    publish := {},
    publishLocal := {},
    publishSigned := {},
    publishLocalSigned := {},
    publishArtifact := false,
    sonatypePublishTo := None
  )}

  private val releasingProcess = Seq[ReleaseStep](ReleaseStep(identity)  /* no-op */
    , runClean
    , checkSnapshotDependencies
    , releaseStepTask(dependencyCheckAggregate)
    , inquireVersions
    , runTest
    , setReleaseVersion
    , commitReleaseVersion
    , tagRelease
    , publishArtifacts
    , setNextVersion
    , commitNextVersion
    , pushChanges
  )

  private def nitfProject(schemaVersion: String): Project = {
    Project(id = projectId(schemaVersion), base = file(schemaVersion))
      .settings(mainSettings)
      .settings(version := s"$schemaVersion.${(ThisBuild/version).value}")
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
