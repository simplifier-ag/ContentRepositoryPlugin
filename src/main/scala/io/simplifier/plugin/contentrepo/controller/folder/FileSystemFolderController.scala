package io.simplifier.plugin.contentrepo.controller.folder

import io.simplifier.plugin.contentrepo.controller.BaseController.ContentRepoPermissions
import io.simplifier.plugin.contentrepo.controller.repository.ContentRepositoryController
import io.simplifier.plugin.contentrepo.dao.{ContentFileDao, ContentFolderDao, ContentRepositoryDao}
import io.simplifier.plugin.contentrepo.definitions.Constants._
import io.simplifier.plugin.contentrepo.definitions.LogMessages._
import io.simplifier.plugin.contentrepo.definitions.caseClasses.ContentFolderCaseClasses._
import io.simplifier.plugin.contentrepo.definitions.exceptions.ContentFolderExceptions._
import io.simplifier.plugin.contentrepo.definitions.exceptions.ContentRepositoryExceptions.ContentRepoRetrievalFailure
import io.simplifier.plugin.contentrepo.dto.RestMessages._
import io.simplifier.plugin.contentrepo.model.{ContentFolder, ContentRepository}
import io.simplifier.plugin.contentrepo.permission.PermissionHandler
import io.simplifier.pluginapi.UserSession
import io.simplifier.pluginapi.rest.PluginHeaders.RequestSource
import io.simplifier.pluginbase.util.api.ApiMessage
import org.json4s._

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Success, Try}

/**
  * Implementation of the ContentFolderController for the file system provider
  */
