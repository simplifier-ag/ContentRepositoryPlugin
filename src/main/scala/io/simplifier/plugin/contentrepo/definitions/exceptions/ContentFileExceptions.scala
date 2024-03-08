package io.simplifier.plugin.contentrepo.definitions.exceptions

import akka.http.scaladsl.model.StatusCodes.{InternalServerError, NotFound, UnprocessableEntity}
import io.simplifier.plugin.contentrepo.controller.BaseController.OperationFailureMessage
import io.simplifier.plugin.contentrepo.model.{ContentFile, ContentFolder}

object ContentFileExceptions {

  def ContentFileEmptyFilename = OperationFailureMessage("Filename must not be empty", UnprocessableEntity)
  def ContentFileEmptyPermissionObjectType = OperationFailureMessage("PermissionObjectType must not be empty", UnprocessableEntity)
  def ContentFileEmptyPermissionObjectId = OperationFailureMessage("PermissionObjectId must not be empty", UnprocessableEntity)
  def ContentFileEmptySecuritySchemeID = OperationFailureMessage("SecuritySchemeID must not be empty", UnprocessableEntity)
  def ContentFileInvalidSecuritySchemeID = OperationFailureMessage("Invalid value for SecuritySchemeID", UnprocessableEntity)
  def ContentFileFolderNotFound = OperationFailureMessage("Content folder not found", UnprocessableEntity)
  def ContentFileRepoNotFound = OperationFailureMessage("Content repository not found", UnprocessableEntity)
  def ContentFileDuplicateFilename = OperationFailureMessage("Filename already exists in folder", UnprocessableEntity)
  def ContentFileRepoProviderInvalid = OperationFailureMessage("Underlying repository provider is invalid", UnprocessableEntity)
  def ContentFileIO = OperationFailureMessage("Error on I/O operations", InternalServerError)
  def ContentFileIOLoadFailure = OperationFailureMessage("Error during the loading of a file", InternalServerError)
  def ContentFileIO(reason: String) = OperationFailureMessage(s"Error on I/O operation: $reason", InternalServerError)

  def ContentFileNotFound = OperationFailureMessage("Content file not found", NotFound)
  def ContentFileExternalStorageUpdateFailure = OperationFailureMessage(s"External storage definition of the content file could not be updated in the data base", InternalServerError)
  def ContentFileInsertionFailure = OperationFailureMessage(s"Content file could not be retrieved into the data base", InternalServerError)
  def ContentFileUpdateFailure = OperationFailureMessage(s"Content file could not be updated in the data base", InternalServerError)
  def ContentFileRetrievalFailure(list: Boolean = false) = OperationFailureMessage(s"Content file${if (list) "s" else ""} could not be retrieved from data base", InternalServerError)
  def ContentFileDecoding = OperationFailureMessage("Content data could not be decoded", UnprocessableEntity)
  def ContentFileEncoding = OperationFailureMessage("Content data could not be encoded", UnprocessableEntity)
  def ContentFileUploadSessionNotFound = OperationFailureMessage("Upload session not found", NotFound)
  def ContentFileMissingData = OperationFailureMessage("Neither content data not upload session provided", UnprocessableEntity)
  def ContentFileWrongData = OperationFailureMessage("Too many data sources were passed. Provide either a data source or an upload session or a file path")
  def ContentFileNoData = OperationFailureMessage("No data source was passed. Provide either a data source or an upload session or a file path")
  def ContentFileAlreadyExists = OperationFailureMessage("The file already exists. To overwrite the file pass true as forceOverwrite parameter")

  def ContentFileActionError(file: ContentFile, folder: ContentFolder, action: String) =
    OperationFailureMessage(s"File with name: {${file.fileName}} and file system name: {${file.id}} in folder: {${folder.folderName}} and file system name: {${folder.id}} could not be $action!")
}
