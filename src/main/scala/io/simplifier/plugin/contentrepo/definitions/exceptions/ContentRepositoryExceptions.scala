package io.simplifier.plugin.contentrepo.definitions.exceptions

import akka.http.scaladsl.model.StatusCodes.{Conflict, InternalServerError, NotFound, UnprocessableEntity, BadRequest}
import io.simplifier.plugin.contentrepo.controller.BaseController.OperationFailureMessage

object ContentRepositoryExceptions {

  def ContentRepoEmptyPermissionObjectType = OperationFailureMessage("PermissionObjectType must not be empty", UnprocessableEntity)
  def ContentRepoEmptyProvider = OperationFailureMessage("Content provider must not be empty", UnprocessableEntity)
  def ContentRepoEmptyName = OperationFailureMessage("Name must not be empty", UnprocessableEntity)
  def ContentRepoNameExisting = OperationFailureMessage("ContentRepository with this name already exists", Conflict)
  def ContentRepoUnknownProvider = OperationFailureMessage("Content provider not recognized", UnprocessableEntity)
  def ContentRepoRetrievalFailure = OperationFailureMessage(s"Content repository could not be retrieved from data base", InternalServerError)
  def ContentRepoNotFound = OperationFailureMessage("Content Repository for ID not found", NotFound)
  def ContentRepoNotFoundByName(name: String) = OperationFailureMessage(s"Content Repository with name '$name' not found", NotFound)
  def ContentRepoNotEmptyDelete = OperationFailureMessage("Content Repository contains folders", UnprocessableEntity)
  def ContentRepoWrongDataType(parameter: String, dataType: String) = OperationFailureMessage(s"Parameter $parameter is no $dataType", BadRequest)
}
