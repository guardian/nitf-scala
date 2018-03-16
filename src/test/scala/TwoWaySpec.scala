
import java.net.URL
import java.nio.file.{Files, Paths}

import com.gu.nitf.model.Nitf
import com.gu.nitf.scalaxb._

import com.github.andyglow.xml.diff._
import org.scalatest.Matchers._
import org.scalatest._

import scala.PartialFunction.condOpt
import scala.xml._
import scala.xml.transform._

object ResourceUtils {
  def resource(fileName: String): URL =
    Option(Thread.currentThread.getContextClassLoader)
      .getOrElse(classOf[TwoWaySpec].getClassLoader)
      .getResource(fileName)
}

/** This test is run for each version of the NITF specification
  * to test that we can read and (re)write the example that is distributed with the spec.
  */
object TwoWaySpec {
  private val schemaVersion = System.getProperty("nitf.schema.version")  // defined in project/Build.scala
  assume(Option(schemaVersion).getOrElse("") !== "")

  // create a scope similar to `defaultScope` but without the "xs" namespace and with the extra namespace(s) in the example
  private val namespaces =
    flatten(defaultScope).map(s => s.prefix -> s.uri).toMap
  private val scope =
    Seq(Some("xsi") -> namespaces("xsi"), None -> namespaces("nitf"))
  private val extraScope =
    condOpt(schemaVersion) { case "3.6" => Some("per") -> "http://person.example.net" }
  private val fullScope =
    scalaxb.toScope(scope ++ extraScope: _*)

  private def flatten(scope: NamespaceBinding): Seq[NamespaceBinding] =
    NamespaceBinding(scope.prefix, scope.uri, TopScope) +: Option(scope.parent).toSeq.flatMap(flatten)

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
  import ResourceUtils._
  import TwoWaySpec._

  describe("the parser") {
    it("should parse and regenerate a sample file") {

      val example = XML.load(resource(s"nitf-example-$schemaVersion.xml"))
      val schemaLocation = example.attribute(namespaces("xsi"), "schemaLocation").get.head.text

      val parsed = scalaxb.fromXML[Nitf](example)
      val generated = scalaxb.toXML(parsed, namespace = None, elementLabel = Some("nitf"), fullScope).head

      // update the generated XML with unimportant properties in order to be able to compare it with the input
      // this is because ScalaXB doesn't emit fixed attributes or default values
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
      val outputDir = Files.createDirectories(Paths.get("target/tmp"))
      Seq("actual" -> actual, "expected" -> expected).foreach { case (fileName, xml) =>
        Files.write(outputDir.resolve(s"$fileName.xml"), prettyPrint(xml).getBytes("UTF8"))
      }

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
