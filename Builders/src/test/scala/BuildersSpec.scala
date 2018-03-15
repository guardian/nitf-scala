import java.time.LocalDate

import org.scalatest.FunSpec
import java.time._
import java.io.{ByteArrayInputStream, StringReader}
import java.nio.file.{Files, Path, Paths}

import javax.xml.XMLConstants._
import javax.xml.transform.stream.StreamSource
import javax.xml.validation.SchemaFactory
import javax.xml.parsers.SAXParserFactory

import scala.xml.factory.XMLLoader
import scala.xml._
import com.gu.nitf.model._
import com.gu.nitf.scalaxb._
import com.gu.nitf.model.builders._
import javax.xml.transform.Source
import org.xml.sax.ErrorHandler
import org.scalatest.Matchers._

import scala.xml.parsing.ConsoleErrorHandler

object BuildersSpec {
  def prettyPrint(n: scala.xml.Node): String = new PrettyPrinter(200, 2).format(n)

  def validate(xmlContents: NodeSeq, schemaPath: String): Iterable[SAXParseException] = {
    val path = Paths.get(schemaPath)
    val xsdSources = Seq(path.resolveSibling("xml.xsd"), path).map(xsdSource)
    val schema = SchemaFactory.newInstance(W3C_XML_SCHEMA_NS_URI).newSchema(xsdSources.toArray)

    val validator = schema.newValidator()
    val errorHandler = new ConsoleErrorHandler with ExceptionSuppressingSaxErrorHandler
    validator.setErrorHandler(errorHandler)
    validator.validate(new StreamSource(new StringReader(xmlContents.toString)))
    errorHandler.exceptions
  }

  private def xsdSource(xsdPath: Path): Source =
    new StreamSource(new ByteArrayInputStream(Files.readAllBytes(xsdPath)))

  private trait ExceptionSuppressingSaxErrorHandler extends ErrorHandler {
    var exceptions = Seq.empty[SAXParseException]
    abstract override def warning   (ex: SAXParseException): Unit = { suppress(ex); super.warning(ex) }
    abstract override def error     (ex: SAXParseException): Unit = { suppress(ex); super.error(ex) }
    abstract override def fatalError(ex: SAXParseException): Unit = { suppress(ex); super.fatalError(ex) }
    protected def suppress(ex: SAXParseException): Unit = { exceptions :+= ex }
  }
}

class BuildersSpec extends FunSpec {
  import BuildersSpec._

  describe("builders") {
    it("should produce valid NITF") {
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
          .build
        )
        .withBody(new BodyBuilder()
          .withHead(new BodyHeadBuilder()
            .withHeadline("News Article")
            .withByline("It took a lot of work to get there")
            .withAbstract(<p>It wasn't easy, but they <em>never</em> gave up!</p>)
            .build
          )
          .withContent(new BodyContentBuilder()
            .withParagraph(new ParagraphBuilder().withText("It was done, really!"))
            .build
          )
          .build
        )
        .build

      val nitfXml = scalaxb.toXML(nitf, None, None, BareNitfNamespace).head
      // println("generated XML:\n" + prettyPrint(nitfXml))
      val validationErrors = validate(nitfXml, "../schema/nitf-3.3.xsd")
      val formattedErrors = if (validationErrors.isEmpty) "" else validationErrors.mkString("Validation errors:\n", "\n", "\n")
      formattedErrors shouldBe empty
    }
  }
}
