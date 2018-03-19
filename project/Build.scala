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

  override def projects: Seq[Project] = rootProject.toSeq ++ mainProjects

  override def rootProject = Some(
    Project(id = "root", base = file("."))
      .aggregate(mainProjects.map(Project.projectToRef): _*)
      .settings(commonSettings ++ disabledPublishingSettings)
      .settings(
        Compile / sources := Seq.empty,
        Test    / sources := Seq.empty,
        releaseVcsSign := true,
        releaseCrossBuild := true,
        releaseCommitMessage += "\n\n[ci skip]",
        releaseProcess := releasingProcess
      )
  )

  private def nitfProject(schemaVersion: String): Project = {
    Project(id = projectId(schemaVersion), base = file("."))
      .settings(mainSettings)
      .settings(
        version := s"$schemaVersion.${(ThisBuild/version).value}",
        target := baseDirectory.value / s"target/$schemaVersion",
        Compile / unmanagedSourceDirectories += baseDirectory.value / s"src/main/$schemaVersion",
        Test / javaOptions += s"-Dnitf.schema.version=$schemaVersion"
      )
  }

  private def projectId(schemaVersion: String) = "nitf" + schemaVersion.replaceAll("""\.""", "")
}

object BuildSettings {
  val commonSettings: Seq[Setting[_]] = Metadata.settings ++ Seq(
    name := Metadata.projectName,
    crossScalaVersions := Dependencies.scalaVersions,
    scalaVersion := Dependencies.scalaVersions.min,
    scalacOptions += "-target:jvm-1.8",

    dependencyCheckFailBuildOnCVSS := 4
  )

  val disabledPublishingSettings: Seq[Setting[_]] = Seq(
    skip in publish := true
  )

  val mainSettings: Seq[Setting[_]] = commonSettings ++ Seq(
    publishTo := sonatypePublishTo.value,
    libraryDependencies ++= Dependencies.xmlParsing ++ Dependencies.testing,

    Test / fork := true,
    Test / testOptions += Tests.Argument(TestFrameworks.ScalaTest, "-oD")  // show individual test durations
  )

  val releasePublishAction: TaskKey[_] = PgpKeys.publishSigned
  val releasingProcess: Seq[ReleaseStep] = Seq(ReleaseStep(identity)  /* no-op */
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
