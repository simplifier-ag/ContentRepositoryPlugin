package io.simplifier.plugin.contentrepo.controller.file

import io.simplifier.pluginbase.util.json.NamedTupleAccess._
import io.simplifier.plugin.contentrepo.controller.mimeMapping.MimeMappingController
import io.simplifier.plugin.contentrepo.dao._
import io.simplifier.plugin.contentrepo.definitions.Constants._
import io.simplifier.plugin.contentrepo.definitions.LogMessages._
import io.simplifier.plugin.contentrepo.definitions.caseClasses.ContentFileCaseClasses._
import io.simplifier.plugin.contentrepo.definitions.exceptions.ContentFileExceptions._
import io.simplifier.plugin.contentrepo.dto.RestMessages._
import io.simplifier.plugin.contentrepo.contentRepoIo.{FileStreamSource, StreamSource}
import io.simplifier.plugin.contentrepo.model.ContentRepository
import io.simplifier.plugin.contentrepo.model.provider.ClearFileSystemProvider
import io.simplifier.plugin.contentrepo.permission.PermissionHandler
import io.simplifier.pluginapi.UserSession
import io.simplifier.pluginapi.rest.PluginHeaders.RequestSource
import io.simplifier.pluginbase.PluginDescription
import io.simplifier.pluginbase.SimplifierPlugin.AppServerInformation
import io.simplifier.pluginbase.interfaces.AppServerDispatcher
import org.json4s._

import java.nio.file.attribute.{BasicFileAttributeView, FileOwnerAttributeView}
import java.nio.file.{Files, Paths}
import java.sql.Timestamp
import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try

/**
  * Controller for file operations for provider "ClearFileSystem"
  */
