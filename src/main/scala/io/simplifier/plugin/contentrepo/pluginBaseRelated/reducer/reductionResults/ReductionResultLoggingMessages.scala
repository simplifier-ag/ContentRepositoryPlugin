package io.simplifier.plugin.contentrepo.pluginBaseRelated.reducer.reductionResults

import io.simplifier.plugin.contentrepo.pluginBaseRelated.reducer.ReductionResult
import io.simplifier.pluginbase.util.json.JSONFormatter.renderJSONCompact
import io.simplifier.pluginbase.util.logging.ExceptionFormatting._
import org.json4s.{Formats, JNothing, JObject, JValue}


/**
 * This trait provides methods for log messages for serializers/deserializers and so on.
 */
trait ReductionResultLoggingMessages {


  /**
   * The log when the provided [[JValue]] is checked against the desired  [[ReductionResult]] type.
   *
   * @param value  the provided [[JValue]].
   * @param `type` the reduction result type.
   * @return the log message.
   */
  def CheckingWhetherProvidedJsonEqualsDesiredReductionResultType(value: JValue,
                                                                  `type`: String): String = {
    val providedJsonString: String = renderJSONCompact(value)
    s"Checking whether the provided Json: [$providedJsonString] equals the desired reduction result type: [${`type`}]."
  }


  /**
   * The log when the provided [[JValue]] was successfully checked against the desired [[ReductionResult]] type.
   *
   * @param value  the provided [[JValue]].
   * @param equals the result
   * @param `type` the reduction result type.
   * @return the log message.
   */
  def CheckedWhetherProvidedJsonEqualsDesiredReductionResultType(value: JValue,
                                                                 equals: Boolean,
                                                                 `type`: String): String = {
    val providedJsonString: String = renderJSONCompact(value)
    s"Checked whether the provided Json: [$providedJsonString] was equal to the desired reduction result type: [${`type`}] successfully. " +
      s"The provided Json ${if (equals) s"was the desired" else s"was not the desired"} type."
  }


  /**
   * The log when the provided [[JValue]] could not be checked against the desired [[ReductionResult]].
   *
   * @param error  the occurred error.
   * @param obj    the provided [[JValue]].
   * @param `type` the reduction result type.
   * @return the log message.
   */
  def ErrorDuringReductionResultTypeCheck(error: Throwable,
                                          value: JValue,
                                          `type`: String): String = {
    val rootCauseMessage: String = getCauseMessageSafelyWithFallback(error)
    val rootCauseType: String = getCauseTypeSafelyWithFallback(error)
    val providedJsonString: String = renderJSONCompact(value)

    s"The provided Json: [$providedJsonString] could not have been checked to be of the desired reduction result type: [${`type`}] " +
      s"due to an error of the type: [$rootCauseType] with the message: [$rootCauseMessage]."
  }


  /**
   * The log when the [[ReductionResult]] is deserialized from provided [[JObject]].
   *
   * @param obj    the provided [[JObject]].
   * @param `type` the reduction result type.
   * @return the log message.
   */
  def DeserializingReductionResult(obj: JObject,
                                   `type`: String): String = {
    val providedJsonString: String = renderJSONCompact(obj)
    s"Trying to build the reduction result: [${`type`}] from the provided provided Json-Object: [$providedJsonString]."
  }


  /**
   * The log when the desired [[ReductionResult]] was deserialized from the provided [[JObject]] successfully.
   *
   * @param extractedResult the extracted reduction result.
   * @param obj             the provided [[JObject]].
   * @param formats         the Json-Formats.
   * @param `type`          the reduction result type.
   * @return the log message.
   */
  def DeserializedReductionResult(extractedResult: ReductionResult[_],
                                  obj: JObject,
                                  `type`: String)
                                 (implicit formats: Formats): String = {
    val providedJsonString: String = renderJSONCompact(obj)
    val resultJson: String = renderJSONCompact(Option(extractedResult)
      .map(_.toJson(Some(formats)))
      .getOrElse(JNothing))


    s"The ${`type`} was successfully deserialized from the provided Json-Object: [$providedJsonString] with the result: [$resultJson]."
  }


  /**
   * The log when the provided [[JObject]] could not be deserialized into a [[ReductionResult]].
   *
   * @param error  the occurred error.
   * @param obj    the provided [[JObject]].
   * @param `type` the reduction result type.
   * @return the log message.
   */
  def ErrorDuringReductionResultDeserialization(error: Throwable,
                                                obj: JObject,
                                                `type`: String): String = {
    val rootCauseMessage: String = getCauseMessageSafelyWithFallback(error)
    val rootCauseType: String = getCauseTypeSafelyWithFallback(error)
    val providedJsonString: String = renderJSONCompact(obj)
    s"The ${`type`} could have not ben extracted due to an error of the type: [$rootCauseType] " +
      s"with the message: [$rootCauseMessage] from the provided provided Json-Object: [$providedJsonString]."
  }


}
