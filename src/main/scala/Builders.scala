package com.gu.nitf.model.builders

import java.net.URI
import java.time.LocalDate

import scala.collection.breakOut
import scala.collection.generic.CanBuildFrom
import scala.xml.{NamespaceBinding, NodeSeq}

import scalaxb._

import com.gu.nitf.model._
import com.gu.nitf.scalaxb._
import com.gu.scalaxb._


object `package` {
  // aliases for builder classes whose names are different from the NITF spec
  type DocdataBuilder = DocDataBuilder
  type HedlineBuilder = HeadlineBuilder
  type PBuilder = ParagraphBuilder
  type PubdataBuilder = PublicationDataBuilder

  // aliases for enumeration types whose names are unintuitive
  type NoteType = TypeType2
  type PublicationType = TypeType
  type TaglineType = TypeType3

  val BareNitfNamespace: NamespaceBinding = toScope(None -> defaultScope.uri)

  private[builders] val DateTimeAttributeKey = "norm"
}

object Builder {
  import scala.language.implicitConversions
  implicit def buildBuilder[T](builder: Builder[T]): T = builder.build
}

trait Builder[T] {
  def build: T

  override def toString: String = build.toString
}

trait EnrichedTextBuilder {
  def withAnchor(x: A): this.type = withEnrichedText(x)
  def withChron(x: Chron): this.type = withEnrichedText(x)
  def withEmphasis(x: Em): this.type = withEnrichedText(x)
  def withEvent(x: Event): this.type = withEnrichedText(x)
  def withMoney(x: Money): this.type = withEnrichedText(x)
  def withNumeric(x: Num): this.type = withEnrichedText(x)
  def withQuotation(x: Q): this.type = withEnrichedText(x)
  def withLineBreak(x: Br): this.type = withEnrichedText(x)
  def withLanguage(x: Lang): this.type = withEnrichedText(x)
  def withPerson(x: Person): this.type = withEnrichedText(x)
  def withOrganization(x: Org): this.type = withEnrichedText(x)
  def withLocation(x: Location): this.type = withEnrichedText(x)
  def withCopyright(x: Copyrite): this.type = withEnrichedText(x)
  def withClassifier(x: Classifier): this.type = withEnrichedText(x)
  def withFunction(x: FunctionType): this.type = withEnrichedText(x)
  def withPostalAddress(x: Postaddr): this.type = withEnrichedText(x)
  def withObjectTitle(x: ObjectTitle): this.type = withEnrichedText(x)
  def withPronunciation(x: Pronounce): this.type = withEnrichedText(x)
  def withVirtualLocation(x: Virtloc): this.type = withEnrichedText(x)
  def withXml(x: NodeSeq): this.type = withContent(dataRecord(x))
  def withText(x: String): this.type = withContent(dataRecord(x))

  protected def withEnrichedText[T <: EnrichedTextOption : CanWriteXML](x: T): this.type = withContent(dataRecord(x))
  protected def withContent(x: DataRecord[_]): this.type
}

class NitfBuilder(var build: Nitf = Nitf(body = Body())) extends Builder[Nitf] {
  def withHead(x: Head): this.type = { build = build.copy(head = Option(x)); this }
  def withBody(x: Body): this.type = { build = build.copy(body = x); this }
  def withUno(x: String): this.type = {
    build = build.copy(attributes = build.attributes ++ attrs("uno" -> x.toString))
    this
  }
}

class HeadBuilder(var build: Head = Head()) extends Builder[Head] {
  def withTitle(x: String): this.type = { build = build.copy(title = Option(x).map(t => Title(Seq(dataRecord(t))))); this }
  def withDocData(x: Docdata): this.type = { build = build.copy(docdata = Option(x)); this }
  def withPublicationData(x: Pubdata): this.type = { build = build.copy(pubdata = build.pubdata :+ x); this }
}

