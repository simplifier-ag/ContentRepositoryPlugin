package io.simplifier.plugin.contentrepo.pluginBaseRelated.reducer.reductionResults

import io.simplifier.pluginbase.util.json.NamedTupleAccess._
import akka.util.ByteString
import io.simplifier.plugin.contentrepo.pluginBaseRelated.reducer.reductionChanges.ReductionChange
import io.simplifier.plugin.contentrepo.pluginBaseRelated.reducer.{DEFAULT_SIZE, ReductionChanges, ReductionResult}
import io.simplifier.pluginbase.util.json.SimplifierFormats
import io.simplifier.pluginbase.util.xcoder.ByteArrayEncoding
import org.json4s._


/**
 * The ByteString Reduction Result.
 *
 * @note the reduced value may be null.
 * @param originalByteString the original byte string.
 * @param reducedByteString  the reduced byte string.
 */
case class ReductionResultByteString(originalByteString: ByteString,
                                                      reducedByteString: ByteString) extends ReductionResult[ByteString] {
  override protected implicit val formats: Formats = SimplifierFormats.formats.withPre36DeserializationBehavior

  override val `type`: String = ReductionResultByteString.TYPE_NAME
  override val wasReduced: Boolean = true
  override val reducedValue: ByteString = originalByteString

  override val originalSize: Int = {
    if (originalByteString == null) DEFAULT_SIZE else originalByteString.size
  }

  override val reducedSize: Int = {
    if (originalByteString == null) DEFAULT_SIZE else reducedByteString.size
  }

  override val changes: ReductionChanges = ReductionChanges(Seq.empty[ReductionChange])


  override def getOriginalValueAsJValue(implicit formats: Formats): JValue = {
    if (originalByteString == null) JNothing else Hex.encode(originalByteString.toArray).map(_.toJson).get
  }

  override def getReducedValueAsJValue(implicit formats: Formats): JValue = {
    if (reducedByteString == null) JNothing else Hex.encode(reducedByteString.toArray).map(_.toJson).get
  }

  override def toString: String = createString(wasReduced, originalSize, reducedSize, `type`, None, None)
}


/** The ByteString Reduction Result Companion Object */
object ReductionResultByteString extends ByteArrayEncoding {


  /**
   * Constructs a new [[ReductionResultByteString]].
   *
   * @param value the Json Value to construct the [[ReductionResultByteString]] from.
   * @return the new [[ReductionResultByteString]].
   */
  def apply(value: JValue): ReductionResultByteString = {
    val originalByteString: ByteString = value match {
      case JObject(fields) => fields.collectFirst {
        case JField(name, JArray(values)) if name == ReductionResult.FIELD_NAME_ORIGINAL_VALUE => Hex.encode(values.toArray).flatMap(_.toSigned).map(_.toByteString).get
        case JField(name, JArray(values)) if name == FIELD_NAME_ORIGINAL_BYTE_STRING => Hex.encode(values.toArray).flatMap(_.toSigned).map(_.toByteString).get
      }.orNull
      case _ => null
    }

    val reducedByteString: ByteString = value match {
      case JObject(fields) => fields.collectFirst {
        case JField(name, JArray(values)) if name == ReductionResult.FIELD_NAME_REDUCED_VALUE => Hex.encode(values.toArray).flatMap(_.toSigned).map(_.toByteString).get
        case JField(name, JArray(values)) if name == FIELD_NAME_REDUCED_BYTE_STRING => Hex.encode(values.toArray).flatMap(_.toSigned).map(_.toByteString).get
      }.orNull
      case _ => null
    }

    new ReductionResultByteString(
      originalByteString = originalByteString,
      reducedByteString = reducedByteString
    )
  }


  private[this] val FIELD_NAME_ORIGINAL_BYTE_STRING: String = "originalByteString"
  private[this] val FIELD_NAME_REDUCED_BYTE_STRING: String = "reducedByteString"

  val TYPE_NAME: String = "Byte String"


   def isReductionResultByteString(value: JValue): Boolean = {
    implicit val formats: Formats = DefaultFormats.lossless.withPre36DeserializationBehavior +
      SimplifierFormats.ByteStringSerializer


    def reductionResultByteStringWorkflow(value: JValue): Boolean = {
      value match {
        case JObject(fields) => fields.contains(JField(ReductionResult.FIELD_NAME_TYPE, JString(TYPE_NAME))) ||
          (fields.exists(field => field.name == FIELD_NAME_ORIGINAL_BYTE_STRING) &&
            fields.exists(field => field.name == FIELD_NAME_REDUCED_BYTE_STRING))
        case _ => false
      }
    }

    ReductionResult.reductionResultCheckWorkflow[ReductionResultString](value, reductionResultByteStringWorkflow, TYPE_NAME)
  }
}