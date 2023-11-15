package io.simplifier.plugin.contentrepo.pluginBaseRelated.reducer.reductionResults

import io.simplifier.pluginbase.util.json.NamedTupleAccess._
import io.simplifier.plugin.contentrepo.pluginBaseRelated.reducer._
import io.simplifier.plugin.contentrepo.pluginBaseRelated.reducer.reductionChanges.ReductionChange
import io.simplifier.pluginbase.util.json.SimplifierFormats
import org.json4s._

import scala.reflect.runtime.universe._


/**
 * The original unreduced Json result.
 *
 * @note                   the reduced value may be null.
 * @param reducedJValue    the reduced Json Value.
 * @param originalSize     the original size.
 * @param typeTag          the implicit type tag.
 *
 * @note                   this class is necessary, because the generic class with the Type T cannot handle more specific subtypes.
 * @tparam T               the type parameter for the reduced Json value.
 */
case class OriginalResultJValue[T <: JValue](reducedJValue: T,
                                                              originalSize: Int)
                                                             (implicit typeTag: TypeTag[T]) extends ReductionResult[T] {

  import OriginalResultJValue._

  override protected implicit val formats: Formats = SimplifierFormats.formats.withPre36DeserializationBehavior

  override val `type`: String = TYPE_NAME
  override val wasReduced: Boolean = false
  override val reducedValue: T = reducedJValue
  override val changes: ReductionChanges = ReductionChanges(Seq.empty[ReductionChange])
  override val reducedSize: Int = originalSize


  override def getOriginalValueAsJValue(implicit formats: Formats): JValue = {
    if (reducedJValue == null) JNothing else reducedJValue
  }

  override def getReducedValueAsJValue(implicit formats: Formats): JValue = getOriginalValueAsJValue

  override def toString: String = createStringForTypedValue[T](wasReduced, originalSize, reducedSize, `type`,
    typeStringFilterFunction(_), Some(determineJsonType(reducedValue)))

  override protected def determineSizeString(size: Int,
                                             value: JValue): JValue = {
    if (size == DEFAULT_SIZE) JNothing
    else JString(s"${LENGTH_FORMATTER.format(size)} [Byte]$determineSizeStringSuffix")
  }


  private[this] def determineSizeStringSuffix: String = {
    reducedValue match {
      case JArray(values) if !isByteArrayEquivalent(values) => " (Compactly Rendered JSON-Array Size)"
      case JObject(_) => " (Compactly Rendered JSON-Object Size)"
      case _ => ""
    }
  }

}


/** The Original Json Result Companion Object */
object OriginalResultJValue {


  /**
   * Constructs a new [[OriginalResultJValue]].
   *
   * @param value    the Json Value to construct the [[OriginalResultJValue]] from.
   *
   * @return         the new [[OriginalResultJValue]].
   */
  def apply[_ <: JValue](value: JValue): OriginalResultJValue[_ <: JValue] = {
    implicit val formats: Formats = DefaultFormats.lossless.withPre36DeserializationBehavior

    val originalSize: Int = value match {
      case JObject(fields) => fields.collectFirst {
        case JField(name, JInt(size)) if name == ReductionResult.FIELD_NAME_ORIGINAL_SIZE => size.toInt
        case JField(name, JInt(size)) if name == FIELD_NAME_ORIGINAL_SIZE => size.toInt
      }.getOrElse(DEFAULT_SIZE)
      case _ => DEFAULT_SIZE
    }

    val reducedValue: JValue = value match {
      case JObject(fields) => fields.collectFirst {
        case JField(name, v) if name == ReductionResult.FIELD_NAME_REDUCED_VALUE => Extraction.extract[JValue](v)
        case JField(name, v) if name == FIELD_NAME_REDUCED_JVALUE => Extraction.extract[JValue](v)
      }.getOrElse(JNothing)
      case _ => JNothing
    }

    new OriginalResultJValue(
      reducedJValue = reducedValue,
      originalSize = originalSize
    )
  }

  private[OriginalResultJValue] val JSON_AST_PREFIX: String = "JsonAST$"

  private[this] val FIELD_NAME_REDUCED_JVALUE: String = "reducedJValue"
  private[this] val FIELD_NAME_ORIGINAL_SIZE: String = "originalSize"

  val TYPE_NAME: String = "Original Unreduced Json Result"


  private[reductionResults] def typeStringFilterFunction(typeString: String): String = {
    typeString.stripPrefix(JSON_AST_PREFIX)
  }


   def isOriginalJValueResult(value: JValue): Boolean = {
    implicit val formats: Formats = DefaultFormats.lossless.withPre36DeserializationBehavior

    def reductionResultOriginalResultJValueWorkflow(value: JValue): Boolean = {
      value match {
        case JObject(fields) => fields.contains(JField(ReductionResult.FIELD_NAME_TYPE, JString(TYPE_NAME))) ||
          (fields.exists(field => field.name == FIELD_NAME_REDUCED_JVALUE) &&
            fields.exists(field => field.name == FIELD_NAME_ORIGINAL_SIZE))
        case _ => false
      }
    }
    ReductionResult.reductionResultCheckWorkflow[OriginalResultJValue[_]](value, reductionResultOriginalResultJValueWorkflow, TYPE_NAME)
  }

}

