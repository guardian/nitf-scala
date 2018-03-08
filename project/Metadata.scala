import sbt._
import sbt.Keys._

object Metadata {
  val gitHubUser = "guardian"
  val projectName = "nitf-scala"

  lazy val settings = Seq(
    organization := "com.gu",
    organizationName := "Guardian News & Media Ltd",
    organizationHomepage := Some(url("https://www.theguardian.com/")),

    startYear := Some(2018),
    licenses += "Apache-2.0" -> url("https://choosealicense.com/licenses/apache-2.0/"),

    scmInfo := Some(ScmInfo(
      url(s"https://github.com/$gitHubUser/$projectName"),
      s"scm:git@github.com:$gitHubUser/$projectName.git"
    )),

    homepage := scmInfo.value.map(_.browseUrl),

    developers := List(
      Developer(id = "hosamaly", name = "Hosam Aly", email = null, url = url("https://github.com/hosamaly"))
    )
  )
}
