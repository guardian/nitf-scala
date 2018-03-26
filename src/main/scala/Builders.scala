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
  type TitleType = Type

  val BareNitfNamespace: NamespaceBinding = toScope(None -> defaultScope.uri)

  @inline private[builders] def optionalString(x: Any): Option[String] = Option(x).map(_.toString)
}

object Builder {
  import scala.language.implicitConversions
  implicit def buildBuilder[T](builder: Builder[T]): T = builder.build
}

trait Builder[T] {
  def build: T

  override def toString: String = build.toString
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

  def withTextParagraph(x: String, markAsSummary: Boolean = true): this.type = {
    var paragraphBuilder = new ParagraphBuilder().withText(x)
    if (markAsSummary) paragraphBuilder = paragraphBuilder.asSummary
    withParagraph(paragraphBuilder.build)
  }

  protected def withBlockContent[T <: BlockContentOption : CanWriteXML](x: T): this.type = withContent(dataRecord(x))
  protected def withContent(x: DataRecord[_]): this.type
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

  protected def withEnrichedText[T <: EnrichedTextOption : CanWriteXML](x: T): this.type = withContent(dataRecord(x))
  protected def withContent(x: DataRecord[_]): this.type
}

trait MixedContentBuilder {
  def withText(x: String): this.type = withContent(dataRecord(x))

  /** Appends the given XML to the model object.
    * Note that this method is _not_ type-safe!
    * __No__ validation is performed to verify that the XML matches the expected schema..
    */
  def withXml(x: NodeSeq): this.type = withContent(dataRecord(x))

  protected def withContent(x: DataRecord[_]): this.type
}

/** An extension point to enable writing custom XML to NITF model objects that support extensions.
  * This feature is supported by NITF 3.6 where the model objects contain a special field called ''any'', which acts as
  * an extension point for provider-defined properties from other namespaces.
  *
  * This is an example of how it can be used (with versions 3.6.x only):
  * {{{
  *   import com.gu.nitf.model.builders._
  *   import com.gu.nitf.scalaxb._
  *   import scalaxb._
  *
  *   val builder = new NitfBuilder() with AnyExtensionsBuilder {
  *     protected override def withAny(x: DataRecord[_]) = { build = build.copy(any = build.any :+ x); this }
  *   }
  *
  *   val nitf = builder.withXml(<my:extension/>).build
  *   val xml = scalaxb.toXML(nitf, None, None, toScope(Some("my") -> "http://www.example.com/my-extension"))
  * }}}
  */
trait AnyContentBuilder { this: Builder[_ <: { def any: Seq[scalaxb.DataRecord[Any]] }] =>
  def withXml(x: NodeSeq): this.type = withAny(dataRecord(x))
  protected def withAny(x: DataRecord[_]): this.type
}

class NitfBuilder(var build: Nitf = Nitf(body = Body())) extends Builder[Nitf] {
  def withBody(x: Body): this.type = { build = build.copy(body = x); this }
  def withHead(x: Head): this.type = { build = build.copy(head = Option(x)); this }
  def withUno(x: String): this.type = { build = build.copy(uno = Option(x)); this }
}

class HeadBuilder(var build: Head = Head()) extends Builder[Head] {
  def withTitle(x: String, titleType: Option[TitleType] = None): this.type = {
    build = build.copy(title = Option(x).map(t => Title(Seq(dataRecord(t)), typeValue = titleType)))
    this
  }
  def withDocData(x: Docdata): this.type = { build = build.copy(docdata = Option(x)); this }
  def withPublicationData(x: Pubdata): this.type = { build = build.copy(pubdata = build.pubdata :+ x); this }
}

class DocDataBuilder(var build: Docdata = Docdata()) extends Builder[Docdata] {
  def withDocId(x: String): this.type = withDocId(DocId(idString = Option(x)))
  def withDocId(x: DocId): this.type = withDocDataOption(x)
  def withCopyright(x: DocCopyright): this.type = withDocDataOption(x)
  def withIssueDate(x: LocalDate): this.type = withDocDataOption(DateIssue(norm = optionalString(x)))
  def withReleaseDate(x: LocalDate): this.type = withDocDataOption(DateRelease(norm = optionalString(x)))
  def withUrgency(x: NewsUrgency.Value): this.type = withDocDataOption(Urgency(edUrg = optionalString(x)))
  def withManagementStatus(x: ManagementStatus.Value): this.type = {
    build = build.copy(managementStatus = optionalString(x))
    this
  }
  protected def withDocDataOption[T <: DocdataOption : CanWriteXML](x: T): this.type = {
    build = build.copy(docdataoption = build.docdataoption :+ dataRecord(x))
    this
  }
}

class DocCopyrightBuilder(var build: DocCopyright = DocCopyright()) extends Builder[DocCopyright] {
  def withHolder(x: String): this.type = { build = build.copy(holder = optionalString(x)); this }
  def withYear(x: Int): this.type = { build = build.copy(year = optionalString(x)); this }
}

class DocIdBuilder(var build: DocId = DocId()) extends Builder[DocId] {
  def withId(x: String): this.type = { build = build.copy(idString = optionalString(x)); this }
  def withSource(x: String): this.type = { build = build.copy(regsrc = optionalString(x)); this }
}

class PublicationDataBuilder(var build: Pubdata = Pubdata()) extends Builder[Pubdata] {
  def withType(x: PublicationType): this.type = { build = build.copy(typeValue = Option(x)); this }
  def withPublicationDate(x: LocalDate): this.type = { build = build.copy(datePublication = optionalString(x)); this }
}

