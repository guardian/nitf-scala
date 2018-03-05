
import com.gu.nitf.model.Nitf
import com.gu.nitf.scalaxb._
import org.scalatest.Matchers._
import org.scalatest._

import scala.xml._
import scala.xml.transform._

object TwoWaySpec {
  private val nitfSchemaUri = "http://iptc.org/std/NITF/2006-10-18/"
  private val fullScope = scalaxb.toScope(Some("xsi") -> "http://www.w3.org/2001/XMLSchema-instance", None -> nitfSchemaUri)

  private def resource(xmlFile: String) =
    Thread.currentThread.getContextClassLoader.getResourceAsStream(xmlFile)

  private def prettyPrint(n: Node) = new PrettyPrinter(200, 2).format(Utility.sort(Utility.trim(n)))

  private implicit class RichMetaData(val metaData: MetaData) extends AnyVal {
    def without(unwantedAttributes: Seq[String]): MetaData = unwantedAttributes.foldLeft(metaData)(_ remove _)
  }

  private implicit class RichElem(val e: Elem) extends AnyVal {
    def withAttributes(attrs: Attribute*): Elem = attrs.foldLeft(e)(_ % _)
    def withoutAttributes(unwanted: String*): Elem = e.copy(attributes = e.attributes.without(unwanted))
  }
}

class TwoWaySpec extends FunSpec {
  import TwoWaySpec._

  describe("the parser") {
    it("should parse and regenerate a sample file") {

      // update the generated XML with unimportant properties in order to be able to compare it with the input easily
      val removeFixedAndAddDefaultAttrs = new RuleTransformer(new RewriteRule {
        override def transform(n: Node): Seq[Node] = n match {
          case e: Elem if e.label == "nitf" =>
            e.withAttributes(Attribute("xsi", "schemaLocation", "http://iptc.org/std/NITF/2006-10-18/ ../specification/schema/nitf-3-4.xsd", Null))
              .withoutAttributes("change.date", "change.time", "version")
          case e: Elem if e.label == "tobject" =>
            e % Attribute(None, "tobject.type", Text("news"), Null)
          case e: Elem if e.label == "series" =>
            e % Attribute(None, "series.totalpart", Text("0"), Null)
          case e => e
        }
      })

      val example = XML.load(resource("nitf-fishing-schema.xml"))

      val parsed = scalaxb.fromXML[Nitf](example)
      val generated = scalaxb.toXML(parsed, namespace = None, elementLabel = Some("nitf"), scope = fullScope).head

      val actual = removeFixedAndAddDefaultAttrs(generated)
      val expected = example

      // for debugging:
      import java.nio.file._
      val outputDir = Files.createDirectories(Paths.get("target/tmp"))
      Seq("actual" -> actual, "expected" -> expected).foreach { case (fileName, xml) =>
        Files.write(outputDir.resolve(s"$fileName.xml"), prettyPrint(xml).getBytes("UTF8"))
      }
      import com.github.andyglow.xml.diff._
      val diff = expected =#= actual.asInstanceOf[Elem]

      import org.scalatest.xml.XmlMatchers._
      actual should beXml(expected, ignoreWhitespace = true)
    }
  }
}
