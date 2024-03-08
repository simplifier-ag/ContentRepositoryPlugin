package io.simplifier.plugin.contentrepo.pluginBaseRelated.reducer

import akka.util.ByteString
import io.simplifier.plugin.contentrepo.pluginBaseRelated.reducer.ReductionChanges.ReductionChangesSerializer
import io.simplifier.plugin.contentrepo.pluginBaseRelated.reducer.ReductionResult.{FIELD_NAME_CHANGES, FIELD_NAME_ORIGINAL_SIZE, FIELD_NAME_ORIGINAL_VALUE, FIELD_NAME_REDUCED_SIZE, FIELD_NAME_REDUCED_VALUE, FIELD_NAME_TYPE, FIELD_NAME_WAS_REDUCED}
import io.simplifier.plugin.contentrepo.pluginBaseRelated.reducer.reductionResults.{OriginalResult, OriginalResultJValue, ReductionResultByteString, ReductionResultJArray, ReductionResultJObject, ReductionResultJString, ReductionResultLoggingMessages, ReductionResultReducedStringCreator, ReductionResultString}
import io.simplifier.pluginbase.util.xcoder.ByteArrayEncoding
import org.json4s.{JValue, _}
import org.slf4j.{Logger, LoggerFactory}

import scala.util.{Failure, Success, Try}


/**
 * The reduction result.
 *
 * @tparam T the type parameter for the reduced value.
 */
trait ReductionResult[+T] extends ByteArrayEncoding with ReductionResultReducedStringCreator {


  protected implicit val formats: Formats

  /** The type of reduction result */
  val `type`: String

  /** This flag indicates, whether a reduction occurred or not */
  val wasReduced: Boolean

  /** The reduced value */
  val reducedValue: T

  /** The original size */
  val originalSize: Int

  /** The reduced size */
  val reducedSize: Int

  /** The reduction changed (can be empty if no changes occurred) */
  val changes: ReductionChanges


  /**
   * Returns the original values as a JValue.
   *
   * @param formats the implicit Json formats.
   * @return the original values as a JValue.
   */
  def getOriginalValueAsJValue(implicit formats: Formats): JValue


  /**
   * Returns the reduced values as a JValue.
   *
   * @param formats the implicit Json formats.
   * @return the reduced values as a JValue.
   */
  def getReducedValueAsJValue(implicit formats: Formats): JValue


  /**
   * Returns the Json-representation.
   *
   * @param jsonFormats the optional Json formats.
   * @return the Json-representation.
   */
  def toJson(jsonFormats: Option[Formats]): JObject = {
    JObject(
      JField(FIELD_NAME_TYPE, JString(`type`)),
      JField(FIELD_NAME_WAS_REDUCED, JBool(wasReduced)),
      JField(FIELD_NAME_ORIGINAL_VALUE, getOriginalValueAsJValue(jsonFormats.getOrElse(formats))),
      JField(FIELD_NAME_REDUCED_VALUE, getReducedValueAsJValue(jsonFormats.getOrElse(formats))),
      JField(FIELD_NAME_ORIGINAL_SIZE, determineSizeString(originalSize, getOriginalValueAsJValue(jsonFormats.getOrElse(formats)))),
      JField(FIELD_NAME_REDUCED_SIZE, determineSizeString(reducedSize, getReducedValueAsJValue(jsonFormats.getOrElse(formats)))),
      JField(FIELD_NAME_CHANGES, changes.toJson)
    )
  }


