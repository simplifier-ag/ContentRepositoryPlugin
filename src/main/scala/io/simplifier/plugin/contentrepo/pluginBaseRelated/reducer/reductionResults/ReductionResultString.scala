package io.simplifier.plugin.contentrepo.pluginBaseRelated.reducer.reductionResults

import io.simplifier.pluginbase.util.json.NamedTupleAccess._
import io.simplifier.plugin.contentrepo.pluginBaseRelated.reducer.reductionChanges.ReductionChange
import io.simplifier.plugin.contentrepo.pluginBaseRelated.reducer.reductionResults.ReductionResultString.{TYPE_NAME, calculateReducedStringLength}
import io.simplifier.plugin.contentrepo.pluginBaseRelated.reducer.{DEFAULT_SIZE, ReductionChanges, ReductionResult}
import io.simplifier.pluginbase.util.json.SimplifierFormats
import org.json4s._

import scala.util.matching.Regex


/**
 * The String Reduction Result.
 *
 * @note the reduced value may be null.
 * @param originalString the original String.
 * @param reducedString  the reduced String.
 */
 case class ReductionResultString(originalString: String,
                                                  reducedString: String) extends ReductionResult[String] {
  override protected implicit val formats: Formats = SimplifierFormats.formats.withPre36DeserializationBehavior

  override val `type`: String = TYPE_NAME
  override val wasReduced: Boolean = true
  override val reducedValue: String = reducedString

  override val originalSize: Int = {
    if (originalString == null) DEFAULT_SIZE else originalString.length
  }

  override val reducedSize: Int = {
    if (reducedString == null) DEFAULT_SIZE else calculateReducedStringLength(reducedString)
  }

  override val changes: ReductionChanges = ReductionChanges(Seq.empty[ReductionChange])


  override def getOriginalValueAsJValue(implicit formats: Formats): JValue = {
    if (originalString == null) JNothing else JString(originalString)
  }

  override def getReducedValueAsJValue(implicit formats: Formats): JValue = {
    if (reducedString == null) JNothing else JString(reducedString)
  }

  override def toString: String = createString(wasReduced, originalSize, reducedSize, `type`, None, None)
}


/** The String Reduction Result Companion Object */
 object ReductionResultString {

  /**
   * Constructs a new [[ReductionResultString]].
   *
   * @param value the Json Value to construct the [[ReductionResultString]] from.
   * @return the new [[ReductionResultString]].
   */
  def apply(value: JValue): ReductionResultString = { 
    val originalString: String = value match {
      case JObject(fields) => fields.collectFirst {
        case JField(name, JString(string)) if name == ReductionResult.FIELD_NAME_ORIGINAL_VALUE => string
        case JField(name, JString(string)) if name == FIELD_NAME_ORIGINAL_STRING => string
      }.orNull
      case _ => null
    }

    val reducedString: String = value match {
      case JObject(fields) => fields.collectFirst {
        case JField(name, JString(string)) if name == ReductionResult.FIELD_NAME_REDUCED_VALUE => string
        case JField(name, JString(string)) if name == FIELD_NAME_REDUCED_STRING => string
      }.orNull
      case _ => null
    }


    new ReductionResultString(
      originalString = originalString,
      reducedString = reducedString
    )
  }

  private[this] val FIELD_NAME_ORIGINAL_STRING: String = "originalString"
  private[this] val FIELD_NAME_REDUCED_STRING: String = "reducedString"

  val TYPE_NAME: String = "String"


  val SHORTENED_STRING_DETERMINER_REGEX: Regex = "(?s)( \\(... shortened (?:\\d+(?:\\.\\d+)*)+ of (?:\\d+(?:\\.\\d+)*)+ \\[Byte\\]\\))".r.unanchored


   def isReductionResultString(value: JValue): Boolean = {
    implicit val formats: Formats = DefaultFormats.lossless.withPre36DeserializationBehavior

    def reductionResultStringWorkflow(value: JValue): Boolean = {
      value match {
        case JObject(fields) => fields.contains(JField(ReductionResult.FIELD_NAME_TYPE, JString(TYPE_NAME))) ||
          (fields.exists(field => field.name == FIELD_NAME_ORIGINAL_STRING) &&
            fields.exists(field => field.name == FIELD_NAME_REDUCED_STRING))
        case _ => false
      }
    }

    ReductionResult.reductionResultCheckWorkflow[ReductionResultString](value, reductionResultStringWorkflow, TYPE_NAME)
  }


   def calculateReducedStringLength(string: String): Int = {
    string match {
      //The determiner length will be reduced
      case SHORTENED_STRING_DETERMINER_REGEX(determiner) if string.endsWith(determiner) => string.length - determiner.length
      case _ => string.length
    }
  }
}