class ClearFileSystemFileController(dispatcher: AppServerDispatcher,
                                    pluginDescription: PluginDescription,
                                    appServerInformation: AppServerInformation,
                                    provider: ClearFileSystemProvider,
                                    mimeMappingController: MimeMappingController,
                                    repo: ContentRepository,
                                    permissionHandler: PermissionHandler,
                                    contentFileDao: ContentFileDao = new ContentFileDao,
                                    contentFolderDao: ContentFolderDao = new ContentFolderDao,
                                    contentRepoDao: ContentRepositoryDao = new ContentRepositoryDao)
  extends ContentFileController(dispatcher, pluginDescription, appServerInformation, contentFileDao, contentFolderDao, contentRepoDao) {

  val PROVIDER_NAME = "ClearFileSystem"

  /**
    * Adds a new file to the file system.
    * File data is defined through a data string, an upload session or a file to be copied.
    *
    * @param json        the json received from the REST call
    * @param userSession the implicit user session
    * @return path of the created file
    */
  override def addFile(json: JValue)(implicit userSession: UserSession,
                                     requestSource: RequestSource,
                                     ec: ExecutionContext): Future[ClearContentFileAddResponse] = {
    logger.debug(slotMessageBegin(ACTION_SLOT_ADD, ASPECT_FILE, PROVIDER_NAME, json.filterField(field => field.name != "data")))
    permissionHandler.checkPermissions().flatMap { _ =>
      val fileData = extractWithCustomError[ClearContentFileAddRequest](json)
      checkNotEmpty(fileData.fileName, ContentFileEmptyFilename)

      val filePathFuture = (fileData.data, fileData.uploadSession, fileData.copyFrom) match {
        case (Some(_), None, None) =>
          provider.createFile(repo.name, fileData.folderPath, fileData.fileName, dataDecoded(fileData.data).get, fileData.forceOverwrite)
        case (None, Some(_), None) =>
          dataUploadFetched(fileData.uploadSession).flatMap(bytesource =>
            provider.createFile(repo.name, fileData.folderPath, fileData.fileName, bytesource, fileData.forceOverwrite)
          )
        case (None, None, Some(_)) =>
          Future(provider.copyFile(repo.name, fileData.copyFrom.get, fileData.folderPath, fileData.fileName, fileData.forceOverwrite.getOrElse(false)))
        case (None, None, None) =>
          Future.failed(ContentFileNoData)
        case _ =>
          Future.failed(ContentFileWrongData)
      }
      filePathFuture.map { filePath =>
        logger.debug(slotMessageEnd(ACTION_SLOT_ADD, ASPECT_FILE, PROVIDER_NAME, filePath))
        ClearContentFileAddResponse(filePath, addFileSuccess)
      }
    }

  }

  /**
    * Get data, length and path of a file.
    *
    * @param json          the json received from the REST call
    * @param userSession   the implicit user session
    * @param requestSource the implicit request source.
    * @return data, length and path of the requested file
    */
  override def getFile(json: JValue)
                      (implicit userSession: UserSession, requestSource: RequestSource, ec: ExecutionContext): Future[ClearContentFileGetResponse] = {
    logger.debug(slotMessageBegin(ACTION_SLOT_GET, ASPECT_FILE, PROVIDER_NAME, json))
    permissionHandler.checkPermissions().map { _ =>
      val fileData = extractWithCustomError[ClearContentFileGetRequest](json)
      val file = provider.getFile(repo.name, fileData.filePath)
      val data = file.data.get
      val metaData = getMetadata(fileData.filePath)
      val mimeType = mimeMappingController.getMimeTypeAndExtensionForDownload(fileData.filePath)
      val url = getUrl(file.filePathForDownload)
      val urlWithToken: Option[String] = userSession.tokenOpt.map(tok => s"$url?SimplifierToken=$tok")
      logger.debug(slotMessageEnd(ACTION_SLOT_GET, ASPECT_FILE, PROVIDER_NAME, file))
      ClearContentFileGetResponse(fileData.filePath, encodeB64(data), getFileSuccess, metaData.length,
        mimeType, s"$url/", urlWithToken, Some(metaData.recDate), Some(metaData.chgDate))
    }
  }

  /**
    * Get the file metadata
    *
    * @param json          the json received from the REST call
    * @param userSession   the implicit user session
    * @param requestSource the implicit request source.
    * @return depends on the implementation
    */
  override def getFileMetadata(json: JValue)
                              (implicit userSession: UserSession, requestSource: RequestSource, ec: ExecutionContext): Future[ClearContentFileGetMetadataResponse] = {
    logger.debug(slotMessageBegin(ACTION_SLOT_GET_METADATA, ASPECT_FILE, PROVIDER_NAME, json))
    permissionHandler.checkPermissions().map { _ =>
      val fileData = extractWithCustomError[ClearContentFileGetRequest](json)
      val metadata = getMetadata(fileData.filePath)
      val retValue = ClearContentFileGetMetadataResponse(metadata.filePath, metadata.mimeType, metadata.url, metadata.urlWithToken,
        getFileMetadataSuccess, Some(metadata.recDate), Some(metadata.chgDate), metadata.length)
      logger.debug(slotMessageEnd(ACTION_SLOT_GET_METADATA, ASPECT_FILE, PROVIDER_NAME, retValue))
      retValue
    }
  }

  /**
    * Edit file name and/or move file within a repo.
    *
    * @param json        the json received from the REST call
    * @param userSession the implicit user session
    * @return success message
    */
  override def editFile(json: JValue)(implicit userSession: UserSession,
                                      requestSource: RequestSource,
                                      ec: ExecutionContext): Future[ContentFileEditResponse] = {
    logger.debug(slotMessageBegin(ACTION_SLOT_EDIT, ASPECT_FILE, PROVIDER_NAME, json))
    permissionHandler.checkPermissions().map { _ =>
      val fileData = extractWithCustomError[ClearContentFileEditRequest](json)
      provider.moveFile(repo.name, fileData.sourceFilePath, fileData.destFilePath, fileData.forceOverwrite)
      logger.debug(slotMessageEnd(ACTION_SLOT_EDIT, ASPECT_FILE, PROVIDER_NAME))
      ContentFileEditResponse(editFileSuccess)
    }
  }

  /**
    * Delete a file from the file system.
    *
    * @param json        the json received from the REST call
    * @param userSession the implicit user session
    * @return success message
    */
  override def deleteFile(json: JValue)(implicit userSession: UserSession,
                                        requestSource: RequestSource,
                                        ec: ExecutionContext): Future[ContentFileDeleteResponse] = {
    logger.debug(slotMessageBegin(ACTION_SLOT_DELETE, ASPECT_FILE, PROVIDER_NAME, json))
    permissionHandler.checkPermissions().map { _ =>
      val fileData = extractWithCustomError[ClearContentFileDeleteRequest](json)
      provider.deleteFile(repo.name, fileData.filePath)
      logger.debug(slotMessageEnd(ACTION_SLOT_DELETE, ASPECT_FILE, PROVIDER_NAME))
      ContentFileDeleteResponse(deleteFileSuccess)
    }
  }

  /**
    * Find all files with given name within a repo and (optional) within a folder.
    *
    * @param json          the json received from the REST call
    * @param userSession   the implicit user session
    * @param requestSource the implicit request source.
    * @return list of file paths
    */
  override def findFile(json: JValue)(implicit userSession: UserSession,
                                      requestSource: RequestSource,
                                      ec: ExecutionContext): Future[ContentFileFindResponse] = {
    logger.debug(slotMessageBegin(ACTION_SLOT_FIND, ASPECT_FILE, PROVIDER_NAME, json))
    permissionHandler.checkPermissions().map { _ =>
      val fileData = extractWithCustomError[ClearContentFileFindRequest](json)
      val mimeType = mimeMappingController.getMimeTypeAndExtensionForDownload(fileData.fileName)
      val responseItems = provider.findFile(repo.name, fileData.folderPath, fileData.fileName).map { f =>
        val metaData = getMetadata(Paths.get(fileData.folderPath.getOrElse(""), fileData.fileName).toString)
        val url: String = getUrl(f.filePathForDownload)
        val urlWithToken: Option[String] = userSession.tokenOpt.map(tok => s"$url?SimplifierToken=$tok")
        ClearContentFileFindResponseItem(f.filePath, mimeType, s"$url/", urlWithToken, Some(metaData.recDate),
          Some(metaData.chgDate), metaData.length)
      }
      logger.debug(slotMessageEnd(ACTION_SLOT_FIND, ASPECT_FILE, PROVIDER_NAME, responseItems))
      ContentFileFindResponse(responseItems, listFileSuccess)
    }
  }

  /**
    * List all files within a folder.
    *
    * @param json          the json received from the REST call
    * @param userSession   the implicit user session
    * @param requestSource the implicit request source.
    * @return list of file names
    */
  override def listFiles(json: JValue)(implicit userSession: UserSession,
                                       requestSource: RequestSource,
                                       ec: ExecutionContext): Future[ContentFileListResponse] = {
    logger.debug(slotMessageBegin(ACTION_SLOT_LIST, ASPECT_FILE, PROVIDER_NAME, json))
    permissionHandler.checkPermissions().map { _ =>
      val fileData = extractWithCustomError[ClearContentFileListRequest](json)
      val responseItems = provider.listFiles(repo.name, fileData.folderPath).map { f =>
        val mimeType = mimeMappingController.getMimeTypeAndExtensionForDownload(f.fileName)
        val url: String = getUrl(f.filePathForDownload)
        val urlWithToken: Option[String] = userSession.tokenOpt.map(tok => s"$url?SimplifierToken=$tok")
        val metaData = getMetadata(Paths.get(fileData.folderPath.getOrElse(""), f.fileName).toString)
        ClearContentFileListResponseItem(f.fileName, mimeType, s"$url/", urlWithToken, Some(metaData.recDate),
          Some(metaData.chgDate), metaData.length)
      }
      logger.debug(slotMessageEnd(ACTION_SLOT_LIST, ASPECT_FILE, PROVIDER_NAME, responseItems))
      ContentFileListResponse(responseItems, listFileSuccess)
    }
  }

  /**
    * Resolve file to stream
    *
    * @param repoName          repo name
    * @param parentFolderNames names of parent folders
    * @param folderName        folder name
    * @param fileName          file name
    * @param userSession       the implicit user session
    * @return stream
    */
  override def resolveContentFileToStream(repoName: String, parentFolderNames: Seq[String],
                                          folderName: String, fileName: String)
                                         (implicit userSession: UserSession,
                                          requestSource: RequestSource,
                                          ec: ExecutionContext): Future[Option[Try[StreamSource]]] = {
    val path = provider.getNioPath(repoName, parentFolderNames, folderName, fileName)
    Future.successful(Some(Try(new FileStreamSource(path))))
  }

  override protected def getMetadataBatched(json: JValue)
                                           (implicit userSession: UserSession,
                                            requestSource: RequestSource,
                                            ec: ExecutionContext): Future[Seq[GetFileMetadataBatchedResponseItem]] = {
    permissionHandler.checkPermissions().map { _ =>
      extractWithCustomError[ClearContentFileGetMetadataBatchedRequest](json).files.map(
        file => getMetadata(file.filePath)
      )
    }
  }

  def getMetadata(filePath: String)(implicit userSession: UserSession,
                                            requestSource: RequestSource): ClearContentFileGetMetadataBatchedResponseItem = {
    val mimeType = mimeMappingController.getMimeTypeAndExtensionForDownload(filePath)
    val url = getUrl(provider.getFilePathForDownloadUrl(repo.name, filePath))
    val urlWithToken: Option[String] = userSession.tokenOpt.map(tok => s"$url?SimplifierToken=$tok")
    val pathWithRepository = provider.getNioPath(repo.name, Seq(), "", filePath)
    val basicMetaDataAttributes = Files.getFileAttributeView(pathWithRepository, classOf[BasicFileAttributeView]).readAttributes()
    val ownerMetaData = Files.getFileAttributeView(pathWithRepository, classOf[FileOwnerAttributeView])
    ClearContentFileGetMetadataBatchedResponseItem(filePath, mimeType, url, urlWithToken,
      new Timestamp(basicMetaDataAttributes.creationTime().toMillis),
      ownerMetaData.getOwner.getName,
      new Timestamp(basicMetaDataAttributes.lastModifiedTime().toMillis),
      basicMetaDataAttributes.size())
  }

}