class DocDataBuilder(var build: Docdata = Docdata()) extends Builder[Docdata] {
  def withDocId(x: DocId): this.type = withDocDataOption(x)
  def withCopyright(x: DocCopyright): this.type = withDocDataOption(x)
  def withIssueDate(x: LocalDate): this.type = withDocDataOption(DateIssue(attrs(DateTimeAttributeKey -> x.toString)))
  def withReleaseDate(x: LocalDate): this.type = withDocDataOption(DateRelease(attrs(DateTimeAttributeKey -> x.toString)))
  def withUrgency(x: NewsUrgency.Value): this.type = withDocDataOption(Urgency(attrs("ed-urg" -> x.toString)))
  def withManagementStatus(x: ManagementStatus.Value): this.type = {
    build = build.copy(attributes = build.attributes ++ attrs("management-status" -> x.toString))
    this
  }
  private def withDocDataOption[T <: DocdataOption : CanWriteXML](x: T): this.type = {
    build = build.copy(docdataoption = build.docdataoption :+ dataRecord(x))
    this
  }
}

class DocCopyrightBuilder(var build: DocCopyright = DocCopyright()) extends Builder[DocCopyright] {
  def withHolder(x: String): this.type = withAttribute("holder", x)
  def withYear(x: Int): this.type = withAttribute("year", x.toString)
  private def withAttribute(key: String, value: String): this.type = {
    build = build.copy(attributes = build.attributes ++ attrs(key -> value))
    this
  }
}

class DocIdBuilder(var build: DocId = DocId()) extends Builder[DocId] {
  def withId(x: String): this.type = withAttribute("id-string", x)
  def withSource(x: String): this.type = withAttribute("regsrc", x)
  private def withAttribute(key: String, value: String): this.type = {
    build = build.copy(attributes = build.attributes ++ attrs(key -> value))
    this
  }
}

class PublicationDataBuilder(var build: Pubdata = Pubdata()) extends Builder[Pubdata] {
  def withType(x: PublicationType): this.type = {
    build = build.copy(attributes = build.attributes + attrWithValue("type", x))
    this
  }
  def withPublicationDate(x: LocalDate): this.type = {
    build = build.copy(attributes = build.attributes ++ attrs("date.publication" -> x.toString))
    this
  }
}

class BodyBuilder(var build: Body = Body()) extends Builder[Body] {
  def withHead(x: BodyHead): this.type = { build = build.copy(bodyHead = Option(x)); this }
  def withContent(x: BodyContent): this.type = { build = build.copy(bodyContent = build.bodyContent :+ x); this }
}

class BodyHeadBuilder(var build: BodyHead = BodyHead()) extends Builder[BodyHead] {
  def withByline(x: String): this.type = withByline(Byline(Seq(dataRecord(x))))
  def withHeadline(x: String): this.type = withHeadline(new HeadlineBuilder().withPrimaryHeadline(x))
  def withAbstract(x: NodeSeq): this.type = withAbstract(Abstract(Seq(dataRecord(x))))
  def withAbstract(x: String, markAsSummary: Boolean = true): this.type = {
    var paragraphBuilder = new ParagraphBuilder().withText(x)
    if (markAsSummary) paragraphBuilder = paragraphBuilder.asSummary
    withAbstract(Abstract(Seq(dataRecord(paragraphBuilder.build))))
  }

  def withRights(x: Rights): this.type = { build = build.copy(rights = Option(x)); this }
  def withSeries(x: Series): this.type = { build = build.copy(series = Option(x)); this }
  def withDistributor(x: Distributor): this.type = { build = build.copy(distributor = Option(x)); this }

  def withNote(x: Note): this.type = { build = build.copy(note = build.note :+ x); this }
  def withByline(x: Byline): this.type = { build = build.copy(byline = build.byline :+ x); this }
  def withHeadline(x: Hedline): this.type = { build = build.copy(hedline = build.hedline :+ x); this }
  def withDateline(x: Dateline): this.type = { build = build.copy(dateline = build.dateline :+ x); this }
  def withAbstract(x: Abstract): this.type = {
    build = build.copy(abstractValue = Chooser.choose(build.abstractValue, x))
    this
  }
}

