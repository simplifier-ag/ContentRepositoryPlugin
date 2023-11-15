package io.simplifier.plugin.contentrepo.pluginBaseRelated.reducer.reductionChanges

import io.simplifier.plugin.contentrepo.pluginBaseRelated.reducer.ReducibleJSONDataTypes._
import io.simplifier.plugin.contentrepo.pluginBaseRelated.reducer.ReductionChanges._
import io.simplifier.plugin.contentrepo.pluginBaseRelated.reducer.reductionChanges.RemovedElement.RemovedElementSerializer
import io.simplifier.plugin.contentrepo.pluginBaseRelated.reducer.{ReducibleJSONDataTypes, ReductionChanges, determineSizeString}
import io.simplifier.pluginbase.util.logging.ExceptionFormatting
import org.json4s.{JValue, _}

import scala.util.Try
import scala.util.matching.Regex


/**
 * The reduction change case class containing all information about a reduction Change.
 *
 * @param index          the optional index, if the reduced element was a value in an index.
 * @param key            the optional key, if the reduced element was a value in an object.
 * @param `type`         the [[ReducibleJSONDataTypes]]
 * @param originalSize   the original size of the element. A <b>-1</b> indicates that this element cannot be reduced.
 * @param reducedSize    the original size of the element. A <b>-1</b> indicates that no reduction has occurred.
 * @param removedElement the optional removed element.
 * @param changes        the optional changes for arrays or objects, as they have multiple elements.
 */
 case class ReductionChange(index: Option[Int],
                                            key: Option[String],
                                            `type`: ReducibleJSONDataTypes,
                                            originalSize: Int,
                                            reducedSize: Int,
                                            removedElement: Option[RemovedElement],
                                            changes: Option[ReductionChanges]) {

  import ReductionChange._


  /**
   * Returns the respective Json representation.
   *
   * @return the respective Json representation.
   */
  def toJson: JValue = {
    JObject(
      JField(FIELD_NAME_INDEX, index.fold[JValue](JNothing)(JInt(_))),
      JField(FIELD_NAME_KEY, key.fold[JValue](JNothing)(JString)),
      JField(FIELD_NAME_TYPE, `type`.toJson),
      JField(FIELD_NAME_ORIGINAL_SIZE, determineSizeString(originalSize, `type`)),
      JField(FIELD_NAME_REDUCED_SIZE, determineSizeString(reducedSize, `type`)),
      JField(FIELD_NAME_REMOVED_ELEMENT, removedElement.fold[JValue](JNothing)(_.toJson)),
      JField(FIELD_NAME_CHANGES, changes.fold[JValue](JNothing)(_.toJson))
    )
  }


  /**
   * Returns the respective String representation.
   *
   * @return the respective String representation.
   */
  override def toString: String = {
    val indexString: String = index.fold("without an index")(i => s"with the index: [$i]")
    val keyString: String = key.fold("without a key")(k => s"with the key: [$k]")
    val originalSizeString: String = if (originalSize == -1) "without an original size" else s"with the original size: [$originalSize]"
    val reducedSizeString: String = if (reducedSize == -1) "without a reduced size" else s"with the reduced size: [$reducedSize]"
    val removedElementString: String = removedElement.fold("without any removed element")(c => {
      val changesStringSanitized: String = Character.toLowerCase(c.toString.charAt(0)) + c.toString.substring(1).stripSuffix(".")
      s"with $changesStringSanitized"
    })
    val changesString: String = changes.fold("without any changes")(c => {
      val changesStringSanitized: String = Character.toLowerCase(c.toString.charAt(0)) + c.toString.substring(1).stripSuffix(".")
      s"with $changesStringSanitized"
    })

    s"A ${`type`} change $indexString, $keyString, $originalSizeString, $reducedSizeString, $removedElementString and $changesString."
  }
}


