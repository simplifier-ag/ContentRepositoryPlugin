package io.simplifier.plugin.contentrepo.controller.file

import akka.http.scaladsl.model.ContentType.Binary
import akka.http.scaladsl.model.ContentTypes.`application/octet-stream`
import akka.http.scaladsl.model._
import com.google.common.net.UrlEscapers
import io.simplifier.plugin.contentrepo.controller.BaseController
import io.simplifier.plugin.contentrepo.controller.BaseController.ContentRepoPermissions
import io.simplifier.plugin.contentrepo.dao._
import io.simplifier.plugin.contentrepo.definitions.caseClasses.ContentFileCaseClasses._
import io.simplifier.plugin.contentrepo.definitions.caseClasses.MimeMappingCaseClasses.MimeMappingReturn
import io.simplifier.plugin.contentrepo.definitions.exceptions.ContentFileExceptions._
import io.simplifier.plugin.contentrepo.definitions.exceptions.ContentFolderExceptions._
import io.simplifier.plugin.contentrepo.definitions.exceptions.ContentRepositoryExceptions._
import io.simplifier.plugin.contentrepo.dto.RestMessages._
import io.simplifier.plugin.contentrepo.interfaces.ContentRepoProxyInterface
import io.simplifier.plugin.contentrepo.contentRepoIo.StreamSource
import io.simplifier.plugin.contentrepo.model.{ContentFile, ContentFolder, ContentRepository, SecurityScheme}
import io.simplifier.pluginapi.UserSession
import io.simplifier.pluginapi.helper.Base64Encoding
import io.simplifier.pluginapi.rest.PluginHeaders._
import io.simplifier.pluginbase.PluginDescription
import io.simplifier.pluginbase.SimplifierPlugin.AppServerInformation
import io.simplifier.pluginbase.interfaces.AppServerDispatcher
import io.simplifier.pluginbase.util.api.ApiMessage
import io.simplifier.pluginbase.util.io.StreamUtils
import io.simplifier.pluginbase.util.io.StreamUtils.ByteSource
import org.json4s._

import scala.annotation.tailrec
import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try

/**
  * Controller for Content Files.
  */
