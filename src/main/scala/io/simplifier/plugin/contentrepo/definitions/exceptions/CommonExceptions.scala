package io.simplifier.plugin.contentrepo.definitions.exceptions

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.model.StatusCodes.{InternalServerError, UnprocessableEntity}
import io.simplifier.plugin.contentrepo.controller.BaseController.OperationFailureMessage

object CommonExceptions {
  def CommonInvalidCharacter(reason: String, kind: String) = OperationFailureMessage(s"$reason in $kind name", UnprocessableEntity)
  def CommonUnexpectedError(msg: String) = OperationFailureMessage(s"Failed unexpectedly: $msg", InternalServerError)
  def CommonMissingParametersException(msg: String) = OperationFailureMessage("Missing Parameters " + msg, StatusCodes.BadRequest)
  def CommonInvalidPath = OperationFailureMessage("Invalid path", UnprocessableEntity)
}