class FileSystemFolderController(repoController: ContentRepositoryController,
                                 permissionHandler: PermissionHandler,
                                 permissionsOpt: Option[ContentRepoPermissions],
                                 contentRepoDao: ContentRepositoryDao = new ContentRepositoryDao,
                                 contentFolderDao: ContentFolderDao = new ContentFolderDao,
                                 contentFileDao: ContentFileDao = new ContentFileDao
                                ) extends ContentFolderController(permissionHandler, contentRepoDao, contentFolderDao) {

  val PROVIDER_NAME: String = "FileSystem"

  /**
    * Add new folder to file system and database.
    *
    * @param json        the json received from the REST call
    * @param userSession the implicit user session
    * @return folder information
    */
  def addFolder(folderData: ContentFolderAddRequest)(implicit userSession: UserSession,
                                       requestSource: RequestSource,
                                       ec: ExecutionContext): Future[ContentFolderAddResponse] = {
    for {
      _ <- Future(checkContentFolderParams(folderData.name, folderData.securitySchemeID, folderData.permissionObjectType, folderData.permissionObjectID))
      permissions <- Future(permissionsOpt.get).recoverWith { case _ => permissionHandler.checkPermissions() }
    } yield {
      permissions.checkPermissionObjectId(folderData.permissionObjectType, folderData.permissionObjectID)

      val repo: ContentRepository = Try(contentRepoDao.getById(folderData.contentId)).fold(e =>
        throw ContentRepoRetrievalFailure.initCause(e), res => res).getOrElse(throw ContentFolderRepoNotFound)

      if (folderData.parentFolderId.isDefined) {
        val parent: ContentFolder = getContentFolder(folderData.parentFolderId.get, ContentFolderParentNotFound)
        if (parent.contentId != folderData.contentId) throw ContentFolderParentInWrongRepo
        permissionHandler.checkFolderPermissionRecursive(permissions, parent, contentRepoDao, contentFolderDao)
      } else {
        permissions.checkPermissionObjectId(repo)
      }

      // Check if folder with same name already exists
      checkForDuplicateFolders(folderData.contentId, folderData.name, folderData.parentFolderId).get

      val newFolder: ContentFolder = new ContentFolder(0,
        folderData.name,
        sanitizeDescription(folderData.description),
        "Default",
        "Default",
        folderData.securitySchemeID,
        "Default",
        folderData.permissionObjectType,
        folderData.permissionObjectID,
        folderData.parentFolderId,
        folderData.contentId)

      //TODO Error in Simplifier Log
      val folder: ContentFolder = Try(contentFolderDao.insert(newFolder))
        .fold(e => throw e,
          res => logAndReturn[ContentFolder](res, logger.trace, contentActionTraceEnd[ContentFolder](createContentFolderMetaContent, newFolder,
            ASPECT_FOLDER, ACTION_INSERT)))

      logger.debug(slotMessageEnd(ACTION_SLOT_ADD, ASPECT_FOLDER, PROVIDER_NAME, folder))
      ContentFolderAddResponse(folder.id, folder.folderName, folder.folderDescription, addFolderSuccess)
    }
  }

  override def addFolder(json: JValue)(implicit userSession: UserSession,
                              requestSource: RequestSource, ec: ExecutionContext): Future[ContentFolderAddResponse] = {
    logger.debug(slotMessageBegin(ACTION_SLOT_ADD, ASPECT_FOLDER, PROVIDER_NAME, json))
    val folderData = extractWithCustomError[ContentFolderAddRequest](json)
    addFolder(folderData)
  }

  def addFolderIfNotExisting(json: JValue)(implicit userSession: UserSession,
                                       requestSource: RequestSource, ec: ExecutionContext): Future[ContentFoldersAddResponse] = {
    val folderData = extractWithCustomError[ContentFoldersAddRequest](json)
    val createdFolderFutures = folderData.contentFolders.map { folder =>
      addFolder(folder).map(i => Seq(i)) recoverWith {
        case _ => for {
           findResponse <- findFolder(ContentFolderFindRequest(folder.contentId, folder.name, folder.parentFolderId))
        } yield {
          findResponse.folders.filter {
            case e: ContentFolderFindResponseItem => e.name == folder.name
          }.map {
            case e: ContentFolderFindResponseItem => ContentFolderAddResponse(e.id, e.name, e.description, findResponse.message, findResponse.success)
          }
        }
      }
    }
    for {
      createdFolders <- Future.sequence(createdFolderFutures)
    } yield {
      ContentFoldersAddResponse(createdFolders.flatten)
    }
  }

  /**
    * Get folder by id
    *
    * @param json        the json received from the REST call
    * @param userSession the implicit user session
    * @return folder information
    */
  override def getFolder(json: JValue)(implicit userSession: UserSession,
                                       requestSource: RequestSource,
                                       ec: ExecutionContext): Future[ContentFolderGetResponse] = {
    logger.debug(slotMessageBegin(ACTION_SLOT_GET, ASPECT_FOLDER, PROVIDER_NAME, json))
    for {
      folderData <- Future(extractWithCustomError[ContentFolderGetRequest](json))
      permissions <- permissionHandler.checkPermissions()
    } yield {
      val folder: ContentFolder = Try(contentFolderDao.getById(folderData.id)).fold(e =>
        throw ContentFolderRetrievalFailure().initCause(e), res => res map { folder =>
        permissionHandler.checkFolderPermissionRecursive(permissions, folder, contentRepoDao, contentFolderDao)
        folder
      }).getOrElse(throw ContentFolderNotFound)
      logger.debug(slotMessageEnd(ACTION_SLOT_GET, ASPECT_FOLDER, PROVIDER_NAME, folder))
      ContentFolderGetResponse(folder.id, folder.parentFolderId, folder.folderName, folder.folderDescription, folder.statusSchemeID, folder.statusID,
        folder.securitySchemeID, folder.currentStatus, folder.permissionObjectType, folder.permissionObjectID, getFolderSuccess)
    }
  }

  /**
    * Edit folder data
    *
    * @param json        the json received from the REST call
    * @param userSession the implicit user session
    * @return success message
    */
  override def editFolder(json: JValue)(implicit userSession: UserSession,
                                        requestSource: RequestSource,
                                        ec: ExecutionContext): Future[ContentFolderEditResponse] = {
    logger.debug(slotMessageBegin(ACTION_SLOT_EDIT, ASPECT_FOLDER, PROVIDER_NAME, json))
    for {
      folderData <- Future(extractWithCustomError[ContentFolderEditRequest](json)).andThen { case Success(folderData) =>
        checkContentFolderParams(folderData.name, folderData.securitySchemeID, folderData.permissionObjectType, folderData.permissionObjectID)
      }
      permissions <- Future(permissionsOpt.get).recoverWith { case _ => permissionHandler.checkPermissions() }
    } yield {
      permissions.checkPermissionObjectId(folderData.permissionObjectType, folderData.permissionObjectID)

      val contentFolder: ContentFolder = getContentFolder(folderData.id, ContentFolderNotFound)

      permissionHandler.checkFolderPermissionRecursive(permissions, contentFolder, contentRepoDao, contentFolderDao)
      // Check if folder with same name already exists (which is not this folder itself)
      if (folderData.name != contentFolder.folderName) {
        checkForDuplicateFolders(contentFolder.contentId, folderData.name, contentFolder.parentFolderId).get
        contentFolder.folderName = folderData.name
      }

      logger.trace(contentOldTrace[ContentFolder](createContentFolderMetaContent, contentFolder, ASPECT_FOLDER))
      contentFolder.folderDescription = sanitizeDescription(folderData.description)
      contentFolder.securitySchemeID = folderData.securitySchemeID
      contentFolder.permissionObjectType = folderData.permissionObjectType
      contentFolder.permissionObjectID = folderData.permissionObjectID

      //TODO Error in Simplifier Log
      val folder: ContentFolder = Try(contentFolderDao.update(contentFolder)).fold(e => throw e,
        res => logAndReturn[ContentFolder](res, logger.trace, contentActionTraceEnd[ContentFolder](createContentFolderMetaContent, contentFolder, ASPECT_FOLDER, ACTION_EDIT)))

      logger.debug(slotMessageEnd(ACTION_SLOT_EDIT, ASPECT_FOLDER, PROVIDER_NAME, folder))
      ContentFolderEditResponse(editFolderSuccess)
    }

  }

  /**
    * Delete folder by id
    *
    * @param json        the json received from the REST call
    * @param userSession the implicit user session
    * @return success message
    */
  override def deleteFolder(json: JValue)(implicit userSession: UserSession,
                                          requestSource: RequestSource,
                                          ec: ExecutionContext): Future[ContentFolderDeleteResponse] = {
    logger.debug(slotMessageBegin(ACTION_SLOT_DELETE, ASPECT_FOLDER, PROVIDER_NAME, json))
    Future(permissionsOpt.get).recoverWith { case _ => permissionHandler.checkPermissions() }.map { permissions =>
      val folderData = extractWithCustomError[ContentFolderDeleteRequest](json)

      val folder: ContentFolder = getContentFolder(folderData.id, ContentFolderNotFound)
      permissionHandler.checkFolderPermissionRecursive(permissions, folder, contentRepoDao, contentFolderDao)
      val filesAreNonEmpty: Boolean = contentFileDao.getAllByFolder(folderData.id).fold(e => throw e, res => res.nonEmpty)
      val foldersAreNonEmpty: Boolean = contentFolderDao.getAllByParent(folderData.id).fold(e => throw e, res => res.nonEmpty)

      (filesAreNonEmpty, foldersAreNonEmpty) match {
        case (true, true) => throw ContentFolderNotEmptyFoldersAndFiles
        case (true, false) => throw ContentFolderNotEmptyFiles
        case (false, true) => throw ContentFolderNotEmptyFolders
        case (false, false) => //TODO Error in Simplifier Log
          Try(contentFolderDao.delete(folder)).fold(e => throw e,
            _ => logAndReturn[ContentFolder](folder, logger.trace, contentActionTraceEnd[ContentFolder](createContentFolderMetaContent, folder, ASPECT_FOLDER, ACTION_DELETE)))
      }

      logger.debug(slotMessageEnd(ACTION_SLOT_DELETE, ASPECT_FOLDER, PROVIDER_NAME))
      ContentFolderDeleteResponse(deleteFolderSuccess)
    }

  }

  /**
    * Find folder with given name within the given repo
    *
    * @param json        the json received from the REST call
    * @param userSession the implicit user session
    * @return list of folders
    */
  def findFolder(folderData: ContentFolderFindRequest)(implicit userSession: UserSession,
                                                                requestSource: RequestSource,
                                                                ec: ExecutionContext): Future[ContentFolderFindResponse] = {
    Future(permissionsOpt.get).recoverWith { case _ => permissionHandler.checkPermissions() }.map { permissions =>
      val folders: Option[ContentFolder] = contentFolderDao.findByNameAndParent(folderData.contentId, folderData.name, folderData.parentFolderId)
        .fold(e => throw ContentFolderRetrievalFailure().initCause(e), res => res) filter (folder =>
        permissionHandler.hasFolderPermissionRecursive(permissions, folder, contentRepoDao, contentFolderDao))

      logger.debug(slotMessageEnd(ACTION_SLOT_FIND, ASPECT_FOLDER, PROVIDER_NAME, folders))
      ContentFolderFindResponse(folders.toSeq map (folder =>
        ContentFolderFindResponseItem(folder.id, folder.folderName, folder.folderDescription,
          folder.statusSchemeID, folder.statusID, folder.securitySchemeID, folder.currentStatus,
          folder.permissionObjectType, folder.permissionObjectID)), listFolderSuccess)
    }
  }

  override def findFolder(json: JValue)(implicit userSession: UserSession,
                                        requestSource: RequestSource, ec: ExecutionContext): Future[ContentFolderFindResponse] = {
    logger.debug(slotMessageBegin(ACTION_SLOT_FIND, ASPECT_FOLDER, PROVIDER_NAME, json))
    val folderData = extractWithCustomError[ContentFolderFindRequest](json)
    findFolder(folderData)
  }

  /**
    * List folders within a folder.
    * If no folder is given all folders within the repo are listed.
    * If no repo is given all folders of all repos are listed.
    *
    * @param json        the json received from the REST call
    * @param userSession the implicit user session
    * @return list of folders
    */
  override def listFolders(json: JValue)(implicit userSession: UserSession, requestSource: RequestSource, ec: ExecutionContext): Future[ApiMessage] = {
    logger.debug(slotMessageBegin(ACTION_SLOT_LIST, ASPECT_FOLDER, PROVIDER_NAME, json))
    Future(extractWithCustomError[ContentFolderListRequest](json)).flatMap { folderData =>
      (folderData.contentId, folderData.parentFolderId) match {
        case (None, None) =>
          listAllOwnContentFolders(None).map( repositories => ContentFileListResponseAll(repositories, listFolderSuccess))
        case (None, pId) =>
          listAllOwnContentFolders(pId).map( repositories => ContentFileListResponseAll(repositories, listFolderSuccess))
        case (Some(contentId), _) =>
          listContentFolders(contentId, folderData.parentFolderId).map(_.map(mapContentFolderModel)).map { folders =>
            ContentFolderListResponse(folders, listFolderSuccess)
          }
      }
    }.andThen { case Success(response) =>
      logger.debug(slotMessageEnd(ACTION_SLOT_LIST, ASPECT_FOLDER, PROVIDER_NAME, response))
    }
  }

  private def listAllOwnContentFolders(parentId: Option[Int])
                                      (implicit userSession: UserSession,
                                       requestSource: RequestSource,
                                       ec: ExecutionContext): Future[Option[Seq[Repository]]] = {
    repoController.listContentRepositories(None).flatMap { repositories =>
      parentId match {
        case None =>
          Future.sequence(repositories.map(repo =>  listAllFoldersForRepository(repo.id).map( folders => (repoController.mapContentRepoModelToListResponseItem(Option(repo)), folders))))
        case Some(id) => listAllFoldersForParent(id).map(_.groupBy(_.contentId).map { case (contentId, folders) =>
          (repoController.mapContentRepoModelToListResponseItem(repositories.find(repo => repo.id == contentId)), folders)
        }.toSeq)
      }
    }.map { reposFolders =>
      Option(reposFolders.map { case (repos, folders) => Repository(repos, Option(folders).filter(_.nonEmpty)) }).filter(_.nonEmpty)
    }
  }

  private[this] def listContentFolders(contentId: Int, parentFolderId: Option[Int])
                                      (implicit userSession: UserSession,
                                       requestSource: RequestSource,
                                       ec: ExecutionContext): Future[Seq[ContentFolder]] = {
    Future(permissionsOpt.get).recoverWith { case _ => permissionHandler.checkPermissions() }.map { permissions =>
      val folders = contentFolderDao.findAllByRepoAndParent(contentId, parentFolderId).recover { case e => throw ContentFolderRetrievalFailure(true).initCause(e) }.get
      folders filter { folder =>
        permissionHandler.hasFolderPermissionRecursive(permissions, folder, contentRepoDao, contentFolderDao)
      }
    }
  }

  private def checkContentFolderParams(name: String, securitySchemeID: String, permissionObjectType: String,
                                       permissionObjectID: String): Unit = {
    checkNotEmpty(permissionObjectType, ContentFolderEmptyPermissionObjectType)
    checkNotEmpty(permissionObjectID, ContentFolderEmptyPermissionObjectId)
    checkNotEmpty(securitySchemeID, ContentFolderEmptySecuritySchemeID)
    checkNotEmpty(name, ContentFolderEmptyName)
    checkSecuritySchemeExists(securitySchemeID, ContentFolderInvalidSecuritySchemeID)
  }

  private def getContentFolder(id: Int, exception: Throwable): ContentFolder = {
    Try(contentFolderDao.getById(id)).fold(e => throw ContentFolderRetrievalFailure().initCause(e), res => res.getOrElse(throw exception))
  }

  private def checkForDuplicateFolders(contentId: Int, name: String, parentFolderId: Option[Int]): Try[Unit] = {
    contentFolderDao.findByNameAndParent(contentId, name, parentFolderId)
      .transform(cf => Try(cf.foreach(_ => throw ContentFolderDuplicateFolderName(name))), e => throw ContentFolderRetrievalFailure().initCause(e))
  }

  private def listAllFoldersForParent(parentId: Int)(implicit userSession: UserSession,
                                                     requestSource: RequestSource,
                                                     ec: ExecutionContext): Future[Seq[ContentFolderGetResponseForAll]] = {
    Future(permissionsOpt.get).recoverWith { case _ => permissionHandler.checkPermissions() }.map { permissions =>
      contentFolderDao.getAllByParent(parentId).recover { case e => throw ContentFolderRetrievalFailure().initCause(e) }.map { res =>
        res.filter {folder =>
          permissionHandler.hasFolderPermissionRecursive(permissions, folder, contentRepoDao, contentFolderDao)}
        .map(folder => ContentFolderGetResponseForAll(folder.id, folder.parentFolderId, folder.contentId, folder.folderName, folder.folderDescription,
          folder.statusSchemeID, folder.statusID, folder.securitySchemeID, folder.currentStatus, folder.permissionObjectType, folder.permissionObjectID))
      }.get
    }
  }

  private def mapContentFolderModel(folder: ContentFolder): ContentFolderListResponseItem = {
    ContentFolderListResponseItem(folder.id, folder.folderName, folder.folderDescription, folder.statusSchemeID, folder.statusID,
      folder.securitySchemeID, folder.currentStatus, folder.permissionObjectType, folder.permissionObjectID)
  }
}