abstract class ContentFileController(
                                      appServerDispatcher: AppServerDispatcher,
                                      pluginDescription: PluginDescription,
                                      appServerInformation: AppServerInformation,
                                      contentFileDao: ContentFileDao = new ContentFileDao,
                                      contentFolderDao: ContentFolderDao = new ContentFolderDao,
                                      contentRepoDao: ContentRepositoryDao = new ContentRepositoryDao,
                                    )
  extends BaseController with Base64Encoding {

  val PROVIDER_NAME: String

  /**
    * Base function to add file
    * Implemented in the provider specific controllers
    *
    * @param json        the json received from the REST call
    * @param userSession the implicit user session
    * @return depends on the implementation
    */
  def addFile(json: JValue)(implicit userSession: UserSession, requestSource: RequestSource, ec: ExecutionContext): Future[ApiMessage]

  /**
    * Base function to get file data
    * Implemented in the provider specific controllers
    *
    * @param json             the json received from the REST call
    * @param userSession      the implicit user session
    * @param requestSource    the implicit request source.
    * @return                 depends on the implementation
    */
  def getFile(json: JValue)(implicit userSession: UserSession, requestSource: RequestSource, ec: ExecutionContext): Future[ApiMessage]

  /**
    * Base function to get only the file metadata without the raw file data
    *
    * @param json             the json received from the REST call
    * @param userSession      the implicit user session
    * @param requestSource    the implicit request source.
    * @return                 depends on the implementation
    */
  def getFileMetadata(json: JValue)(implicit userSession: UserSession, requestSource: RequestSource, ec: ExecutionContext): Future[ApiMessage]

  /**
    * Base function to get only the file metadata for many files
    *
    * @param json              the json received from the REST call
    * @param userSession       the implicit user session
    * @param requestSource     the implicit request source.
    * @return                  depends on the implementation
    */
  def getFileMetadataBatched(json: JValue)(implicit userSession: UserSession, requestSource: RequestSource, ec: ExecutionContext): Future[GetFileMetadataBatchedResponse] = {
    getMetadataBatched(json).map { fileMetadata =>
      GetFileMetadataBatchedResponse(fileMetadata, getFileMetadataBatchedSuccess)
    }
  }

  protected def getMetadataBatched(json: JValue)(implicit userSession: UserSession, requestSource: RequestSource, ec: ExecutionContext): Future[Seq[GetFileMetadataBatchedResponseItem]]

  /**
    * Base function to edit file
    * Implemented in the provider specific controllers
    *
    * @param json        the json received from the REST call
    * @param userSession the implicit user session
    * @return depends on the implementation
    */
  def editFile(json: JValue)(implicit userSession: UserSession, requestSource: RequestSource, ec: ExecutionContext): Future[ApiMessage]

  /**
    * Base function to delete file
    * Implemented in the provider specific controllers
    *
    * @param json        the json received from the REST call
    * @param userSession the implicit user session
    * @return depends on the implementation
    */
  def deleteFile(json: JValue)(implicit userSession: UserSession, requestSource: RequestSource, ec: ExecutionContext): Future[ApiMessage]

  /**
    * Base function to find file
    * Implemented in the provider specific controllers
    *
    * @param json            the json received from the REST call
    * @param userSession     the implicit user session
    * @param requestSource   the implicit request source.
    * @return                depends on the implementation
    */
  def findFile(json: JValue)(implicit userSession: UserSession, requestSource: RequestSource, ec: ExecutionContext): Future[ApiMessage]

  /**
    * Base function to list files
    * Implemented in the provider specific controllers
    *
    * @param json             the json received from the REST call
    * @param userSession      the implicit user session
    * @param requestSource    the implicit request source.
    * @return                 depends on the implementation
    */
  def listFiles(json: JValue)(implicit userSession: UserSession, requestSource: RequestSource, ec: ExecutionContext): Future[ApiMessage]

  protected def convertPossibleMimeToBinary(possibleMime: Option[String]): Try[Binary] = Try {
    possibleMime match {
      case None => `application/octet-stream`
      case Some(mime) => ContentType.Binary(MediaType.custom(mime, binary = true).asInstanceOf[MediaType.Binary])
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
  def resolveContentFileToStream(repoName: String, parentFolderNames: Seq[String], folderName: String,
                                 fileName: String)(implicit userSession: UserSession,
                                                   requestSource: RequestSource,
                                                   ec: ExecutionContext): Future[Option[Try[StreamSource]]]

  protected def resolveContentFile(repoName: String, parentFolderNames: Seq[String], folderName: String,
                                   fileName: String, permissions: ContentRepoPermissions): Option[(ContentFile, ContentFolder)] = {
    val allFolderNames: Seq[String] = parentFolderNames :+ folderName
    val rootFolderName :: subFolderNames = allFolderNames.toList

    val repoOpt: Option[ContentRepository] = contentRepoDao.getByName(repoName).fold(
      e => throw ContentRepoRetrievalFailure.initCause(e),
      res => res.filter(permissions.hasPermissionObjectId)
    )

    repoOpt flatMap { repo =>
      val rootFolderOpt = contentFolderDao.findByNameAndParent(repo.id, rootFolderName, None).fold(
        e => throw ContentFolderRetrievalFailure().initCause(e),
        res => res.filter(permissions.hasPermissionObjectId)
      )

      val innerFolder = (subFolderNames foldLeft rootFolderOpt) {
        (parentFolderOpt, nextFolderName) =>
          parentFolderOpt.flatMap {
            folder =>
              contentFolderDao.findByNameAndParent(repo.id, nextFolderName, Some(folder.id))
                .fold(e => throw ContentFolderRetrievalFailure().initCause(e), res => res)
          }.filter(permissions.hasPermissionObjectId)
      }

      innerFolder flatMap { folder =>
        contentFileDao
          .getByFolderAndName(folder.id, fileName).fold(e => throw ContentFileRetrievalFailure().initCause(e), res => res)
          .filter(permissions.hasPermissionObjectId)
          .map((_, folder))
      }
    }
  }

  @tailrec
  protected final def isFolderPublicRecursive(folder: ContentFolder): Boolean = {
    (folder.securitySchemeID == SecurityScheme.Public.name) && {
      folder.parentFolderId match {
        case None => true
        case Some(parentFolderId) =>
          val parentFolder = Try(contentFolderDao.getById(parentFolderId)).fold(e =>
            throw ContentFolderRetrievalFailure().initCause(e), res => res).getOrElse(throw ContentFileFolderNotFound)
          isFolderPublicRecursive(parentFolder)
      }
    }
  }

  /*
   * Data decoding
   */

  /**
    * Decode data string to byte source
    *
    * @param data data string
    * @return byte source
    */
  def dataDecoded(data: Option[String]): Option[ByteSource] = data map {
    content =>
      val bytes = Try(decodeB64(content)).getOrElse(throw ContentFileDecoding)
      io.simplifier.plugin.contentrepo.pluginBaseRelated.fileIo.StreamUtils.byteArraySource(bytes)
  }

  /**
    * Fetch data from upload session
    *
    * @param uploadSession upload session
    * @param userSession   the implicit user session
    * @return future byte source
    */
  def dataUploadFetched(uploadSession: Option[String])(implicit userSession: UserSession,
                                                       requestSource: RequestSource): Future[ByteSource] = {
    uploadSession.map {
      assetName => appServerDispatcher.downloadAsset(assetName)
    }.getOrElse(Future.failed(ContentFileUploadSessionNotFound))
  }

  protected[this] def getContentFile(id: Int, exception: Throwable): Try[ContentFile] = {
    Try(contentFileDao.getById(id)).transform(res => Try(res.getOrElse(throw exception)), e =>
      throw ContentFileRetrievalFailure().initCause(e))
  }

  protected[this] def getContentFileFolder(folderId: Int, exception: Throwable): Try[ContentFolder] = {
    Try(contentFolderDao.getById(folderId)).transform(cf => Try(cf.getOrElse(throw exception)), e =>
      throw ContentFolderRetrievalFailure().initCause(e))
  }

  protected[this] def checkForDuplicateFiles(folderId: Int, fileName: String): Try[Unit] = {
    contentFileDao.getByFolderAndName(folderId, fileName)
      .transform(cf => Try(cf.foreach(_ => throw ContentFileDuplicateFilename)), e =>
        throw ContentFileRetrievalFailure().initCause(e))
  }

  protected[this] def mapContentFileModel(contentFile: ContentFile, mimeType: MimeMappingReturn, url: String,
                                          urlWithToken: Option[String], size: Long): ContentFileListResponseItem = {
    ContentFileListResponseItem(contentFile.id, contentFile.fileName,
      contentFile.fileDescription, contentFile.statusSchemeID, contentFile.statusID, contentFile.securitySchemeID,
      contentFile.permissionObjectType, contentFile.permissionObjectID, contentFile.folderID, mimeType, url, urlWithToken,
      contentFile.recDate, contentFile.recUser, contentFile.chgDate, contentFile.chgUser, size
    )
  }

  protected[contentrepo] def getUrl(filePathSegments: Seq[String])(implicit requestSource: RequestSource): String = {
    def getSource(source: Option[Uri]): String = s"${
      source
        .flatMap(u => if (u.isEmpty) {
          logger.warn("The provided uri was empty")
          None
        } else Some(u))
        .orElse {
          logger.warn("The called URI was not provided. Falling back to the AppServer prefix.")
          appServerInformation.prefix
        }.getOrElse {
        logger.warn("The AppServer prefix was was not provided. Falling back to localhost: {http://localhost:8080}.")
        "http://localhost:8080"
      }
    }"

    val appServer: String = (requestSource match {
      case AppServer(source) => getSource(source)
      case AppServerDirect(source) => getSource(source)
      case BusinessObject(_, source) => getSource(source)
      case Plugin(_, source) => getSource(source)
    }).stripSuffix("/")

    val encodedFilePath = encodePath(filePathSegments)

    val url: String = s"$appServer/" +
                      s"${appServerInformation.routes.fold("client/2.0/plugin")(_.client.getOrElse("PluginProxyService", "plugin"))}/" +
                      s"${pluginDescription.name}/" +
                      s"${ContentRepoProxyInterface.FILE_PREFIX}/" +
                      s"$encodedFilePath"
    url
  }

  private def encodePath(path: Seq[String]): String =
    path.map(segment => UrlEscapers.urlPathSegmentEscaper().escape(segment)).mkString("/")
}