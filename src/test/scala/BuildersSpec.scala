import java.time.LocalDate

import org.scalatest.FunSpec
import XmlUtils._

import com.gu.nitf.model._
import com.gu.nitf.model.builders._
import com.gu.nitf.scalaxb._
import com.gu.scalaxb._


class BuildersSpec extends FunSpec {
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
            .withAbstract(new AbstractBuilder()
              .withParagraph(new ParagraphBuilder()
                .withText("It wasn't easy, but they ")
                .withEmphasis(Em(Seq(dataRecord("never"))))
                .withText(" gave up!")
              )
            )
          )
          .withContent(new BodyContentBuilder()
            .withParagraph(new ParagraphBuilder().withText("It was done, really!"))
            .withMedia(new MediaBuilder(MediaType.Image)
              .withReference(new MediaReferenceBuilder()
                .withSource(java.net.URI.create("https://upload.wikimedia.org/wikipedia/commons/7/70/Example.png"))
            ))
          )
        )
        .build

      val nitfXml = scalaxb.toXML(nitf, None, None, BareNitfNamespace).head
      validate(nitfXml)
    }
  }
}
