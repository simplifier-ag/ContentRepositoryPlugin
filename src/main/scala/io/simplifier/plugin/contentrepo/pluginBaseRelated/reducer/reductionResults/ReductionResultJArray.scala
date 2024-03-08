package io.simplifier.plugin.contentrepo.pluginBaseRelated.reducer.reductionResults

import io.simplifier.pluginbase.util.json.NamedTupleAccess._
import io.simplifier.plugin.contentrepo.pluginBaseRelated.reducer.ReducibleJSONDataTypes.{JsonArrayReducibleType, JsonByteArrayEquivalentReducibleType}
import io.simplifier.plugin.contentrepo.pluginBaseRelated.reducer.ReductionResult._
import io.simplifier.plugin.contentrepo.pluginBaseRelated.reducer._
import io.simplifier.plugin.contentrepo.pluginBaseRelated.reducer.reductionResults.ReductionResultJArray.FIELD_NAME_ARRAY_CHANGES
import io.simplifier.pluginbase.util.json.SimplifierFormats
import org.json4s._


/**
 * The Json Array Reduction Result.
 *
 * @note the reduced value may be null.
 * @param originalArray the original Json Array.
 * @param reducedArray  the reduced Json Array.
 * @param changes       the occurred changes.
 *
 */
case class ReductionResultJArray(originalArray: JArray,
                                                  reducedArray: JArray,
                                                  changes: ReductionChanges) extends ReductionResult[JArray] {
  override protected implicit val formats: Formats = SimplifierFormats.formats.withPre36DeserializationBehavior

  override val `type`: String = ReductionResultJArray.TYPE_NAME
  override val wasReduced: Boolean = {
    reducedArray match {
      case null => false
      case _ if originalArray == null => true
      case _ if isByteArrayEquivalent(reducedArray.arr) && reducedArray.values.size < originalArray.values.size => true
      case _ => determineSerializedLength(reducedArray) < determineSerializedLength(originalArray) || reducedArray.values.size < originalArray.values.size
    }
  }
  override val reducedValue: JArray = reducedArray

  override val originalSize: Int = {
    originalArray match {
      case null => DEFAULT_SIZE
      case JArray(values) if isByteArrayEquivalent(values) => values.size
      case _ => determineSerializedLength(originalArray)
    }
  }

  override val reducedSize: Int = {
    reducedArray match {
      case null => DEFAULT_SIZE
      case JArray(values) if isByteArrayEquivalent(values) => values.size
      case _ => determineSerializedLength(reducedArray)
    }
  }

  override def getOriginalValueAsJValue(implicit formats: Formats): JValue = {
    if (originalArray == null) JNothing else originalArray
  }

  override def getReducedValueAsJValue(implicit formats: Formats): JValue = {
    if (reducedArray == null) JNothing else reducedArray
  }

  override def toJson(jsonFormats: Option[Formats]): JObject = {
    JObject(
      (super.toJson(jsonFormats).obj :+ JField(FIELD_NAME_ARRAY_CHANGES, changes.toJson))
        .filterNot(_.name == FIELD_NAME_CHANGES)
    )
  }

  override def toReducedJson(omitReducedValue: Boolean,
                             jsonFormats: Option[Formats]): JObject = {
    JObject(
      (super.toReducedJson(omitReducedValue, jsonFormats).obj :+ JField(FIELD_NAME_ARRAY_CHANGES, changes.toJson))
        .filterNot(_.name == FIELD_NAME_CHANGES)
    )
  }

  override def toString: String = createString(wasReduced, originalSize, reducedSize, `type`, Some(changes), Some(determineReducibleJsonArrayType))

  override protected def determineSizeString(size: Int,
                                             value: JValue): JValue = {
    size match {
      case DEFAULT_SIZE => JNothing
      case _ => value match {
        case JArray(values) if isByteArrayEquivalent(values) => JString(s"${LENGTH_FORMATTER.format(size)} [Byte]")
        case _ => JString(s"${LENGTH_FORMATTER.format(size)} [Byte] (Compactly Rendered JSON-Array Size)")
      }
    }
  }


  private[this] def determineReducibleJsonArrayType: ReducibleJSONDataTypes = {
    reducedArray match {
      case JArray(values) if isByteArrayEquivalent(values) => JsonByteArrayEquivalentReducibleType()
      case JArray(_) => JsonArrayReducibleType()
    }
  }
}


/** The Json Array Reduction Result Companion Object */
object ReductionResultJArray {


  /**
   * Constructs a new [[ReductionResultJArray]].
   *
   * @param value the Json Value to construct the [[ReductionResultJArray]] from.
   * @return the new [[ReductionResultJArray]].
   */
  def apply(value: JValue): ReductionResultJArray = {
    implicit val formats: Formats = DefaultFormats.lossless.withPre36DeserializationBehavior +
      ReductionChanges.ReductionChangesSerializer

    val originalJArray: JArray = value match {
      case JObject(fields) => fields.collectFirst {
        case JField(name, array: JArray) if name == ReductionResult.FIELD_NAME_ORIGINAL_VALUE => array
        case JField(name, array: JArray) if name == FIELD_NAME_ORIGINAL_ARRAY => array
      }.orNull
      case _ => null
    }

    val reducedJArray: JArray = value match {
      case JObject(fields) => fields.collectFirst {
        case JField(name, array: JArray) if name == ReductionResult.FIELD_NAME_REDUCED_VALUE => array
        case JField(name, array: JArray) if name == FIELD_NAME_REDUCED_ARRAY => array
      }.orNull
      case _ => null
    }

    val reductionChanges: ReductionChanges = value match {
      case JObject(fields) => fields.collectFirst {
        case JField(name, v) if name == FIELD_NAME_ARRAY_CHANGES => Extraction.extract[ReductionChanges](v)
        case JField(name, v) if name == FIELD_NAME_CHANGES => Extraction.extract[ReductionChanges](v)
      }.getOrElse(ReductionChanges())
      case _ => ReductionChanges()
    }

    new ReductionResultJArray(
      originalArray = originalJArray,
      reducedArray = reducedJArray,
      changes = reductionChanges
    )
  }


  val FIELD_NAME_ARRAY_CHANGES: String = "Array Changes"

  private[this] val FIELD_NAME_ORIGINAL_ARRAY: String = "originalArray"
  private[this] val FIELD_NAME_REDUCED_ARRAY: String = "reducedArray"
  private[this] val FIELD_NAME_CHANGES: String = "changes"

  val TYPE_NAME: String = "Json Array"


  def isReductionResultJArray(value: JValue): Boolean = {
    implicit val formats: Formats = DefaultFormats.lossless.withPre36DeserializationBehavior

    def reductionResultJArrayWorkflow(value: JValue): Boolean = {
      value match {
        case JObject(fields) => fields.contains(JField(ReductionResult.FIELD_NAME_TYPE, JString(TYPE_NAME))) ||
          (fields.exists(field => field.name == FIELD_NAME_ORIGINAL_ARRAY) &&
            fields.exists(field => field.name == FIELD_NAME_REDUCED_ARRAY))
        case _ => false
      }
    }

    ReductionResult.reductionResultCheckWorkflow[ReductionResultJArray](value, reductionResultJArrayWorkflow, TYPE_NAME)
  }
}



