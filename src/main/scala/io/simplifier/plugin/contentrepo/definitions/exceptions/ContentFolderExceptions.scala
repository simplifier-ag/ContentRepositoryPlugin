package io.simplifier.plugin.contentrepo.definitions.exceptions

import akka.http.scaladsl.model.StatusCodes._
import io.simplifier.plugin.contentrepo.controller.BaseController.OperationFailureMessage

object ContentFolderExceptions {

  def ContentFolderEmptyPermissionObjectType = OperationFailureMessage("PermissionObjectType must not be empty", UnprocessableEntity)
  def ContentFolderEmptyPermissionObjectId = OperationFailureMessage("PermissionObjectId must not be empty", UnprocessableEntity)
  def ContentFolderEmptySecuritySchemeID = OperationFailureMessage("SecuritySchemeID must not be empty", UnprocessableEntity)
  def ContentFolderEmptyName = OperationFailureMessage("name must not be empty", UnprocessableEntity)
  def ContentFolderDuplicateFolderName(folderName: String) = OperationFailureMessage(s"Folder with name '$folderName' already exists in Content Repository or Parent Folder", UnprocessableEntity)
  def ContentFolderInvalidSecuritySchemeID = OperationFailureMessage("Invalid value for SecuritySchemeID", UnprocessableEntity)
  def ContentFolderParentInWrongRepo = OperationFailureMessage("Parent folder is in another content repository", UnprocessableEntity)
  def ContentFolderNotFound = OperationFailureMessage("Content folder for ID not found", NotFound)
  def ClearContentFolderNotFound = OperationFailureMessage("Content folder not found", NotFound)
  def ContentFolderRetrievalFailure(list: Boolean = false) = OperationFailureMessage(s"Content folder${if (list) "s" else ""} could not be retrieved from data base", InternalServerError)
  def ContentFolderNotEmptyFiles = OperationFailureMessage("Content folder contains files", UnprocessableEntity)
  def ContentFolderNotEmptyFolders = OperationFailureMessage("Content folder contains child folders", UnprocessableEntity)
  def ContentFolderNotEmptyFoldersAndFiles = OperationFailureMessage("Content folder contains child folders as well as files", UnprocessableEntity)
  def ContentFolderNotEmpty = OperationFailureMessage("Content folder contains children", UnprocessableEntity)
  def ContentFolderInvalidCharacter(reason: String) = OperationFailureMessage(s"$reason in folder name", UnprocessableEntity)
  def ContentFolderUnexpectedError(msg: String) = OperationFailureMessage(s"Failed unexpectedly: $msg", InternalServerError)
  def ContentFolderRepoNotFound = OperationFailureMessage("Content repository not found", UnprocessableEntity)
  def ContentFolderParentNotFound = OperationFailureMessage("Parent folder not found", UnprocessableEntity)
}
