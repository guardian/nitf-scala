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
  import BuildSettings._

  private val schemaVersions = Seq("3.3", "3.4", "3.5", "3.6")

  lazy val mainProjects: Seq[Project] = schemaVersions.map(nitfProject)
  lazy val testProjects: Seq[Project] = schemaVersions.map(testProject)
  lazy val leafProjects: Seq[Project] = mainProjects ++ testProjects

  override def projects: Seq[Project] = rootProject.toSeq ++ leafProjects

  override def rootProject = Some(
    Project(id = Metadata.projectName, base = file("."))
      .aggregate(leafProjects.map(Project.projectToRef): _*)
      .settings(commonSettings ++ releaseSettings ++ disabledPublishingSettings)
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

object BuildSettings {
  val commonDependencies: Seq[ModuleID] = Dependencies.xmlParsing

  val commonSettings: Seq[Setting[_]] = Metadata.settings ++ Seq(
    crossScalaVersions := Dependencies.scalaVersions,
    scalaVersion := Dependencies.scalaVersions.min,
    scalacOptions += "-target:jvm-1.8",
    publishMavenStyle := true,

    dependencyCheckFailBuildOnCVSS := 4
  )

  val disabledPublishingSettings: Seq[Setting[_]] = Seq(
    skip in publish := true
  )

  val mainSettings: Seq[Setting[_]] = commonSettings ++ Seq(
    name := "nitf-scala",
    publishTo := sonatypePublishTo.value,
    libraryDependencies ++= commonDependencies
  )

  val testSettings: Seq[Setting[_]] = commonSettings ++ disabledPublishingSettings ++ Seq(
    fork := true,
    libraryDependencies ++= commonDependencies ++ Dependencies.testing,
    dependencyCheckSkip := true
  )

  val releaseSettings: Seq[Setting[_]] = Seq(
    releaseVcsSign := true,
    releaseCrossBuild := true,
    releaseProcess := releasingProcess,
    releaseCommitMessage += "\n\n[ci skip]"
  )

  val releasePublishAction: TaskKey[_] = PgpKeys.publishSigned
  def releasingProcess: Seq[ReleaseStep] = Seq(ReleaseStep(identity)  /* no-op */
    , runClean
    , checkSnapshotDependencies
    , releaseStepTask(dependencyCheckAggregate)
    , inquireVersions
    , runTest
    , setReleaseVersion
    , commitReleaseVersion
    , tagRelease
    , releaseStepCommandAndRemaining(s"+${releasePublishAction.key.label}")
    , setNextVersion
    , commitNextVersion
    , pushChanges
  )
}
