
import com.gu.nitf.model.Nitf
import com.gu.nitf.scalaxb._
import org.scalatest.Matchers._
import org.scalatest._

import scala.xml._
import scala.xml.transform._

object TwoWaySpec {
  private val schemaVersion = System.getProperty("nitf.schema.version")  // defined in project/Build.scala
  assume(Option(schemaVersion).getOrElse("") !== "")

  private val namespaces = flatten(defaultScope).map(s => s.prefix -> s.uri).toMap
  private val scope = Seq(Some("xsi") -> namespaces("xsi"), None -> namespaces("nitf"))
  private val extraScope = if (schemaVersion != "3.6") Seq.empty else
    Seq(Some("per") -> "http://person.example.net")
  private val fullScope = scalaxb.toScope(scope ++ extraScope: _*)

  private def flatten(scope: NamespaceBinding): Seq[NamespaceBinding] =
    NamespaceBinding(scope.prefix, scope.uri, TopScope) +: Option(scope.parent).toSeq.flatMap(flatten)

  private def resource(xmlFile: String) =
    Thread.currentThread.getContextClassLoader.getResourceAsStream(xmlFile)

  private def prettyPrint(n: Node) = new PrettyPrinter(200, 2).format(Utility.sort(Utility.trim(n)))

  private implicit class RichMetaData(val metaData: MetaData) extends AnyVal {
    def without(unwantedAttributes: Seq[String]): MetaData = unwantedAttributes.foldLeft(metaData)(_ remove _)
  }

  private implicit class RichElem(val e: Elem) extends AnyVal {
    def withAttribute(prefix: String, key: String, value: String): Elem = e % Attribute(prefix, key, value, Null)
    def withAttributes(attrs: Attribute*): Elem = attrs.foldLeft(e)(_ % _)
    def withoutAttributes(unwanted: String*): Elem = e.copy(attributes = e.attributes.without(unwanted))
  }
}

class TwoWaySpec extends FunSpec {
  import TwoWaySpec._

  describe("the parser") {
    it("should parse and regenerate a sample file") {

      val example = XML.load(resource(s"nitf-example-$schemaVersion.xml"))
      val schemaLocation = example.attribute(namespaces("xsi"), "schemaLocation").get.head.text

      val parsed = scalaxb.fromXML[Nitf](example)
      val generated = scalaxb.toXML(parsed, namespace = None, elementLabel = Some("nitf"), scope = fullScope).head

      // update the generated XML with unimportant properties in order to be able to compare it with the input easily
      val removeFixedAndAddDefaultAttrs = new RuleTransformer(new RewriteRule {
        override def transform(n: Node): Seq[Node] = n match {
          case e: Elem if e.label == "nitf" =>
            e.withAttribute("xsi", "schemaLocation", schemaLocation)
              .withoutAttributes("change.date", "change.time", "version")
          case e: Elem if e.label == "tobject" =>
            e % Attribute(None, "tobject.type", Text("news"), Null)
          case e: Elem if e.label == "series" =>
            e % Attribute(None, "series.totalpart", Text("0"), Null)
          case e => e
        }
      })

      val actual = removeFixedAndAddDefaultAttrs(generated)
      val expected = example.withoutAttributes("version")  // some examples have it while others don't

      // for debugging:
      import java.nio.file._
      val outputDir = Files.createDirectories(Paths.get("target/tmp"))
      Seq("actual" -> actual, "expected" -> expected).foreach { case (fileName, xml) =>
        Files.write(outputDir.resolve(s"$fileName.xml"), prettyPrint(xml).getBytes("UTF8"))
      }
      import com.github.andyglow.xml.diff._
      val diff = expected =#= actual.asInstanceOf[Elem]
      if (!diff.successful) {
        info(diff.toString)
        info(diff.errorMessage)
      }

      import org.scalatest.xml.XmlMatchers._
      actual should beXml(expected, ignoreWhitespace = true)
    }
  }
}
