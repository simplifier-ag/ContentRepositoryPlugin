package io.simplifier.plugin.contentrepo.controller.folder

import io.simplifier.plugin.contentrepo.dao.{ContentFolderDao, ContentRepositoryDao}
import io.simplifier.plugin.contentrepo.definitions.Constants._
import io.simplifier.plugin.contentrepo.definitions.LogMessages._
import io.simplifier.plugin.contentrepo.definitions.caseClasses.ContentFolderCaseClasses._
import io.simplifier.plugin.contentrepo.dto.RestMessages._
import io.simplifier.plugin.contentrepo.model.ContentRepository
import io.simplifier.plugin.contentrepo.model.provider.ClearFileSystemProvider
import io.simplifier.plugin.contentrepo.permission.PermissionHandler
import io.simplifier.pluginapi.UserSession
import io.simplifier.pluginapi.rest.PluginHeaders.RequestSource
import org.json4s._

import scala.concurrent.{ExecutionContext, Future}

/**
  * Implementation of the ContentFolderController for the clear file system provider
  */
class ClearFileSystemFolderController(provider: ClearFileSystemProvider,
                                      repo: ContentRepository,
                                      permissionHandler: PermissionHandler,
                                      contentRepoDao: ContentRepositoryDao = new ContentRepositoryDao,
                                      contentFolderDao: ContentFolderDao = new ContentFolderDao
                                     ) extends ContentFolderController(permissionHandler, contentRepoDao, contentFolderDao) {

  val PROVIDER_NAME: String = "ClearFileSystem"

  /**
    * Add folder to file system
    *
    * @param json the json received from the REST call
    * @param userSession the implicit user session
    * @return folder path
    */
  override def addFolder(json: JValue)(implicit userSession: UserSession,
                                       requestSource: RequestSource,
                                       ec: ExecutionContext): Future[ClearContentFolderAddResponse] = {
    logger.debug(slotMessageBegin(ACTION_SLOT_ADD, ASPECT_FOLDER, PROVIDER_NAME, json))
    permissionHandler.checkPermissions().map { _ =>
      val folderData = extractWithCustomError[ClearContentFolderAddRequest](json)
      addFolder(folderData)
    }
  }

  def addFolder(folderData: ClearContentFolderAddRequest)(implicit userSession: UserSession,
                                                        requestSource: RequestSource,
                                                        ec: ExecutionContext): ClearContentFolderAddResponse = {
    val path = provider.createFolder(folderData.name, Some(repo.name), folderData.parentFolderPath)
    logger.debug(slotMessageEnd(ACTION_SLOT_ADD, ASPECT_FOLDER, PROVIDER_NAME, path))
    ClearContentFolderAddResponse(path, addFolderSuccess)
  }

  override def addFolderIfNotExisting(json: JValue)(implicit userSession: UserSession,
                                       requestSource: RequestSource,
                                       ec: ExecutionContext): Future[ClearContentFoldersAddResponse] = {
    logger.debug(slotMessageBegin(ACTION_SLOT_ADD, ASPECT_FOLDER, PROVIDER_NAME, json))
    permissionHandler.checkPermissions().map { _ =>
      val folderData = extractWithCustomError[ClearContentFoldersAddRequest](json)
      ClearContentFoldersAddResponse(folderData.name.map { folderName =>
        addFolder(ClearContentFolderAddRequest(folderData.contentId, folderData.parentFolderPath, folderName))
      }.map(_.path), addFolderSuccess)
    }
  }

  /**
    * Get folder by path
    *
    * @param json the json received from the REST call
    * @param userSession the implicit user session
    * @return folder path
    */
  override def getFolder(json: JValue)(implicit userSession: UserSession,
                                       requestSource: RequestSource,
                                       ec: ExecutionContext): Future[ClearContentFolderGetResponse] = {
    logger.debug(slotMessageBegin(ACTION_SLOT_GET, ASPECT_FOLDER, PROVIDER_NAME, json))
    permissionHandler.checkPermissions().map { _ =>
      val folderData = extractWithCustomError[ClearContentFolderGetRequest](json)
      provider.validateFolderExists(repo.name, folderData.folderPath)
      logger.debug(slotMessageEnd(ACTION_SLOT_GET, ASPECT_FOLDER, PROVIDER_NAME, folderData.folderPath))
      ClearContentFolderGetResponse(folderData.folderPath, getFolderSuccess)
    }
  }

  /**
    * Edit folder name and/or move folder
    *
    * @param json the json received from the REST call
    * @param userSession the implicit user session
    * @return success message
    */
  override def editFolder(json: JValue)(implicit userSession: UserSession,
                                        requestSource: RequestSource,
                                        ec: ExecutionContext): Future[ContentFolderEditResponse] = {
    logger.debug(slotMessageBegin(ACTION_SLOT_EDIT, ASPECT_FOLDER, PROVIDER_NAME, json))
    permissionHandler.checkPermissions().map { _ =>
      val folderData = extractWithCustomError[ClearContentFolderEditRequest](json)
      provider.editFolder(repo.name, folderData.sourceFolderPath, folderData.destFolderPath)
      logger.debug(slotMessageEnd(ACTION_SLOT_EDIT, ASPECT_FOLDER, PROVIDER_NAME))
      ContentFolderEditResponse(editFolderSuccess)
    }
  }

  /**
    * Delete folder by path
    *
    * @param json the json received from the REST call
    * @param userSession the implicit user session
    * @return success message
    */
  override def deleteFolder(json: JValue)(implicit userSession: UserSession,
                                          requestSource: RequestSource,
                                          ec: ExecutionContext): Future[ContentFolderDeleteResponse] = {
    logger.debug(slotMessageBegin(ACTION_SLOT_DELETE, ASPECT_FOLDER, PROVIDER_NAME, json))
    permissionHandler.checkPermissions().map { _ =>
      val folderData = extractWithCustomError[ClearContentFolderDeleteRequest](json)
      provider.deleteFolder(repo.name, Some(folderData.folderPath), folderData.forceDelete)
      logger.debug(slotMessageEnd(ACTION_SLOT_DELETE, ASPECT_FOLDER, PROVIDER_NAME))
      ContentFolderDeleteResponse(deleteFolderSuccess)
    }
  }

  /**
    * Find folders by name within a folder.
    * If no folder is given the whole repo is searched.
    *
    * @param json the json received from the REST call
    * @param userSession the implicit user session
    * @return list folder paths
    */
  override def findFolder(json: JValue)(implicit userSession: UserSession,
                                        requestSource: RequestSource,
                                        ec: ExecutionContext): Future[ContentFolderFindResponse] = {
    logger.debug(slotMessageBegin(ACTION_SLOT_FIND, ASPECT_FOLDER, PROVIDER_NAME, json))
    permissionHandler.checkPermissions().map { _ =>
      val folderData = extractWithCustomError[ClearContentFolderFindRequest](json)
      val responseItems = provider.findFolder(repo.name, folderData.name, folderData.parentFolderPath).map(ClearContentFolderFindResponseItem)
      logger.debug(slotMessageEnd(ACTION_SLOT_FIND, ASPECT_FOLDER, PROVIDER_NAME, responseItems))
      ContentFolderFindResponse(responseItems, listFolderSuccess)
    }
  }

  /**
    * List folders within a parent folder.
    * If no parent folder is given all first level folders of the repo are listed.
    *
    * @param json the json received from the REST call
    * @param userSession the implicit user session
    * @return list of folder names
    */
  override def listFolders(json: JValue)(implicit userSession: UserSession,
                                         requestSource: RequestSource,
                                         ec: ExecutionContext): Future[ContentFolderListResponse] = {
    logger.debug(slotMessageBegin(ACTION_SLOT_LIST, ASPECT_FOLDER, PROVIDER_NAME, json))
    permissionHandler.checkPermissions().map { _ =>
      val folderData = extractWithCustomError[ClearContentFolderListRequest](json)

      val responseItems = provider.listFolders(repo.name, folderData.parentFolderPath).map(ClearContentFolderListResponseItem)
      logger.debug(slotMessageEnd(ACTION_SLOT_LIST, ASPECT_FOLDER, PROVIDER_NAME, responseItems))
      ContentFolderListResponse(responseItems, listFolderSuccess)
    }

  }
}