/** The Reduction Change Companion Object */
 object ReductionChange extends ExceptionFormatting {

  /** JSON Serializer for [[ReductionChange]]. */
  lazy val ReductionChangeSerializer: CustomSerializer[ReductionChange] = {
    new CustomSerializer[ReductionChange](_ => {
      implicit val formats: Formats = DefaultFormats.withPre36DeserializationBehavior + REDUCIBLE_JSON_DATA_TYPES_TYPE_HINTS +
        ReducibleJSONDataTypes.ReducibleJSONDataTypesSerializer + RemovedElementSerializer

      val deserializer: PartialFunction[JValue, ReductionChange] = {
        case obj: JObject if Extraction.extractOpt[ReductionChange](obj).nonEmpty => Extraction.extract[ReductionChange](obj)
        case JObject(fields) => constructFromFields(fields)
        case _ => apply()
      }
      val serializer: PartialFunction[Any, JValue] = {
        case change: ReductionChange => change.toJson
      }
      (deserializer, serializer)
    })
  }

  /**
   * Creates a default [[ReductionChange]].
   *
   * @return the default [[ReductionChange]].
   */
  def apply(): ReductionChange = {
    apply(
      `type` = NonReducibleJson(),
      originalSize = -1,
      reducedSize = -1
    )
  }


  /**
   * Creates a new [[ReductionChange]] without any optional information.
   *
   * @param `type`       the [[ReducibleJSONDataTypes]]
   * @param originalSize the original size of the element. A <b>-1</b> indicates that this element cannot be reduced.
   * @param reducedSize  the original size of the element. A <b>-1</b> indicates that no reduction has occurred.
   * @return the [[ReductionChange]] without any optional information.
   */
  def apply(`type`: ReducibleJSONDataTypes,
            originalSize: Int,
            reducedSize: Int): ReductionChange = {
    new ReductionChange(
      index = None,
      key = None,
      `type` = `type`,
      originalSize = originalSize,
      reducedSize = reducedSize,
      removedElement = None,
      changes = None
    )
  }


  private[reducer] val FIELD_NAME_INDEX: String = "Index"
  private[reducer] val FIELD_NAME_KEY: String = "Key"
  private[reducer] val FIELD_NAME_TYPE: String = "Type"
  private[reducer] val FIELD_NAME_ORIGINAL_SIZE: String = "Original Size"
  private[reducer] val FIELD_NAME_REDUCED_SIZE: String = "Reduced Size"
  private[reducer] val FIELD_NAME_REMOVED_ELEMENT: String = "Removed Element"
  private[reducer] val FIELD_NAME_CHANGES: String = "Changes"


  private[this] val SIZE_STRING: Regex = "((?:\\d+(?:\\.\\d+)*)).*".r.unanchored


  private[this] def constructFromFields(fields: Seq[JField]): ReductionChange = {
    new ReductionChange(
      index = fields.collectFirst {
        case JField(FIELD_NAME_INDEX, JInt(i)) if i <= Integer.MAX_VALUE => i.toInt
      },

      key = fields.collectFirst {
        case JField(FIELD_NAME_KEY, JString(k)) => k
      },

      `type` = fields.collectFirst {
        case JField(FIELD_NAME_TYPE, JString(t)) => ReducibleJSONDataTypes(t)
      }.getOrElse(NonReducibleJson()),

      originalSize = fields.collectFirst {
        case JField(FIELD_NAME_ORIGINAL_SIZE, JInt(size)) if size <= Integer.MAX_VALUE => size.toInt
        case JField(FIELD_NAME_ORIGINAL_SIZE, JString(SIZE_STRING(size))) if canParseIntFromFormattedString(size) => parseIntFromFormattedString(size)
      }.getOrElse(-1),

      reducedSize = fields.collectFirst {
        case JField(FIELD_NAME_REDUCED_SIZE, JInt(size)) if size <= Integer.MAX_VALUE => size.toInt
        case JField(FIELD_NAME_REDUCED_SIZE, JString(SIZE_STRING(size))) if canParseIntFromFormattedString(size) => parseIntFromFormattedString(size)
      }.getOrElse(-1),

      removedElement = fields
        .collectFirst {
          case JField(FIELD_NAME_REMOVED_ELEMENT, value) => Extraction.extractOpt[RemovedElement](value)(DefaultFormats.lossless + RemovedElementSerializer, manifest[RemovedElement])
        }.flatten match {
        case Some(re) if re.isEmpty => None
        case re => re
      },

      changes = {
        fields
          .collectFirst {
            case JField(FIELD_NAME_CHANGES, value) => value \ FIELD_NAME_CHANGED_ELEMENTS match {
              case JArray(values) => values.collect {
                case JObject(f) => constructFromFields(f)
              }
              case JObject(f) => Seq(constructFromFields(f))
              case _ => Seq.empty[ReductionChange]
            }
          }
          .filter(_.nonEmpty)
          .map(ReductionChanges(_))
      }
    )
  }


  private[this] def parseIntFromFormattedString(string: String): Int = {
    java.lang.Long.parseLong(string.replace(".", "")).toInt
  }


  private[this] def canParseIntFromFormattedString(string: String): Boolean = {
    Try(parseIntFromFormattedString(string)).map(_ <= Integer.MAX_VALUE).getOrElse(false)
  }
}