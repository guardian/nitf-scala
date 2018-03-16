
import scala.PartialFunction.condOpt
import scala.xml._
import scala.xml.transform._

import com.github.andyglow.xml.diff._
import org.scalatest._
import org.scalatest.Matchers._
import org.scalatest.xml.XmlMatchers._
import Utils._

import com.gu.nitf.model.Nitf
import com.gu.nitf.scalaxb._

/** This test is run for each version of the NITF specification
  * to test that we can read and (re)write the example that is distributed with the spec.
  */
object TwoWaySpec {
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
}

class TwoWaySpec extends FunSpec {
  import TwoWaySpec._

  describe("the parser") {
    it("should parse and regenerate a sample file") {

      val example = XML.load(resource(s"nitf-example-$schemaVersion.xml"))
      val schemaLocation = example.attribute(namespaces("xsi"), "schemaLocation").get.head.text

      val parsed = scalaxb.fromXML[Nitf](example)
      val generated = scalaxb.toXML(parsed, namespace = None, elementLabel = Some("nitf"), fullScope).head
      validate(generated)

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

      // for debugging purposes:
      writeTemporaryFile("actual", actual)
      writeTemporaryFile("expected", expected)

      val diff = expected =#= actual.asInstanceOf[Elem]
      withClue(diff.toString + "\n" + diff.errorMessage) {
        actual should beXml(expected, ignoreWhitespace = true)
      }
    }
  }
}
