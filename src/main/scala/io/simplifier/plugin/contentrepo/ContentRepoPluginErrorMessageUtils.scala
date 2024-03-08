package io.simplifier.plugin.contentrepo

import io.simplifier.plugin.contentrepo.controller.BaseController.OperationFailure
import io.simplifier.plugin.contentrepo.dto.RestMessages.RestMessage
import io.simplifier.plugin.contentrepo.pluginBaseRelated.reducer.SizeReducer
import com.typesafe.config.{Config, ConfigFactory}
import io.simplifier.pluginbase.util.logging.ExceptionFormatting
import org.slf4j.Logger

import scala.util.{Failure, Try}

trait ContentRepoPluginErrorMessageUtils extends SizeReducer {

  import ContentRepoPluginErrorMessageUtils._


  /** The external Logger to use for Logging */
  val Logger: Logger

  /**
    * Reduces the message text for a provided operation failure.
    *
    * @param failure       the operation failure to shorten the message text from.
    * @param messageLength the maximum message length that may be stay before being shortened.
    * @return the operation failure with the shortened message text.
    */
  def shortenOperationFailureMessageTexts(failure: OperationFailure,
                                          messageLength: Int): OperationFailure = {
    Option(failure) match {
      case None => DEFAULT_OPERATION_FAILURE
      case Some(f) => f.copy(
        msg = f.msg.copy(
          msgText = shortenMessageText(f.msg.msgText, messageLength)
        )
      )
    }
  }


  /**
    * Reduces the message text for a provided string.
    *
    * @param message       the message text.
    * @param messageLength the maximum message length that may be stay before being shortened.
    * @return the shortened message text.
    */
  def shortenMessageText(message: String,
                         messageLength: Int): String = {
    Option(message) match {
      case None => DEFAULT_MESSAGE
      case Some(msg) => shortenString(msg, messageLength)
    }
  }


  /**
    * Determines the maximum possible message length.
    *
    * @param config               the configuration reference.
    * @param defaultMessageLength the default maximum possible message length should no length be provided via configuration.
    * @param path                 the path in the configuration to the regular expression.
    * @return the maximum possible message length for each message.
    */
  def determineMaximumMessageLength(config: Config,
                                    defaultMessageLength: Int,
                                    path: String): Int = {
    val sanitizedConfig: Config = Option(config).getOrElse(ConfigFactory.empty())

    path match {
      case null | "" => defaultMessageLength
      case p if !sanitizedConfig.hasPath(p) => defaultMessageLength
      case p => Try(sanitizedConfig.getInt(p))
        .recoverWith {
          case e => Logger.warn(ConfigurationValueCouldNotBeFound[Int](e, path, TYPE_MAXIMUM_MESSAGE_LENGTH, EXPECTED_DATATYPE_INTEGER, defaultMessageLength))
            Failure(e)
        }.getOrElse(defaultMessageLength)
    }
  }


  private[this] def shortenString(string: String,
                                  messageLength: Int): String = {
    shortenString(messageLength, reduce = true, None, string)
      .map(_.reducedValue)
      .recoverWith { case e =>
        Logger.warn(MessageCouldNotBeShortened(e, string))
        Failure(e)
      }.getOrElse(string)
  }
}


/** The Content Repo Plugin Error Message Utils Companion Object */
object ContentRepoPluginErrorMessageUtils extends ExceptionFormatting {


  private[ContentRepoPluginErrorMessageUtils] val DEFAULT_MESSAGE: String = ""
  private[ContentRepoPluginErrorMessageUtils] val DEFAULT_OPERATION_FAILURE: OperationFailure = OperationFailure(
    RestMessage(
      msgId = "",
      msgType = "Internal Error",
      msgText = "Received null instead of operation failure"
    )
  )

  private[ContentRepoPluginErrorMessageUtils] val EXPECTED_DATATYPE_INTEGER: String = "Integer"
  private[ContentRepoPluginErrorMessageUtils] val TYPE_MAXIMUM_MESSAGE_LENGTH: String = "message length"


  private[ContentRepoPluginErrorMessageUtils] def ConfigurationValueCouldNotBeFound[T](error: Throwable,
                                                                                       path: String,
                                                                                       `type`: String,
                                                                                       expectedDataType: String,
                                                                                       defaultValue: T): String = {
    val rootCauseType: String = getCauseTypeSafelyWithFallback(error, classNameType = CanonicalClassNames)
    val rootCauseMessage: String = getCauseMessageSafelyWithFallback(error)

    s"Could not find a value of the expected data type: [$expectedDataType] for the ${`type`} for path: [$path] " +
      s"due to an error of the type: [$rootCauseType] with the message: [$rootCauseMessage]. " +
      s"Using the provided default value for the ${`type`}: [$defaultValue] instead."
  }


  private[ContentRepoPluginErrorMessageUtils] def MessageCouldNotBeShortened(error: Throwable,
                                                                             message: String): String = {
    val rootCauseType: String = getCauseTypeSafelyWithFallback(error, classNameType = CanonicalClassNames)
    val rootCauseMessage: String = getCauseMessageSafelyWithFallback(error)

    s"Could not replace message: [$message] due to an error of the type: [$rootCauseType] " +
      s"with the message: [$rootCauseMessage]. Using the original message instead."
  }

}