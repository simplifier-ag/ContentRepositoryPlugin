package io.simplifier.plugin.contentrepo.dto

import io.swagger.annotations.ApiModelProperty


object RestMessages {

  /** Rest message for JSON output */
  case class RestMessage(
                          @ApiModelProperty(value="The message id.") msgId: String,
                          @ApiModelProperty(value="The message type.") msgType: String,
                          @ApiModelProperty(value="The message text.") msgText: String)

  private def msgIdContent(id: Int) = s"iTZContent" + ("%02d" format id)

  private val typeSuccess = "S"
  private val typeError = "E"

  private var nextOperationMessageId = 1

  /** Generator function for REST messages. */
  private def mkRestMessagePair(dataObject: String, op: String, plural: Boolean = false): (RestMessage, String => RestMessage) = {
    val errorId = nextOperationMessageId
    val successId = nextOperationMessageId + 1
    val haveHas = if (plural) "have" else "has"
    nextOperationMessageId += 2
    val successMessage = RestMessage(msgIdContent(successId), typeSuccess, s"$dataObject $haveHas been $op successfully.")
    val genErrorMessage: String => RestMessage = { reason =>
      RestMessage(msgIdContent(errorId), typeError, s"$dataObject cannot be $op due to the following reason: $reason.")
    }
    (successMessage, genErrorMessage)
  }

  val (addRepositorySuccess, addRepositoryError) = mkRestMessagePair("Repository", "created")
  val (getRepositorySuccess, getRepositoryError) = mkRestMessagePair("Repository", "fetched")
  val (editRepositorySuccess, editRepositoryError) = mkRestMessagePair("Repository", "edited")
  val (deleteRepositorySuccess, deleteRepositoryError) = mkRestMessagePair("Repository", "deleted")
  val (listRepositorySuccess, listRepositoryError) = mkRestMessagePair("Repositories", "listed", plural = true)

  val (addFolderSuccess, addFolderError) = mkRestMessagePair("Folder", "created")
  val (getFolderSuccess, getFolderError) = mkRestMessagePair("Folder", "fetched")
  val (editFolderSuccess, editFolderError) = mkRestMessagePair("Folder", "edited")
  val (deleteFolderSuccess, deleteFolderError) = mkRestMessagePair("Folder", "deleted")
  val (listFolderSuccess, listFolderError) = mkRestMessagePair("Folders", "listed", plural = true)

  val (addFileSuccess, addFileError) = mkRestMessagePair("File", "created")
  val (getFileSuccess, getFileError) = mkRestMessagePair("File", "fetched")
  val (getFileMetadataSuccess, getFileMetadataError) = mkRestMessagePair("File metadata", "fetched")
  val (getFileMetadataBatchedSuccess, getFileMetadataBatchedError) = mkRestMessagePair("File metadata", "batch fetched")
  val (editFileSuccess, editFileError) = mkRestMessagePair("File", "edited")
  val (deleteFileSuccess, deleteFileError) = mkRestMessagePair("File", "deleted")
  val (listFileSuccess, listFileError) = mkRestMessagePair("Files", "listed", plural = true)

  val (addMimeMappingSuccess, addMimeMappingError) = mkRestMessagePair("Mime Mapping", "added")
  val (getMimeMappingSuccess, getMimeMappingError) = mkRestMessagePair("Mime Mapping", "fetched")
  val (editMimeMappingSuccess, editMimeMappingError) = mkRestMessagePair("Mime Mapping", "modified")
  val (deleteMimeMappingSuccess, deleteMimeMappingError) = mkRestMessagePair("Mime Mapping", "deleted")
  val (listMimeMappingSuccess, listMimeMappingError) = mkRestMessagePair("Mime Mappings", "listed", plural = true)

}