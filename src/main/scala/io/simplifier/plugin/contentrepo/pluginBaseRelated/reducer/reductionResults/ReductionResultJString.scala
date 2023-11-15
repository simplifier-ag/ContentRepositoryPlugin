package io.simplifier.plugin.contentrepo.pluginBaseRelated.reducer.reductionResults

import io.simplifier.pluginbase.util.json.NamedTupleAccess._
import io.simplifier.plugin.contentrepo.pluginBaseRelated.reducer.ReducibleJSONDataTypes.JsonStringReducibleType
import io.simplifier.plugin.contentrepo.pluginBaseRelated.reducer.reductionChanges.ReductionChange
import io.simplifier.plugin.contentrepo.pluginBaseRelated.reducer.reductionResults.ReductionResultString.calculateReducedStringLength
import io.simplifier.plugin.contentrepo.pluginBaseRelated.reducer.{DEFAULT_SIZE, ReductionChanges, ReductionResult}
import io.simplifier.pluginbase.util.json.SimplifierFormats
import org.json4s._


/**
 * The Json Reduction Result.
 *
 * @note the reduced value may be null.
 * @param originalJString the original Json String.
 * @param reducedJString  the reduced Json String.
 */
 case class ReductionResultJString(originalJString: JString,
                                                   reducedJString: JString) extends ReductionResult[JString] {
  override protected implicit val formats: Formats = SimplifierFormats.formats.withPre36DeserializationBehavior

  override val `type`: String = ReductionResultJString.TYPE_NAME
  override val wasReduced: Boolean = true
  override val reducedValue: JString = reducedJString

  override val originalSize: Int = {
    if (originalJString == null) DEFAULT_SIZE else originalJString.s.length
  }

  override val reducedSize: Int = {
    if (reducedJString == null) DEFAULT_SIZE else calculateReducedStringLength(reducedJString.s)
  }

  override val changes: ReductionChanges = ReductionChanges(Seq.empty[ReductionChange])


  override def getOriginalValueAsJValue(implicit formats: Formats): JValue = {
    if (originalJString == null) JNothing else originalJString
  }

  override def getReducedValueAsJValue(implicit formats: Formats): JValue = {
    if (reducedJString == null) JNothing else reducedJString
  }

  override def toString: String = createString(wasReduced, originalSize, reducedSize, `type`, None, Some(JsonStringReducibleType()))
}


/** The Json String Reduction Result Companion Object */
 object ReductionResultJString {


  /**
   * Constructs a new [[ReductionResultJString]].
   *
   * @param value the Json Value to construct the [[ReductionResultJString]] from.
   * @return the new [[ReductionResultJString]].
   */
  def apply(value: JValue): ReductionResultJString = {
    val originalString: JString = value match {
      case JObject(fields) => fields.collectFirst {
        case JField(name, string: JString) if name == ReductionResult.FIELD_NAME_ORIGINAL_VALUE => string
        case JField(name, string: JString) if name == FIELD_NAME_ORIGINAL_JSTRING => string
      }.orNull
      case _ => null
    }

    val reducedString: JString = value match {
      case JObject(fields) => fields.collectFirst {
        case JField(name, obj: JString) if name == ReductionResult.FIELD_NAME_REDUCED_VALUE => obj
        case JField(name, obj: JString) if name == FIELD_NAME_REDUCED_JSTRING => obj
      }.orNull
      case _ => null
    }


    new ReductionResultJString(
      originalJString = originalString,
      reducedJString = reducedString
    )
  }


  private[this] val FIELD_NAME_ORIGINAL_JSTRING: String = "originalJString"
  private[this] val FIELD_NAME_REDUCED_JSTRING: String = "reducedJString"

  val TYPE_NAME: String = "Json String"


   def isReductionResultJString(value: JValue): Boolean = {
    implicit val formats: Formats = DefaultFormats.lossless.withPre36DeserializationBehavior


    def reductionResultJStringWorkflow(value: JValue): Boolean = {
      value match {
        case JObject(fields) => fields.contains(JField(ReductionResult.FIELD_NAME_TYPE, JString(TYPE_NAME))) ||
          (fields.exists(field => field.name == FIELD_NAME_ORIGINAL_JSTRING)
            && fields.exists(field => field.name == FIELD_NAME_REDUCED_JSTRING))
        case _ => false
      }
    }

    ReductionResult.reductionResultCheckWorkflow[ReductionResultJString](value, reductionResultJStringWorkflow, TYPE_NAME)
  }
}