  /**
   * Returns the reduced Json-representation.
   *
   * @note the original value will be omitted.
   * @note should the parameter: <b>omitReducedValue</b> be true, then the reduced value cannot be restored either.
   * @param omitValues  the flag, whether the values should be omitted from the Json or not.
   * @param jsonFormats the optional Json formats.
   * @return the reduced Json-representation.
   */
  def toReducedJson(omitValues: Boolean,
                    jsonFormats: Option[Formats]): JObject = {
    JObject(
      JField(FIELD_NAME_TYPE, JString(`type`)),
      JField(FIELD_NAME_WAS_REDUCED, JBool(wasReduced)),
      JField(FIELD_NAME_ORIGINAL_VALUE, if (wasReduced || omitValues) JNothing else getOriginalValueAsJValue(jsonFormats.getOrElse(formats))),
      JField(FIELD_NAME_REDUCED_VALUE, if (wasReduced && !omitValues) getReducedValueAsJValue(jsonFormats.getOrElse(formats)) else JNothing),
      JField(FIELD_NAME_ORIGINAL_SIZE, if (originalSize == -1 || !wasReduced && omitValues) JNothing else determineSizeString(originalSize, getOriginalValueAsJValue(jsonFormats.getOrElse(formats)))),
      JField(FIELD_NAME_REDUCED_SIZE, if (reducedSize == -1 || !wasReduced) JNothing else determineSizeString(reducedSize, getReducedValueAsJValue(jsonFormats.getOrElse(formats)))),
      JField(FIELD_NAME_CHANGES, changes.toJson)
    )
  }

  /**
   * Determines the Json-String for the size.
   *
   * @param size  the size.
   * @param value the provided Json Value i.e. the reduced or the original.
   * @return the respective Json-String.
   */
  protected def determineSizeString(size: Int,
                                    value: JValue): JValue = {
    if (size == DEFAULT_SIZE) JNothing
    else JString(s"${LENGTH_FORMATTER.format(size)} [Byte]")
  }
}


/** The Reduction Result Companion Object */
object ReductionResult extends ReductionResultLoggingMessages {

  import scala.reflect.runtime.universe.TypeTag

  val Logger: Logger = LoggerFactory.getLogger(getClass.getName.stripSuffix("$"))

  /**
   * JSON Serializer for [[ReductionResult]].
   *
   * @note with serializer a Json cannot be completely deserialized into the respective case class, as the original value is lost.
   *       Please use the [[ReductionResultFullSerializer]] if you want a complete deserialization.
   * @note should the parameter: <b>omitReducedValue</b> be true, then the reduced value cannot be restored either.
   * @param omitReducedValue the flag, whether the reduced value should be omitted from the Json or not.
   * @param formats          the formats for the original result.
   * @tparam T the type parameter for the generic reduction result.
   * @return the respective CustomSerializer for [[ReductionResult]].
   */
  def ReductionResultSmallSerializer[T: TypeTag : Manifest](omitReducedValue: Boolean,
                                                            formats: Formats): CustomSerializer[ReductionResult[_]] = {
    new CustomSerializer[ReductionResult[_]](_ => {
      implicit val FORMATS: Formats = createFormats(formats)

      val deserializer: PartialFunction[JValue, ReductionResult[_]] = reductionResultDeserializer[T]
      val serializer: PartialFunction[Any, JValue] = {
        case reducingResult: ReductionResult[_] => reducingResult.toReducedJson(omitReducedValue, Some(FORMATS))
      }
      (deserializer, serializer)
    })
  }

  /**
   * JSON Serializer for [[ReductionResult]].
   *
   * @param formats the formats for the original result.
   * @tparam T the type parameter for the generic reduction result.
   * @return the respective CustomSerializer for [[ReductionResult]].
   */
  def ReductionResultFullSerializer[T: TypeTag : Manifest](formats: Formats): CustomSerializer[ReductionResult[_]] = {
    new CustomSerializer[ReductionResult[_]](_ => {
      implicit val FORMATS: Formats = createFormats(formats)

      val deserializer: PartialFunction[JValue, ReductionResult[_]] = reductionResultDeserializer[T]
      val serializer: PartialFunction[Any, JValue] = {
        case reducingResult: ReductionResult[_] => reducingResult.toJson(Some(FORMATS))
      }
      (deserializer, serializer)
    })
  }


