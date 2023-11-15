package io.simplifier.plugin.contentrepo.pluginBaseRelated.reducer.reductionResults

import io.simplifier.pluginbase.util.json.NamedTupleAccess._
import io.simplifier.plugin.contentrepo.pluginBaseRelated.reducer.ReducibleJSONDataTypes.JsonObjectReducibleType
import io.simplifier.plugin.contentrepo.pluginBaseRelated.reducer.ReductionResult.FIELD_NAME_CHANGES
import io.simplifier.plugin.contentrepo.pluginBaseRelated.reducer._
import io.simplifier.plugin.contentrepo.pluginBaseRelated.reducer.reductionResults.ReductionResultJObject.FIELD_NAME_OBJECT_CHANGES
import io.simplifier.pluginbase.util.json.SimplifierFormats
import org.json4s._


/**
 * The Json Object Reduction Result.
 *
 * @note the reduced value may be null.
 * @param originalObject the original Json Object.
 * @param reducedObject  the reduced Json Object.
 * @param changes        the occurred changes.
 *
 */
 case class ReductionResultJObject(originalObject: JObject,
                                                   reducedObject: JObject,
                                                   changes: ReductionChanges) extends ReductionResult[JObject] {
  override protected implicit val formats: Formats = SimplifierFormats.formats.withPre36DeserializationBehavior

  override val `type`: String = ReductionResultJObject.TYPE_NAME
  override val wasReduced: Boolean = {
    reducedObject match {
      case null => false
      case _ if originalObject == null => true
      case _ => determineSerializedLength(reducedObject) < determineSerializedLength(originalObject) || reducedObject.obj.size < originalObject.obj.size
    }
  }
  override val reducedValue: JObject = reducedObject
  override val originalSize: Int = if (originalObject == null) DEFAULT_SIZE else determineSerializedLength(originalObject)
  override val reducedSize: Int = if (reducedObject == null) DEFAULT_SIZE else determineSerializedLength(reducedObject)

  override def getOriginalValueAsJValue(implicit formats: Formats): JValue = {
    if (originalObject == null) JNothing else originalObject
  }

  override def getReducedValueAsJValue(implicit formats: Formats): JValue = {
    if (reducedObject == null) JNothing else reducedObject
  }

  override def toJson(jsonFormats: Option[Formats]): JObject = {
    JObject(
      (super.toJson(jsonFormats).obj :+ JField(FIELD_NAME_OBJECT_CHANGES, changes.toJson))
        .filterNot(_.name == FIELD_NAME_CHANGES)
    )
  }

  override def toReducedJson(omitReducedValue: Boolean,
                             jsonFormats: Option[Formats]): JObject = {
    JObject(
      (super.toReducedJson(omitReducedValue, jsonFormats).obj :+ JField(FIELD_NAME_OBJECT_CHANGES, changes.toJson))
        .filterNot(_.name == FIELD_NAME_CHANGES)
    )
  }

  override def toString: String = createString(wasReduced, originalSize, reducedSize, `type`, Some(changes), Some(JsonObjectReducibleType()))

  override protected def determineSizeString(size: Int,
                                             value: JValue): JValue = {
    size match {
      case DEFAULT_SIZE => JNothing
      case _ => JString(s"${LENGTH_FORMATTER.format(size)} [Byte] (Compactly Rendered JSON-Object Size)")
    }
  }
}


/** The Json Object Reduction Result Companion Object */
 object ReductionResultJObject {

  /**
   * Constructs a new [[ReductionResultJObject]].
   *
   * @param value the Json Value to construct the [[ReductionResultJObject]] from.
   * @return the new [[ReductionResultJObject]].
   */
  def apply(value: JValue): ReductionResultJObject = {
    implicit val formats: Formats = DefaultFormats.lossless.withPre36DeserializationBehavior +
      ReductionChanges.ReductionChangesSerializer

    val originalObject: JObject = value match {
      case JObject(fields) => fields.collectFirst {
        case JField(name, obj: JObject) if name == ReductionResult.FIELD_NAME_ORIGINAL_VALUE => obj
        case JField(name, obj: JObject) if name == FIELD_NAME_ORIGINAL_OBJECT => obj
      }.orNull
      case _ => null
    }

    val reducedObject: JObject = value match {
      case JObject(fields) => fields.collectFirst {
        case JField(name, obj: JObject) if name == ReductionResult.FIELD_NAME_REDUCED_VALUE => obj
        case JField(name, obj: JObject) if name == FIELD_NAME_REDUCED_OBJECT => obj
      }.orNull
      case _ => null
    }

    val reductionChanges: ReductionChanges = value match {
      case JObject(fields) => fields.collectFirst {
        case JField(name, v) if name == FIELD_NAME_OBJECT_CHANGES => Extraction.extract[ReductionChanges](v)
        case JField(name, v) if name == FIELD_NAME_CHANGES => Extraction.extract[ReductionChanges](v)
      }.getOrElse(ReductionChanges())
      case _ => ReductionChanges()
    }

    new ReductionResultJObject(
      originalObject = originalObject,
      reducedObject = reducedObject,
      changes = reductionChanges
    )
  }


  private[this] val FIELD_NAME_ORIGINAL_OBJECT: String = "originalObject"
  private[this] val FIELD_NAME_REDUCED_OBJECT: String = "reducedObject"
  private[this] val FIELD_NAME_CHANGES: String = "changes"

  private[reducer] val FIELD_NAME_OBJECT_CHANGES: String = "Object Changes"

  val TYPE_NAME: String = "Json Object"


   def isReductionResultJObject(value: JValue): Boolean = {
    implicit val formats: Formats = DefaultFormats.lossless.withPre36DeserializationBehavior

    def reductionResultJObjectWorkflow(value: JValue): Boolean = {
      value match {
        case JObject(fields) => fields.contains(JField(ReductionResult.FIELD_NAME_TYPE, JString(TYPE_NAME))) ||
          (fields.exists(field => field.name == FIELD_NAME_ORIGINAL_OBJECT) &&
            fields.exists(field => field.name == FIELD_NAME_REDUCED_OBJECT))
        case _ => false
      }
    }

    ReductionResult.reductionResultCheckWorkflow[ReductionResultJObject](value, reductionResultJObjectWorkflow, TYPE_NAME)
  }
}