class BodyBuilder(var build: Body = Body()) extends Builder[Body] {
  def withHead(x: BodyHead): this.type = { build = build.copy(bodyHead = Option(x)); this }
  def withContent(x: BodyContent): this.type = { build = build.copy(bodyContent = build.bodyContent :+ x); this }
  def withEnd(x: BodyEnd): this.type = { build = build.copy(bodyEnd = Option(x)); this }
}

class BodyHeadBuilder(var build: BodyHead = BodyHead()) extends Builder[BodyHead] {
  def withByline(x: String): this.type = withByline(Byline(Seq(dataRecord(x))))
  def withHeadline(x: String): this.type = withHeadline(new HeadlineBuilder().withPrimaryHeadline(x))

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

class AbstractBuilder(var build: Abstract = Abstract()) extends Builder[Abstract] with BlockContentBuilder {
  protected def withContent(x: DataRecord[_]): this.type = {
    build = build.copy(abstractoption = build.abstractoption :+ x)
    this
  }
}

class HeadlineBuilder(var build: Hedline = Hedline(Hl1())) extends Builder[Hedline] {
  def withPrimaryHeadline(x: Hl1): this.type = { build = build.copy(hl1 = x); this }
  def withPrimaryHeadline(x: String): this.type = { withPrimaryHeadline(new PrimaryHeadlineBuilder(build.hl1).withText(x)) }
  def withSubordinateHeadline(x: Hl2): this.type = { build = build.copy(hl2 = build.hl2 :+ x); this }
  def withSubordinateHeadline(x: String): this.type = { withSubordinateHeadline(Hl2(Seq(dataRecord(x)))); this }
}

class PrimaryHeadlineBuilder(var build: Hl1 = Hl1()) extends Builder[Hl1] with EnrichedTextBuilder with MixedContentBuilder {
  protected def withContent(x: DataRecord[_]): this.type = { build = build.copy(mixed = build.mixed :+ x); this }
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

class MediaBuilder(var build: Media) extends Builder[Media] {
  def this(mediaType: MediaType.Value) = this(Media(mediaType = mediaType.toString))

  def withMetadata(key: String, value: String): this.type = withMetadata(new MediaMetadataBuilder(key, value))
  def withMetadata(x: MediaMetadata): this.type = { build = build.copy(mediaMetadata = build.mediaMetadata :+ x); this }
  def withCaption(x: MediaCaption): this.type = { build = build.copy(mediaCaption = build.mediaCaption :+ x); this }
  def withType(x: MediaType.Value): this.type = { build = build.copy(mediaType = x.toString); this }
  def withProducer(x: MediaProducer): this.type = {
    build = build.copy(mediaProducer = Chooser.choose(build.mediaProducer, x))
    this
  }
  def withReference(x: MediaReference, y: Option[MediaObject] = None): this.type = {
    build = build.copy(mediasequence1 = build.mediasequence1 :+ MediaSequence1(x, y))
    this
  }
}

class MediaCaptionBuilder(var build: MediaCaption = MediaCaption())
    extends Builder[MediaCaption] with BlockContentBuilder with EnrichedTextBuilder  with MixedContentBuilder {
  protected override def withContent(x: DataRecord[_]): this.type = { build = build.copy(build.mixed :+ x); this }
}

class MediaMetadataBuilder(var build: MediaMetadata) extends Builder[MediaMetadata] {
  def this(name: String) = this(MediaMetadata(name = name))
  def this(name: String, value: String) = this(MediaMetadata(name = name, valueAttribute = Option(value)))

  def withName(x: String): this.type = { build = build.copy(name = x); this }
  def withValue(x: String): this.type = { build = build.copy(valueAttribute = Option(x)); this }
}

class MediaReferenceBuilder(var build: MediaReference = MediaReference()) extends Builder[MediaReference] with MixedContentBuilder {
  def asNoFlow: this.type = { build = build.copy(noflow = Some(NoflowValue)); this }
  def withSource(x: URI): this.type = withSource(x.toString)
  def withSource(x: String): this.type = { build = build.copy(source = Option(x)); this }
  def withCoding(x: String): this.type = { build = build.copy(coding = Option(x)); this }
  def withOutCue(x: String): this.type = { build = build.copy(outcue = Option(x)); this }
  def withImageMap(x: String): this.type = { build = build.copy(imagemap = Option(x)); this }
  def withWidth(x: Int): this.type = { build = build.copy(width = Option(x.toString)); this }
  def withHeight(x: Int): this.type = { build = build.copy(height = Option(x.toString)); this }
  def withCopyright(x: String): this.type = { build = build.copy(copyright = Option(x)); this }
  def withMimeType(x: String): this.type = { build = build.copy(mimeType = Option(x)); this }
  def withAlternateName(x: String): this.type = { build = build.copy(name = Option(x)); this }
  def withAlternateText(x: String): this.type = { build = build.copy(alternateText = Option(x)); this }
  def withSourceCredit(x: String): this.type = { build = build.copy(sourceCredit = Option(x)); this }
  def withTimeUnitOfMeasure(x: String): this.type = { build = build.copy(timeUnitOfMeasure = Option(x)); this }
  def withTimeLength(x: Int): this.type = { build = build.copy(time = Option(x.toString)); this }

  protected override def withContent(x: DataRecord[_]): this.type = { build = build.copy(build.mixed :+ x); this }
}

class ParagraphBuilder(var build: P = P()) extends Builder[P] with EnrichedTextBuilder with MixedContentBuilder {
  def asLead: this.type = { build = build.copy(lede = Option("true")); this }
  def asSummary: this.type = { build = build.copy(summary = Option("true")); this }
  def asOptional: this.type = { build = build.copy(optionalText = Option("true")); this }

  protected override def withContent(x: DataRecord[_]): this.type = {
    build = build.copy(mixed = build.mixed :+ x)
    this
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