  private[reducer] val FIELD_NAME_TYPE: String = "Type"
  private[reducer] val FIELD_NAME_WAS_REDUCED: String = "Was Reduced"
  private[reducer] val FIELD_NAME_REDUCED_VALUE: String = "Reduced Value"
  private[reducer] val FIELD_NAME_REDUCED_JVALUE: String = "Reduced JValue"
  private[reducer] val FIELD_NAME_ORIGINAL_VALUE: String = "Original Value"
  private[reducer] val FIELD_NAME_ORIGINAL_SIZE: String = "Original Size"
  private[reducer] val FIELD_NAME_REDUCED_SIZE: String = "Reduced Size"
  private[reducer] val FIELD_NAME_CHANGES: String = "Changes"


  protected[reducer] def reductionResultCheckWorkflow[T <: ReductionResult[_] : Manifest](value: JValue,
                                                                                          nonExtractWorkflow: JValue => Boolean,
                                                                                          `type`: String)
                                                                                         (implicit formats: Formats): Boolean = {
    Logger.trace(CheckingWhetherProvidedJsonEqualsDesiredReductionResultType(value, `type`))
    Try(Extraction.extractOpt[T](value).nonEmpty || Option(nonExtractWorkflow).exists(_(value))) match {
      case Failure(e) => Logger.warn(ErrorDuringReductionResultTypeCheck(e, value, `type`)); throw e
      case Success(isReductionResult) =>
        Logger.trace(CheckedWhetherProvidedJsonEqualsDesiredReductionResultType(value, isReductionResult, `type`))
        isReductionResult
    }
  }


  private[this] def createFormats(formats: Formats): Formats = formats + ReductionChangesSerializer


  private[this] def reductionResultDeserializer[T: TypeTag : Manifest](implicit formats: Formats): PartialFunction[JValue, ReductionResult[_]] = {
    case obj: JObject if ReductionResultString.isReductionResultString(obj) =>
      reductionResultDeserializerWorkflow[String](obj, Try(ReductionResultString(obj)), ReductionResultString.TYPE_NAME)
    case obj: JObject if ReductionResultJString.isReductionResultJString(obj) =>
      reductionResultDeserializerWorkflow[JString](obj, Try(ReductionResultJString(obj)), ReductionResultJString.TYPE_NAME)
    case obj: JObject if ReductionResultJArray.isReductionResultJArray(obj) =>
      reductionResultDeserializerWorkflow[JArray](obj, Try(ReductionResultJArray(obj)), ReductionResultJArray.TYPE_NAME)
    case obj: JObject if ReductionResultJObject.isReductionResultJObject(obj) =>
      reductionResultDeserializerWorkflow[JObject](obj, Try(ReductionResultJObject(obj)), ReductionResultJObject.TYPE_NAME)
    case obj: JObject if ReductionResultByteString.isReductionResultByteString(obj) =>
      reductionResultDeserializerWorkflow[ByteString](obj, Try(ReductionResultByteString(obj)), ReductionResultByteString.TYPE_NAME)
    case obj: JObject if OriginalResultJValue.isOriginalJValueResult(obj) =>
      reductionResultDeserializerWorkflow[JValue](obj, Try(OriginalResultJValue[JValue](obj)), OriginalResultJValue.TYPE_NAME)
    case obj: JObject if OriginalResult.isOriginalResult(obj) =>
      reductionResultDeserializerWorkflow[T](obj, Try(OriginalResult[T](obj)), OriginalResult.TYPE_NAME)
  }


  private[this] def reductionResultDeserializerWorkflow[T: TypeTag : Manifest](obj: JObject,
                                                                               reductionResult: Try[ReductionResult[T]],
                                                                               `type`: String)
                                                                              (implicit formats: Formats): ReductionResult[T] = {
    Logger.trace(DeserializingReductionResult(obj, `type`))
    reductionResult match {
      case Failure(e) => Logger.warn(ErrorDuringReductionResultDeserialization(e, obj, `type`)); throw e
      case Success(reductionResult) => Logger.trace(DeserializedReductionResult(reductionResult, obj, `type`)); reductionResult
    }
  }
}