package io.simplifier.plugin.contentrepo.controller.file

import io.simplifier.pluginbase.util.json.NamedTupleAccess._
import akka.stream.Materializer
import akka.stream.scaladsl.Sink
import io.simplifier.plugin.contentrepo.controller.BaseController.ContentRepoPermissions
import io.simplifier.plugin.contentrepo.controller.folder.ContentFolderController
import io.simplifier.plugin.contentrepo.controller.mimeMapping.MimeMappingController
import io.simplifier.plugin.contentrepo.controller.repository.ContentRepositoryController
import io.simplifier.plugin.contentrepo.dao._
import io.simplifier.plugin.contentrepo.definitions.Constants._
import io.simplifier.plugin.contentrepo.definitions.LogMessages._
import io.simplifier.plugin.contentrepo.definitions.caseClasses.ContentFileCaseClasses._
import io.simplifier.plugin.contentrepo.definitions.caseClasses.ContentFolderCaseClasses.ContentFolderGetResponseForAll
import io.simplifier.plugin.contentrepo.definitions.caseClasses.ContentRepositoryCaseClasses.ContentRepoListResponseItem
import io.simplifier.plugin.contentrepo.definitions.exceptions.ContentFileExceptions._
import io.simplifier.plugin.contentrepo.definitions.exceptions.ContentFolderExceptions.ContentFolderRetrievalFailure
import io.simplifier.plugin.contentrepo.definitions.exceptions.ContentRepositoryExceptions.ContentRepoNotFound
import io.simplifier.plugin.contentrepo.dto.RestMessages._
import io.simplifier.plugin.contentrepo.helper.database
import io.simplifier.plugin.contentrepo.contentRepoIo.StreamSource
import io.simplifier.plugin.contentrepo.model.provider.{ContentFileIO, FileSystemProvider}
import io.simplifier.plugin.contentrepo.model.{ContentFile, ContentFolder, ContentRepository, SecurityScheme}
import io.simplifier.plugin.contentrepo.permission.PermissionHandler
import io.simplifier.pluginapi.UserSession
import io.simplifier.pluginapi.rest.PluginHeaders.RequestSource
import io.simplifier.pluginbase.PluginDescription
import io.simplifier.pluginbase.SimplifierPlugin.AppServerInformation
import io.simplifier.pluginbase.interfaces.AppServerDispatcher
import io.simplifier.pluginbase.model.UserTransaction
import io.simplifier.pluginbase.util.api.ApiMessage
import io.simplifier.pluginbase.util.io.StreamUtils.ByteSource
import org.json4s._
import org.apache.commons.io.FilenameUtils

import java.nio.file.Files
import java.nio.file.attribute.BasicFileAttributeView
import java.sql.Timestamp
import java.util.Date
import scala.annotation.tailrec
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, ExecutionContext, Future}
import scala.util.control.NonFatal
import scala.util.{Success, Try}

/**
  * Controller for file operations for provider "FileSystem"
  */