class HeadlineBuilder(var build: Hedline = Hedline(Hl1())) extends Builder[Hedline] {
  def withPrimaryHeadline(x: Hl1): this.type = { build = build.copy(hl1 = x); this }
  def withPrimaryHeadline(x: String): this.type = { withPrimaryHeadline(new PrimaryHeadlineBuilder(build.hl1).withText(x)) }
  def withSubordinateHeadline(x: Hl2): this.type = { build = build.copy(hl2 = build.hl2 :+ x); this }
  def withSubordinateHeadline(x: String): this.type = { withSubordinateHeadline(Hl2(Seq(dataRecord(x)))); this }
}

class PrimaryHeadlineBuilder(var build: Hl1 = Hl1()) extends Builder[Hl1] with EnrichedTextBuilder {
  protected def withContent(x: DataRecord[_]): this.type = { build = build.copy(mixed = build.mixed :+ x); this }
}

trait BlockContentBuilder {
  def withNote(x: Note): this.type = withBlockContent(x)
  def withFootnote(x: Fn): this.type = withBlockContent(x)
  def withMedia(x: Media): this.type = withBlockContent(x)
  def withParagraph(x: P): this.type = withBlockContent(x)
  def withTable(x: Table): this.type = withBlockContent(x)
  def withBlockQuote(x: Bq): this.type = withBlockContent(x)
  def withOrderedList(x: Ol): this.type = withBlockContent(x)
  def withPreformatted(x: Pre): this.type = withBlockContent(x)
  def withUnorderedList(x: Ul): this.type = withBlockContent(x)
  def withDefinitionList(x: Dl): this.type = withBlockContent(x)
  def withHorizontalRule(x: Hr): this.type = withBlockContent(x)
  def withNitfTable(x: NitfTable): this.type = withBlockContent(x)
  def withSubordinateHeadline(x: Hl2): this.type = withBlockContent(x)
  def withXml(x: NodeSeq): this.type = withContent(dataRecord(x))

  protected def withBlockContent[T <: BlockContentOption : CanWriteXML](x: T): this.type = withContent(dataRecord(x))
  protected def withContent(x: DataRecord[_]): this.type
}

class BodyContentBuilder(var build: BodyContent = BodyContent()) extends Builder[BodyContent] with BlockContentBuilder {
  def withBlock(x: Block): this.type = withContent(dataRecord(x))
  protected override def withContent(x: DataRecord[_]): this.type = {
    build = build.copy(bodycontentoption = build.bodycontentoption :+ x)
    this
  }
}

class BlockBuilder(var build: Block = Block()) extends Builder[Block] with BlockContentBuilder {
  def withDataSource(x: Datasource): this.type = {
    build = build.copy(blocksequence2 = Option(x).map(d => BlockSequence2(Option(d))))
    this
  }
  protected override def withContent(x: DataRecord[_]): this.type = {
    build = build.copy(blockoption = build.blockoption :+ x)
    this
  }
}

class MediaBuilder(var build: Media = Media()) extends Builder[Media] {
  def withMetadata(key: String, value: String): this.type = withMetadata(new MediaMetadataBuilder(key, value))
  def withMetadata(x: MediaMetadata): this.type = { build = build.copy(mediaMetadata = build.mediaMetadata :+ x); this }
  def withCaption(x: MediaCaption): this.type = { build = build.copy(mediaCaption = build.mediaCaption :+ x); this }
  def withProducer(x: MediaProducer): this.type = {
    build = build.copy(mediaProducer = Chooser.choose(build.mediaProducer, x))
    this
  }
  def withReference(x: MediaReference, y: Option[MediaObject] = None): this.type = {
    build = build.copy(mediasequence1 = build.mediasequence1 :+ MediaSequence1(x, y))
    this
  }
  def withType(x: MediaType.Value): this.type = {
    build = build.copy(attributes = build.attributes ++ attrs("media-type" -> x.toString))
    this
  }
}

