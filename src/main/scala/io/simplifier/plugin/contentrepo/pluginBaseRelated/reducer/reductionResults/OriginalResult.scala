package io.simplifier.plugin.contentrepo.pluginBaseRelated.reducer.reductionResults

import io.simplifier.pluginbase.util.json.NamedTupleAccess._
import io.simplifier.plugin.contentrepo.pluginBaseRelated.reducer._
import io.simplifier.plugin.contentrepo.pluginBaseRelated.reducer.reductionChanges.ReductionChange
import io.simplifier.plugin.contentrepo.pluginBaseRelated.reducer.reductionResults.OriginalResult.TYPE_NAME
import io.simplifier.plugin.contentrepo.pluginBaseRelated.types.{TypeTagCreator, TypeTagMatcher}
import org.json4s._

import scala.reflect.ClassTag
import scala.reflect.runtime.universe._

/**
 * The original unreduced result.
 *
 * @note the reduced value may be null.
 * @param reducedValue the reduced value.
 * @param originalSize the original size.
 * @param formats      the implicit formats.
 * @param typeTag      the implicit type tag.
 * @tparam T the type parameter for the reduced value.
 *
 */
case class OriginalResult[T](reducedValue: T,
                                              originalSize: Int)
                                             (implicit val formats: Formats,
                                              typeTag: TypeTag[T]) extends ReductionResult[T] with TypeTagMatcher with TypeTagCreator {
  override val `type`: String = TYPE_NAME
  override val wasReduced: Boolean = false
  override val reducedSize: Int = DEFAULT_SIZE
  override val changes: ReductionChanges = ReductionChanges(Seq.empty[ReductionChange])


  override def getOriginalValueAsJValue(implicit formats: Formats): JValue = {
    if (reducedValue == null) JNothing else Extraction.decompose(reducedValue)
  }

  override def getReducedValueAsJValue(implicit formats: Formats): JValue = getOriginalValueAsJValue

  override def toString: String = {
    val sizeString: String = originalSize match {
      case DEFAULT_SIZE => "without a clear length"
      case other => s"with the length of ${LENGTH_FORMATTER.format(other)} [Byte]"
    }

    val typeString: String = reducedValue match {
      case value if isAssignable[T, Any](value) => OriginalResultJValue.typeStringFilterFunction(createString(typeTag))
      case _ => createString(typeTag)
    }

    s"The ${if (reducedValue == null) "Unknown " else ""}${`type`} of the type: [$typeString] $sizeString."
  }
}


/** The Original Result Companion Object */
object OriginalResult {

  /**
   * Constructs a new [[OriginalResult]].
   *
   * @param value   the Json Value to construct the [[OriginalResult]] from.
   * @param formats the implicit Json formats.
   * @return the new [[OriginalResult]].
   */
  def apply[T: TypeTag : ClassTag : Manifest](value: JValue)
                                             (implicit formats: Formats): OriginalResult[T] = {
    val originalSize: Int = value match {
      case JObject(fields) => fields.collectFirst {
        case JField(name, JInt(size)) if name == ReductionResult.FIELD_NAME_ORIGINAL_SIZE => size.toInt
        case JField(name, JInt(size)) if name == FIELD_NAME_ORIGINAL_SIZE => size.toInt
      }.getOrElse(DEFAULT_SIZE)
      case _ => DEFAULT_SIZE
    }

    val reducedValue: T = value match {
      case JObject(fields) =>
        fields.collectFirst {
          case JField(name, v) if name == ReductionResult.FIELD_NAME_REDUCED_VALUE => Extraction.extractOpt[T](v)
          case JField(name, v) if name == FIELD_NAME_REDUCED_VALUE => Extraction.extractOpt[T](v)
        }.flatten.getOrElse(null.asInstanceOf[T])
      case _ => null.asInstanceOf[T]
    }

    new OriginalResult[T](
      reducedValue = reducedValue,
      originalSize = originalSize
    )
  }


  /** JSON Serializer for [[OriginalResult]] */
  def OriginalResultChangesSerializer[T: TypeTag : Manifest]()(implicit formats: Formats): CustomSerializer[OriginalResult[T]] = {
    new CustomSerializer[OriginalResult[T]](_ => {
      val deserializer: PartialFunction[JValue, OriginalResult[T]] = {
        case json => apply[T](json)
      }
      val serializer: PartialFunction[Any, JValue] = {
        case result: OriginalResult[_] => result.toJson(Option(formats))
      }
      (deserializer, serializer)
    })
  }


  private[this] val FIELD_NAME_REDUCED_VALUE: String = "reducedValue"
  private[this] val FIELD_NAME_ORIGINAL_SIZE: String = "originalSize"

  val TYPE_NAME: String = "Original Unreduced Result"

  def isOriginalResult(value: JValue)
                                       (implicit formats: Formats): Boolean = {
    def reductionResultOriginalResultWorkflow(value: JValue): Boolean = {
      value match {
        case JObject(fields) => fields.contains(JField(ReductionResult.FIELD_NAME_TYPE, JString(TYPE_NAME))) ||
          (fields.exists(field => field.name == FIELD_NAME_REDUCED_VALUE) &&
            fields.exists(field => field.name == FIELD_NAME_ORIGINAL_SIZE))
        case _ => false
      }
    }

    ReductionResult.reductionResultCheckWorkflow[OriginalResult[_]](value, reductionResultOriginalResultWorkflow, TYPE_NAME)
  }


}