class FileSystemFileController(
                                dispatcher: AppServerDispatcher,
                                pluginDescription: PluginDescription,
                                appServerInformation: AppServerInformation,
                                fileSystemProvider: FileSystemProvider,
                                mimeMappingController: MimeMappingController,
                                contentRepoController: ContentRepositoryController,
                                contentFolderController: ContentFolderController,
                                permissionHandler: PermissionHandler,
                                permissionsOpt: Option[ContentRepoPermissions],
                                timeout: Duration,
                                databaseHelper: database.ContentFileHelper = new database.ContentFileHelper,
                                contentRepoDao: ContentRepositoryDao = new ContentRepositoryDao,
                                contentFolderDao: ContentFolderDao = new ContentFolderDao,
                                contentFileDao: ContentFileDao = new ContentFileDao,
                                mimeMappingDao: MimeMappingDao = new MimeMappingDao,
                                userTransaction: UserTransaction = new UserTransaction
                              )(implicit materializer: Materializer)
  extends ContentFileController(dispatcher, pluginDescription, appServerInformation,
    contentFileDao, contentFolderDao, contentRepoDao) {

  val PROVIDER_NAME: String = "FileSystem"

  /**
    * Add a new file to file system and database.
    * File data is defined through a data string, an upload session or a file to be copied.
    *
    * @param json        the json received from the REST call
    * @param userSession the implicit user session
    * @return file id and file name
    */
  override def addFile(json: JValue)(implicit userSession: UserSession, requestSource: RequestSource, ec: ExecutionContext): Future[ApiMessage] = {
    logger.debug(slotMessageBegin(ACTION_SLOT_ADD, ASPECT_FILE, PROVIDER_NAME, json.filterField(field => field.name != "data")))
    Future(extractWithCustomError[ContentFileAddRequest](json)).flatMap { fileData =>
      resolveData(fileData).map { dataResolved =>
        (fileData, dataResolved)
      }
    }.flatMap { case (fileData, dataResolved) =>
      (for {
        _ <- Future(checkContentFileParams(fileData.name, fileData.securitySchemeID, fileData.permissionObjectType, fileData.permissionObjectID))
        permissions <- Future(permissionsOpt.get).recoverWith { case _ => permissionHandler.checkPermissions() }
        user <- dispatcher.getUserOrAppName
      } yield {
        permissions.checkPermissionObjectId(fileData.permissionObjectType, fileData.permissionObjectID)
        val folder = getContentFileFolder(fileData.folderId, ContentFileFolderNotFound).get
        permissionHandler.checkFolderPermissionRecursive(permissions, folder, contentRepoDao, contentFolderDao)
        checkForDuplicateFiles(fileData.folderId, fileData.name).get

        val nonEmptyDescription: String = fileData.description.filter(_.nonEmpty).getOrElse(" ")
        val fileExtension: String = FilenameUtils.getExtension(fileData.name)

        val recDate = Some(new Timestamp(new Date().getTime))
        val contentFile: ContentFile = new ContentFile(0, fileData.name, nonEmptyDescription, "Default",
          "Default", fileData.securitySchemeID, fileData.permissionObjectType, fileData.permissionObjectID, None,
          fileData.folderId, userSession.userIdOpt, recDate, Some(user.userOrAppName), None, None, Some(fileExtension))

        // Manual transaction control required, so consecutive write operations are executed in the same transaction
        // Also, if the FileIO fails, the transaction will be rolled back and the file not created
        val file = userTransaction.inSingleTransaction {
          databaseHelper.insertDatabaseEntry(contentFile)
            .flatMap(insertedFile => loadFileIO(contentFile, folder).transform(cio => Try(cio, insertedFile), e => throw e))
            .flatMap { case (fileIO, updFile) => databaseHelper.updateExternalStorageFieldFileDefinition(contentFile, fileIO).transform(_ => Try(fileIO, updFile), e => throw e) }
            .map[ContentFile] { case (fio, upFile) => Await.result(fio.create(dataResolved, upFile), timeout) }.get
        }.get
        // This will silently fail, if the stream has already been consumed,
        // but will drain the connection stream to make it responsive for the next connection otherwise
        dataResolved.runWith(Sink.ignore)
        logger.debug(slotMessageEnd(ACTION_SLOT_ADD, ASPECT_FILE, PROVIDER_NAME, file))
        ContentFileAddResponse(file.id, file.fileName, addFileSuccess)
      }).recoverWith {
        case e: Throwable =>
          // This will silently fail, if the stream has already been consumed,
          // but will drain the connection stream to make it responsive for the next connection otherwise
          dataResolved.runWith(Sink.ignore)
          throw e
      }
    }

  }

  /**
    * Get file data
    *
    * @param json          the json received from the REST call
    * @param userSession   the implicit user session
    * @param requestSource the implicit request source.
    * @return file information
    */
  override def getFile(json: JValue)(implicit userSession: UserSession, requestSource: RequestSource, ec: ExecutionContext): Future[ContentFileGetResponse] = {
    logger.debug(slotMessageBegin(ACTION_SLOT_GET, ASPECT_FILE, PROVIDER_NAME, json))
    Future(extractWithCustomError[ContentFileGetRequest](json)).flatMap { fileData =>
      Future(permissionsOpt.get).recoverWith { case _ => permissionHandler.checkPermissions() }.map(permissions => (fileData, permissions))
    }.flatMap { case (fileData, permissions) =>
      val (file, binData) = {
        databaseHelper.getDatabaseEntry(fileData.id).flatMap { cf =>
          Try(cf.map(f => {
            permissions.checkPermissionObjectId(f)
            permissions.checkPermissionToAccessFile(f, allowPublicAccess = true)
            val folder: ContentFolder = getContentFileFolder(f.folderID, ContentFileFolderNotFound).get
            logAndReturn[ContentFolder](folder, logger.trace, contentActionTraceEnd[ContentFolder](createContentFolderMetaContent, folder, ASPECT_FOLDER, ACTION_READ))
            permissionHandler.checkFolderPermissionRecursive(permissions, folder, contentRepoDao, contentFolderDao)
            val repository: ContentRepository = contentRepoDao.getById(folder.contentId).getOrElse(throw ContentRepoNotFound)
            (loadFileIO(f, folder), repository, folder, f)
          }))
        }.flatMap(res =>
          Try(res.map {
            case (cfIo, repo, cf, f) => (cfIo, (cfIo.get.read(f), repo, cf, f))
          })).flatMap(res =>
          Try(res.map {
            case (cfIo, (ss, repo, _, f)) =>
              val url: String = getUrl(getFilePath(repo, f))
              val urlWithToken: Option[String] = userSession.tokenOpt.map(tok => s"$url?SimplifierToken=$tok")
              val metaData = this.getMetadata(f.id, permissions)
              (ContentFileWithUrl(f, mimeMappingController.getMimeTypeAndExtensionForDownload(f.fileName), s"$url/",
                urlWithToken, metaData.length),
                cfIo.get.readStreamSource(ss.get, f))
          })).get
      }.getOrElse(throw ContentFileNotFound)

      binData.map(data => (encodeB64(data), data)).recoverWith { case _ => Future.failed(ContentFileEncoding) }.map {
        case (binDataEncoded, _) =>
          logger.debug(slotMessageEnd(ACTION_SLOT_GET, ASPECT_FILE, PROVIDER_NAME, file))
          ContentFileGetResponse(file.contentFile.id, file.contentFile.folderID, file.contentFile.fileName, file.contentFile.fileDescription,
            file.contentFile.statusSchemeID, file.contentFile.statusID, file.contentFile.securitySchemeID, file.contentFile.permissionObjectType,
            file.contentFile.permissionObjectID, file.mimeType, file.url, file.urlWithToken, binDataEncoded, file.length, getFileSuccess,
            file.contentFile.recDate, file.contentFile.recUser, file.contentFile.chgDate, file.contentFile.chgUser)
      }
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
                              (implicit userSession: UserSession, requestSource: RequestSource, ec: ExecutionContext): Future[ContentFileGetMetadataResponse] = {
    logger.debug(slotMessageBegin(ACTION_SLOT_GET_METADATA, ASPECT_FILE, PROVIDER_NAME, json))
    for {
      fileData <- Future(extractWithCustomError[ContentFileGetRequest](json))
      permissions <- Future(permissionsOpt.get).recoverWith { case _ => permissionHandler.checkPermissions() }
    } yield {
      val file = getMetadata(fileData.id, permissions)

      logger.debug(slotMessageEnd(ACTION_SLOT_GET_METADATA, ASPECT_FILE, PROVIDER_NAME, file))
      ContentFileGetMetadataResponse(file.contentFile.id, file.contentFile.folderID, file.contentFile.fileName, file.contentFile.fileDescription,
        file.contentFile.statusSchemeID, file.contentFile.statusID, file.contentFile.securitySchemeID, file.contentFile.permissionObjectType,
        file.contentFile.permissionObjectID, file.mimeType, file.url, file.urlWithToken, getFileMetadataSuccess, file.contentFile.recDate,
        file.contentFile.recUser, file.contentFile.chgDate, file.contentFile.chgUser, file.length)
    }
  }

  /**
    * Edit file
    *
    * @param json        the json received from the REST call
    * @param userSession the implicit user session
    * @return success message
    */
  override def editFile(json: JValue)(implicit userSession: UserSession, requestSource: RequestSource, ec: ExecutionContext): Future[ApiMessage] = {
    logger.debug(slotMessageBegin(ACTION_SLOT_EDIT, ASPECT_FILE, PROVIDER_NAME, json))
    Future(extractWithCustomError[ContentFileEditRequest](json)).flatMap { fileData =>
      resolveData(fileData).map((fileData, _))
    }.flatMap { case (fileData, dataResolved) =>
      checkContentFileParams(fileData.name, fileData.securitySchemeID, fileData.permissionObjectType, fileData.permissionObjectID)
      (for {
        permissions <- Future(permissionsOpt.get).recoverWith { case _ => permissionHandler.checkPermissions() }
        user <- dispatcher.getUserOrAppName
      } yield {
        permissions.checkPermissionObjectId(fileData.permissionObjectType, fileData.permissionObjectID)
        val file: ContentFile = getContentFile(fileData.id, ContentFileNotFound).get
        permissions.checkPermissionObjectId(file)
        permissions.checkPermissionToAccessFile(file, allowPublicAccess = false)
        val folder: ContentFolder = getContentFileFolder(file.folderID, ContentFileFolderNotFound).get
        permissionHandler.checkFolderPermissionRecursive(permissions, folder, contentRepoDao, contentFolderDao)


        // Check if another file with the same name already exists in the folder
        if (fileData.name != file.fileName) checkForDuplicateFiles(file.folderID, fileData.name)
        val nonEmptyDescription: String = fileData.description.filter(_.nonEmpty).getOrElse(" ")

        // Manual transaction control required, so consecutive write operations are executed in the same transaction
        // Also, if the FileIO fails, the transaction will be rolled back and the file not created
        userTransaction.inSingleTransaction {

          logger.trace(contentOldTrace[ContentFile](createContentFileMetaContent, file, ASPECT_FILE))
          file.fileName = fileData.name
          file.fileDescription = nonEmptyDescription
          file.securitySchemeID = fileData.securitySchemeID
          file.permissionObjectType = fileData.permissionObjectType
          file.permissionObjectID = fileData.permissionObjectID
          file.chgUser = Some(user.userOrAppName)
          file.chgDate = Some(new Timestamp(new Date().getTime))

          Try(contentFileDao.update(file)).
            flatMap(updatedFile => loadFileIO(file, folder)
              .transform(cio => Try(cio, updatedFile), e => throw e))
            .flatMap { case (fileIO, updFile) => databaseHelper.updateExternalStorageFieldFileDefinition(file, fileIO)
              .transform(_ => Try(fileIO, updFile), e => throw e)
            }
            .map[ContentFile] { case (fio, upFile) => Await.result(fio.overwrite(dataResolved, upFile), timeout) }.get
        }.get
        // This will silently fail, if the stream has already been consumed,
        // but will drain the connection stream to make it responsive for the next connection otherwise
        dataResolved.runWith(Sink.ignore)
        logger.debug(slotMessageEnd(ACTION_SLOT_EDIT, ASPECT_FILE, PROVIDER_NAME))
        ContentFileEditResponse(editFileSuccess)
      }).recoverWith {
        case NonFatal(e) =>
          // This will silently fail, if the stream has already been consumed,
          // but will drain the connection stream to make it responsive for the next connection otherwise
          dataResolved.runWith(Sink.ignore)
          throw e
      }
    }
  }

  /**
    * Delete file
    *
    * @param json        the json received from the REST call
    * @param userSession the implicit user session
    * @return success message
    */
  override def deleteFile(json: JValue)(implicit userSession: UserSession,
                                        requestSource: RequestSource,
                                        ec: ExecutionContext): Future[ContentFileDeleteResponse] = {
    logger.debug(slotMessageBegin(ACTION_SLOT_DELETE, ASPECT_FILE, PROVIDER_NAME, json))
    for {
      fileData <- Future(extractWithCustomError[ContentFileDeleteRequest](json))
      permissions <- Future(permissionsOpt.get).recoverWith { case _ => permissionHandler.checkPermissions() }
    } yield {
      val file: ContentFile = getContentFile(fileData.id, ContentFileNotFound).get
      permissions.checkPermissionObjectId(file)
      permissions.checkPermissionToAccessFile(file, allowPublicAccess = false)
      val folder: ContentFolder = getContentFileFolder(file.folderID, ContentFileFolderNotFound).get
      permissionHandler.checkFolderPermissionRecursive(permissions, folder, contentRepoDao, contentFolderDao)

      loadFileIO(file, folder)
        .transform(cio => Try(cio), e => throw e)
        .flatMap(f => f.delete(file))
        .flatMap(databaseHelper.deleteDatabaseEntry).get

      logger.debug(slotMessageEnd(ACTION_SLOT_DELETE, ASPECT_FILE, PROVIDER_NAME))
      ContentFileDeleteResponse(deleteFileSuccess)
    }
  }

  /**
    * Find files within a folder by name
    *
    * @param json          the json received from the REST call
    * @param userSession   the implicit user session
    * @param requestSource the implicit request source.
    * @return array of file with given name within given folder
    */
  override def findFile(json: JValue)
                       (implicit userSession: UserSession, requestSource: RequestSource, ec: ExecutionContext): Future[ContentFileFindResponse] = {
    logger.debug(slotMessageBegin(ACTION_SLOT_FIND, ASPECT_FILE, PROVIDER_NAME, json))
    for {
      fileData <- Future(extractWithCustomError[ContentFileFindRequest](json))
      permissions <- Future(permissionsOpt.get).recoverWith { case _ => permissionHandler.checkPermissions() }
    } yield {
      val folder: ContentFolder = getContentFileFolder(fileData.folderId, ContentFileFolderNotFound).get
      val repository: ContentRepository = contentRepoDao.getById(folder.contentId).getOrElse(throw ContentRepoNotFound)
      permissionHandler.checkFolderPermissionRecursive(permissions, folder, contentRepoDao, contentFolderDao)
      val files: Option[ContentFileWithUrl] = databaseHelper.getDatabaseEntry(fileData.folderId, fileData.name, permissions).get.flatMap(cf => {
        Try {
          permissions.checkPermissionToAccessFile(cf, allowPublicAccess = true)
          val url: String = getUrl(getFilePath(repository, cf))
          val urlWithToken: Option[String] = userSession.tokenOpt.map(tok => s"$url?SimplifierToken=$tok")
          val metaData = getMetadata(cf.id, permissions)
          ContentFileWithUrl(cf,
            mimeMappingController.getMimeTypeAndExtensionForDownloadByExtension(cf.extension.getOrElse("")), s"$url/",
            urlWithToken, metaData.length)
        }.toOption
      })
      logger.debug(slotMessageEnd(ACTION_SLOT_FIND, ASPECT_FILE, PROVIDER_NAME, files))
      ContentFileFindResponse(files.toSeq map (file => ContentFileFindResponseItem(file.contentFile.id, file.contentFile.fileName,
        file.contentFile.fileDescription, file.contentFile.statusSchemeID, file.contentFile.statusID, file.contentFile.securitySchemeID,
        file.contentFile.permissionObjectType, file.contentFile.permissionObjectID, file.contentFile.folderID, file.mimeType, file.url, file.urlWithToken,
        file.contentFile.recDate, file.contentFile.recUser, file.contentFile.chgDate, file.contentFile.chgUser, file.length
      )), listFileSuccess)
    }
  }

  /**
    * List files within a folder. If no folder is given all files are listed.
    *
    * @param json          the json received from the REST call
    * @param userSession   the implicit user session
    * @param requestSource the implicit request source.
    * @return list of files
    */
  override def listFiles(json: JValue)(implicit userSession: UserSession, requestSource: RequestSource, ec: ExecutionContext): Future[ApiMessage] = {
    logger.debug(slotMessageBegin(ACTION_SLOT_LIST, ASPECT_FILE, PROVIDER_NAME, json))
    Future(extractWithCustomError[ContentFileListRequest](json)).flatMap { fileData =>
      fileData.folderId match {
        case None => listAllOwnContentFiles.map((ContentFileListResponseAll(_, listFileSuccess)))
        case Some(id) =>
          listContentFiles(id).map { files =>
            files map (file =>
              mapContentFileModel(file.contentFile, file.mimeType, file.url, file.urlWithToken, file.length))
          }.map { files =>
            ContentFileListResponse(files, listFileSuccess)
          }
      }
    }.andThen { case Success(response) =>
      logger.debug(slotMessageEnd(ACTION_SLOT_LIST, ASPECT_FILE, PROVIDER_NAME, response))
    }

  }

  private def listAllOwnContentFiles(implicit userSession: UserSession, requestSource: RequestSource, ec: ExecutionContext): Future[Option[Seq[Repository]]] = {
    val reposFoldersFilesFuture: Future[Seq[(Option[ContentRepoListResponseItem], Seq[(ContentFolderGetResponseForAll, Seq[ContentFileListResponseItem])])]] = {
      contentRepoController.listContentRepositories(None).flatMap { repos =>
        Future.sequence(repos.map {
          repo =>
            contentFolderController.listAllFoldersForRepository(repo.id).map { folders =>
              folders.map(folder =>
                listContentFiles(folder.id).map { files =>
                  (folder, files.map(file => mapContentFileModel(file.contentFile, file.mimeType, file.url, file.urlWithToken, file.length)))
                }
              )
            }.flatMap { folderAndFiles =>
              Future.sequence(folderAndFiles).map { folderAndFiles =>
                (contentRepoController.mapContentRepoModelToListResponseItem(Option(repo)), folderAndFiles)
              }
            }
        })
      }
    }
    reposFoldersFilesFuture.map { reposFoldersFiles =>
      Option(reposFoldersFiles.map { case (repos, folders) => Repository(repos, Option(folders.map { case (folder, files) =>
        Folder(folder, Option(files).filter(_.nonEmpty))
      }).filter(_.nonEmpty))
      }).filter(_.nonEmpty)
    }
  }

  private def listContentFiles(folderId: Int)
                              (implicit userSession: UserSession, requestSource: RequestSource, ec: ExecutionContext): Future[Seq[ContentFileWithUrl]] = {
    Future(permissionsOpt.get).recoverWith { case _ => permissionHandler.checkPermissions() }.map { permissions =>
      val folder: ContentFolder = getContentFileFolder(folderId, ContentFileFolderNotFound).get
      val repository: ContentRepository = contentRepoDao.getById(folder.contentId).getOrElse(throw ContentRepoNotFound)
      permissionHandler.checkFolderPermissionRecursive(permissions, folder, contentRepoDao, contentFolderDao)

      databaseHelper.listDatabaseEntries(folderId, permissions).get.flatMap { cf =>
        Try {
          permissions.checkPermissionToAccessFile(cf, allowPublicAccess = true)
          val url: String = getUrl(getFilePath(repository, cf))
          val urlWithToken: Option[String] = userSession.tokenOpt.map(tok => s"$url?SimplifierToken=$tok")
          val basicMetaData = getMetadata(cf.id, permissions)
          ContentFileWithUrl(cf, mimeMappingController.getMimeTypeAndExtensionForDownloadByExtension(cf.extension.getOrElse("")),
            s"$url/", urlWithToken, basicMetaData.length)
        }.toOption
      }
    }
  }

  /**
    * Load file IO
    *
    * @param file   file
    * @param folder folder
    * @return file IO
    */
  def loadFileIO(file: ContentFile, folder: ContentFolder)
                (implicit ec: ExecutionContext): Try[ContentFileIO] = {
    val repo = contentRepoDao.getById(folder.contentId).getOrElse(throw ContentFileRepoNotFound)
    fileSystemProvider.resolveFileSystem(file, folder, repo.name)
  }

  /**
    * Get public file by id
    *
    * @param id file id
    * @return file data
    */
  def getPublicFile(id: Int)(implicit ec: ExecutionContext): Option[PublicFileData] = {
    //TODO  SimplifierLogEntry

    databaseHelper.getDatabaseEntry(id)
      .flatMap(file => Try(file.filter(_.securitySchemeID == SecurityScheme.Public.name)
        .flatMap { publicFile =>
          val folder: Option[ContentFolder] = contentFolderDao.getById(publicFile.folderID)
          folder.filter(isFolderPublicRecursive).map(publicFolder => loadFileIO(publicFile, publicFolder)
            .fold(e => throw e,
              fileIO => Try(file.get).flatMap(fileIO.read)
                .fold(e => throw e,
                  ss => Try(normalizeExtension(FilenameUtils.getExtension(publicFile.fileName)))
                    .fold(e => throw e,
                      extension => mimeMappingDao.findByExt(extension)
                        .fold(e => throw e,
                          mt => convertPossibleMimeToBinary(mt.map(_.mimeType))
                            .fold(e => throw e,
                              contentType => PublicFileData(publicFile.fileName, ss, contentType))
                        )))))
        })).get
  }

  private def checkContentFileParams(fileName: String, securitySchemeID: String,
                                     permissionObjectType: String, permissionObjectID: String): Unit = {
    checkNotEmpty(fileName, ContentFileEmptyFilename)
    checkNotEmpty(permissionObjectType, ContentFileEmptyPermissionObjectType)
    checkNotEmpty(permissionObjectID, ContentFileEmptyPermissionObjectId)
    checkNotEmpty(securitySchemeID, ContentFileEmptySecuritySchemeID)
    checkSecuritySchemeExists(securitySchemeID, ContentFileInvalidSecuritySchemeID)
  }

  private def resolveData(source: FileDataSource)(implicit userSession: UserSession,
                                                  requestSource: RequestSource,
                                                  ec: ExecutionContext): Future[ByteSource] = {
    Future(dataDecoded(source.data).get)
      .recoverWith {
        case _ =>
          dataUploadFetched(source.uploadSession)
      }.recoverWith {
      case _ =>
        dataCopyFromResolved(source.copyFrom).map(_.get)
    }.recoverWith {
      case _ =>
        Future.failed(ContentFileMissingData)
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
  override def resolveContentFileToStream(repoName: String, parentFolderNames: Seq[String], folderName: String,
                                          fileName: String)(implicit userSession: UserSession,
                                                            requestSource: RequestSource,
                                                            ec: ExecutionContext): Future[Option[Try[StreamSource]]] = {
    Future(permissionsOpt.get).recoverWith { case _ => permissionHandler.checkPermissions() }.map { permissions =>
      resolveContentFile(repoName, parentFolderNames, folderName, fileName, permissions) map {
        case (contentFile, contentFolder) =>

          //TODO  SimplifierLogEntry
          loadFileIO(contentFile, contentFolder)
            .fold(e => throw e, fileIO => fileIO.read(contentFile))
      }
    }
  }

  /**
    * Resolved data of file to be copied.
    *
    * @param copyFrom    file id of the file to be copied
    * @param userSession the implicit user session
    * @return byte source
    */
  def dataCopyFromResolved(copyFrom: Option[Int])(implicit userSession: UserSession,
                                                  requestSource: RequestSource,
                                                  ec: ExecutionContext): Future[Option[ByteSource]] = {
    copyFrom.map {
      fileId =>
        Future(permissionsOpt.get).recoverWith { case _ => permissionHandler.checkPermissions() }.map { permissions =>
          val file: ContentFile = getContentFile(fileId, ContentFileNotFound).get
          permissions.checkPermissionObjectId(file)
          val folder: ContentFolder = getContentFileFolder(file.folderID, ContentFileFolderNotFound).get
          permissionHandler.checkFolderPermissionRecursive(permissions, folder, contentRepoDao, contentFolderDao)

          val stream = for {
            fileIO <- loadFileIO(file, folder)
            streamSource <- fileIO.read(file)
            stream <- streamSource.stream()
          } yield {
            stream
          }
          Some(stream.get)
        }
    }.getOrElse(Future.successful(None))
  }

  @tailrec
  private def getFolderSeqRecursive(contentFolder: ContentFolder, retSeq: Seq[String]): Seq[String] = {
    if (contentFolder.parentFolderId.isEmpty) {
      retSeq
    } else {
      val parent: ContentFolder = contentFolderDao.getById(contentFolder.parentFolderId.get).getOrElse(throw ContentFolderRetrievalFailure())
      getFolderSeqRecursive(parent, Seq(parent.folderName) ++ retSeq)
    }
  }

  private def getFilePath(contentRepo: ContentRepository, contentFile: ContentFile): Seq[String] = {
    val parentFolder = contentFolderDao.getById(contentFile.folderID).getOrElse(throw ContentFolderRetrievalFailure())
    Seq(contentRepo.name) ++
    getFolderSeqRecursive(parentFolder, Seq(parentFolder.folderName)) ++
    Seq(contentFile.fileName)
  }

  override protected def getMetadataBatched(json: JValue)
                                           (implicit userSession: UserSession, requestSource: RequestSource, ec: ExecutionContext): Future[Seq[GetFileMetadataBatchedResponseItem]] = {
    Future(permissionsOpt.get).recoverWith { case _ => permissionHandler.checkPermissions() }.map { permissions =>
      extractWithCustomError[ContentFileGetMetadataBatchedRequest](json).files.map {
        file =>
          val metadata = getMetadata(file.id, permissions)
          ContentFileGetMetadataBatchedResponseItem(metadata.contentFile.id,
            metadata.contentFile.folderID,
            metadata.contentFile.fileName,
            metadata.contentFile.fileDescription,
            metadata.contentFile.statusSchemeID,
            metadata.contentFile.statusID,
            metadata.contentFile.securitySchemeID,
            metadata.contentFile.permissionObjectType,
            metadata.contentFile.permissionObjectID,
            metadata.mimeType,
            metadata.url,
            metadata.urlWithToken,
            metadata.contentFile.recDate,
            metadata.contentFile.recUser,
            metadata.contentFile.chgDate,
            metadata.contentFile.chgUser)
      }
    }
  }

  def getMetadata(id: Int, permissions: ContentRepoPermissions)(implicit userSession: UserSession,
                                                                        requestSource: RequestSource): ContentFileWithUrl = {
    databaseHelper.getDatabaseEntry(id).map { cf =>
      cf.map(f => {
        permissions.checkPermissionObjectId(f)
        permissions.checkPermissionToAccessFile(f, allowPublicAccess = true)
        val folder: ContentFolder = getContentFileFolder(f.folderID, ContentFileFolderNotFound).get
        logAndReturn[ContentFolder](folder, logger.trace, contentActionTraceEnd[ContentFolder](
          createContentFolderMetaContent, folder, ASPECT_FOLDER, ACTION_READ)
        )
        permissionHandler.checkFolderPermissionRecursive(permissions, folder, contentRepoDao, contentFolderDao)
        val repository: ContentRepository = contentRepoDao.getById(folder.contentId).getOrElse(throw ContentRepoNotFound)
        val filePath = getFilePath(repository, f)
        val url: String = getUrl(filePath)
        val urlWithToken: Option[String] = userSession.tokenOpt.map(tok => s"$url?SimplifierToken=$tok")
        val fileSystemPath = fileSystemProvider.resolveFile(f, folder, repository.name)
        val basicMetaData = Files.getFileAttributeView(fileSystemPath.toPath, classOf[BasicFileAttributeView])
        ContentFileWithUrl(f, mimeMappingController.getMimeTypeAndExtensionForDownload(f.fileName), url, urlWithToken,
          basicMetaData.readAttributes().size())
      })
    }.get.getOrElse(throw ContentFileNotFound)
  }
}