class MediaMetadataBuilder(var build: MediaMetadata = MediaMetadata()) extends Builder[MediaMetadata] {
  def this(name: String, value: String) = this(MediaMetadata(attrs("name" -> name, "value" -> value)))
  def withName(x: String): this.type = withAttribute("name", x)
  def withValue(x: String): this.type = withAttribute("value", x)
  private def withAttribute(key: String, value: String): this.type = {
    build = build.copy(attributes = build.attributes ++ attrs(key -> value))
    this
  }
}

class MediaReferenceBuilder(var build: MediaReference = MediaReference()) extends Builder[MediaReference] {
  def asNoFlow: this.type = withAttribute("noflow", "noflow")
  def withSource(x: URI): this.type = withSource(x.toString)
  def withSource(x: String): this.type = withAttribute("source", x)
  def withCoding(x: String): this.type = withAttribute("coding", x)
  def withOutCue(x: String): this.type = withAttribute("outcue", x)
  def withImageMap(x: String): this.type = withAttribute("imagemap", x)
  def withWidth(x: Int): this.type = withAttribute("width", x.toString)
  def withHeight(x: Int): this.type = withAttribute("height", x.toString)
  def withCopyright(x: String): this.type = withAttribute("copyright", x)
  def withMimeType(x: String): this.type = withAttribute("mime-type", x)
  def withAlternateName(x: String): this.type = withAttribute("name", x)
  def withAlternateText(x: String): this.type = withAttribute("alternate-text", x)
  def withSourceCredit(x: String): this.type = withAttribute("source-credit", x)
  def withTimeUnitOfMeasure(x: String): this.type = withAttribute("time-unit-of-measure", x)
  def withTimeLength(x: Int): this.type = withAttribute("time", x.toString)

  private def withAttribute(key: String, value: String): this.type = {
    build = build.copy(attributes = build.attributes ++ attrs(key -> value))
    this
  }
}

class ParagraphBuilder(var build: P = P()) extends Builder[P] with EnrichedTextBuilder {
  def asLead: this.type = { withAttribute("lede", "true"); this }
  def asSummary: this.type = { withAttribute("summary", "true"); this }
  def asOptional: this.type = { withAttribute("optional-text", "true"); this }

  protected override def withContent(x: DataRecord[_]): this.type = {
    build = build.copy(mixed = build.mixed :+ x)
    this
  }
  private def withAttribute(key: String, value: String): Unit = {
    build = build.copy(attributes = build.attributes ++ attrs(key -> value))
  }
}


/** Encapsulates a strategy for choosing between (or combining) previous and new values.
  *
  * Used for cases when the API is different between two versions of NITF, in which case we rely on the compiler
  * to resolve the correct chooser via implicit resolution.
  */
private[builders] trait Chooser[T, U] { def apply(previousValues: U, newValue: T): U }
private[builders] object Chooser {
  def choose[T, U](previousValues: U, newValue: T)(implicit chooser: Chooser[T, U]): U =
    chooser(previousValues, newValue)

  // when we only have room for one value, we choose the newer
  implicit def optionChooser[T]: Chooser[T, Option[T]] = new Chooser[T, Option[T]] {
    override def apply(previousValues: Option[T], newValue: T): Option[T] = Option(newValue)
  }
  // when we have room for multiple values, we concatenate them
  implicit def collectionChooser[T, U <: Traversable[T]](implicit bf: CanBuildFrom[U, T, U]): Chooser[T, U] = new Chooser[T, U] {
    override def apply(previousValues: U, newValue: T): U = previousValues.++(List(newValue))(breakOut)
  }
}
