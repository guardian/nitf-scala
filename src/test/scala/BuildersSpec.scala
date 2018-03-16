import java.io.{ByteArrayInputStream, StringReader}
import java.nio.file.{Files, Path, Paths}
import java.time.LocalDate

import javax.xml.XMLConstants._
import javax.xml.transform.Source
import javax.xml.transform.stream.StreamSource
import javax.xml.validation.SchemaFactory

import scala.xml._
import scala.xml.parsing.ConsoleErrorHandler

import org.scalatest.FunSpec
import org.scalatest.Matchers._
import org.xml.sax.ErrorHandler

import com.gu.nitf.model._
import com.gu.nitf.scalaxb._
import com.gu.nitf.model.builders._

object BuildersSpec {
  def prettyPrint(n: scala.xml.Node): String = new PrettyPrinter(200, 2).format(n)

  def validate(xmlContents: NodeSeq, schemaPath: String): Iterable[SAXParseException] = {
    val path = Paths.get(schemaPath)
    val xsdSources = Seq(path.resolveSibling("xml.xsd"), path).map(xsdSource)
    val schema = SchemaFactory.newInstance(W3C_XML_SCHEMA_NS_URI).newSchema(xsdSources.toArray)

    val validator = schema.newValidator()
    val errorHandler = new ConsoleErrorHandler with ExceptionCollectingSaxErrorHandler
    validator.setErrorHandler(errorHandler)
    validator.validate(new StreamSource(new StringReader(xmlContents.toString)))
    errorHandler.exceptions
  }

  private def xsdSource(xsdPath: Path): Source =
    new StreamSource(new ByteArrayInputStream(Files.readAllBytes(xsdPath)))

  private trait ExceptionCollectingSaxErrorHandler extends ErrorHandler {
    var exceptions = Seq.empty[SAXParseException]
    abstract override def warning   (ex: SAXParseException): Unit = { collect(ex); super.warning(ex) }
    abstract override def error     (ex: SAXParseException): Unit = { collect(ex); super.error(ex) }
    abstract override def fatalError(ex: SAXParseException): Unit = { collect(ex); super.fatalError(ex) }
    protected def collect(ex: SAXParseException): Unit = { exceptions :+= ex }
  }

  private val schemaVersion = System.getProperty("nitf.schema.version")  // defined in project/Build.scala
  assume(Option(schemaVersion).getOrElse("") !== "")
}

class BuildersSpec extends FunSpec {
  import BuildersSpec._

  describe("builders") {
    it(s"should produce valid NITF (v$schemaVersion)") {
      val docData = new DocDataBuilder()
        .withUrgency(NewsUrgency.Two)
        .withManagementStatus(ManagementStatus.Usable)
        .withCopyright(new DocCopyrightBuilder().withHolder("example.com"))
        .withDocId(new DocIdBuilder().withId("1234"))
        .withIssueDate(LocalDate.now)
        .withReleaseDate(LocalDate.now)

      val publicationData = new PublicationDataBuilder().withType(Print)
        .withPublicationDate(LocalDate.now)

      val nitf = new NitfBuilder()
        .withHead(new HeadBuilder()
          .withTitle("News Article")
          .withDocData(docData)
          .withPublicationData(publicationData)
        )
        .withBody(new BodyBuilder()
          .withHead(new BodyHeadBuilder()
            .withHeadline("News Article")
            .withByline("It took a lot of work to get there")
            .withAbstract(<p>It wasn't easy, but they <em>never</em> gave up!</p>)
          )
          .withContent(new BodyContentBuilder()
            .withParagraph(new ParagraphBuilder().withText("It was done, really!"))
            .withMedia(new MediaBuilder()
              .withType(MediaType.Image)
              .withReference(new MediaReferenceBuilder()
                .withSource(java.net.URI.create("https://upload.wikimedia.org/wikipedia/commons/7/70/Example.png"))
            ))
          )
        )
        .build

      val nitfXml = scalaxb.toXML(nitf, None, None, BareNitfNamespace).head
      val validationErrors = validate(nitfXml, s"src/test/resources/nitf-$schemaVersion.xsd")
      val formattedErrors = if (validationErrors.isEmpty) "" else validationErrors.mkString("\nValidation errors:\n", "\n", "\n")
      withClue(prettyPrint(nitfXml)) {
        formattedErrors shouldBe empty
      }
    }
  }
}
