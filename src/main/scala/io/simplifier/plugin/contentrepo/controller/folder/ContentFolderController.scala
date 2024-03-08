package io.simplifier.plugin.contentrepo.controller.folder

import io.simplifier.plugin.contentrepo.controller.BaseController
import io.simplifier.plugin.contentrepo.dao.{ContentFolderDao, ContentRepositoryDao}
import io.simplifier.plugin.contentrepo.definitions.caseClasses.ContentFolderCaseClasses.ContentFolderGetResponseForAll
import io.simplifier.plugin.contentrepo.definitions.exceptions.ContentFolderExceptions.ContentFolderRetrievalFailure
import io.simplifier.plugin.contentrepo.permission.PermissionHandler
import io.simplifier.pluginapi.UserSession
import io.simplifier.pluginapi.rest.PluginHeaders.RequestSource
import io.simplifier.pluginbase.util.api.ApiMessage
import org.json4s._

import scala.concurrent.{ExecutionContext, Future}


/**
  * Controller for Content Folders.
  */
abstract class ContentFolderController(permissionHandler: PermissionHandler,
                                       contentRepoDao: ContentRepositoryDao = new ContentRepositoryDao,
                                       contentFolderDao: ContentFolderDao = new ContentFolderDao)
  extends BaseController {

  val PROVIDER_NAME: String

  /**
    * Base function to add folder
    * Implemented in the provider specific controllers
    *
    * @param json        the json received from the REST call
    * @param userSession the implicit user session
    * @return
    */
  def addFolder(json: JValue)(implicit userSession: UserSession, requestSource: RequestSource, ec: ExecutionContext): Future[ApiMessage]

  /**
    * Add folder if not already existing. No error is thrown if folder already exists.
    * Implemented in the provider specific controllers
    *
    * @param json the json received from the REST call
    * @param userSession the implicit user session
    * @return
    */
  def addFolderIfNotExisting(json: JValue)(implicit userSession: UserSession, requestSource: RequestSource, ec: ExecutionContext): Future[ApiMessage]

  /**
    * Base function to get folder information
    * Implemented in the provider specific controllers
    *
    * @param json        the json received from the REST call
    * @param userSession the implicit user session
    * @return
    */
  def getFolder(json: JValue)(implicit userSession: UserSession, requestSource: RequestSource, ec: ExecutionContext): Future[ApiMessage]

  /**
    * Base function to edit folder
    * Implemented in the provider specific controllers
    *
    * @param json        the json received from the REST call
    * @param userSession the implicit user session
    * @return
    */
  def editFolder(json: JValue)(implicit userSession: UserSession, requestSource: RequestSource, ec: ExecutionContext): Future[ApiMessage]

  /**
    * Base function to delete folder
    * Implemented in the provider specific controllers
    *
    * @param json        the json received from the REST call
    * @param userSession the implicit user session
    * @return
    */
  def deleteFolder(json: JValue)(implicit userSession: UserSession, requestSource: RequestSource, ec: ExecutionContext): Future[ApiMessage]

  /**
    * Base function to find folders
    * Implemented in the provider specific controllers
    *
    * @param json        the json received from the REST call
    * @param userSession the implicit user session
    * @return
    */
  def findFolder(json: JValue)(implicit userSession: UserSession, requestSource: RequestSource, ec: ExecutionContext): Future[ApiMessage]

  /**
    * Base function to list folders
    * Implemented in the provider specific controllers
    *
    * @param json        the json received from the REST call
    * @param userSession the implicit user session
    * @return
    */
  def listFolders(json: JValue)(implicit userSession: UserSession, requestSource: RequestSource, ec: ExecutionContext): Future[ApiMessage]

  protected[controller] def listAllFoldersForRepository(contentId: Int)
                                                       (implicit userSession: UserSession,
                                                        requestSource: RequestSource,
                                                        ec: ExecutionContext): Future[Seq[ContentFolderGetResponseForAll]] = {
    permissionHandler.checkPermissions().map { permissions =>
      contentFolderDao.getAllByRepository(contentId).fold(e => throw ContentFolderRetrievalFailure().initCause(e),
        res => res filter (folder => permissionHandler.hasFolderPermissionRecursive(permissions, folder, contentRepoDao, contentFolderDao)))
        .map(folder => ContentFolderGetResponseForAll(folder.id, folder.parentFolderId, folder.contentId, folder.folderName, folder.folderDescription,
          folder.statusSchemeID, folder.statusID, folder.securitySchemeID, folder.currentStatus, folder.permissionObjectType, folder.permissionObjectID))
    }
  }

}