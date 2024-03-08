package io.simplifier.plugin.contentrepo.definitions.exceptions

import akka.http.scaladsl.model.StatusCodes._
import io.simplifier.plugin.contentrepo.controller.BaseController.OperationFailureMessage

object MimeMappingExceptions {

  def MimeMappingAlreadyExisting = OperationFailureMessage("Mapping for extension already existing", Conflict)
  def MimeMappingEmptyExtension = OperationFailureMessage("Extension must not be empty", UnprocessableEntity)
  def MimeMappingEmptyMime = OperationFailureMessage("MimeType must not be empty", UnprocessableEntity)
  def MimeMappingNotFound = OperationFailureMessage("Mapping for extension not found", UnprocessableEntity)
  def MimeMappingRetrievalFailure(list: Boolean = false) = OperationFailureMessage(s"Mapping${if (list) "s" else ""} could not be retrieved from data base", InternalServerError)

}
