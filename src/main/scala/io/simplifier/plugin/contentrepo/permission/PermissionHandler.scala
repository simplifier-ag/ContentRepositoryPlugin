package io.simplifier.plugin.contentrepo.permission

import akka.http.scaladsl.model.StatusCodes
import io.simplifier.plugin.contentrepo.controller.BaseController.{ContentRepoPermissions, OperationFailureMessage}
import io.simplifier.plugin.contentrepo.dao.{ContentFolderDao, ContentRepositoryDao}
import io.simplifier.plugin.contentrepo.definitions.exceptions.ContentFolderExceptions.{ContentFolderParentNotFound, ContentFolderRepoNotFound, ContentFolderRetrievalFailure}
import io.simplifier.plugin.contentrepo.definitions.exceptions.ContentRepositoryExceptions.ContentRepoRetrievalFailure
import io.simplifier.plugin.contentrepo.model.{ContentFolder, ContentRepository}
import io.simplifier.pluginapi.UserSession
import io.simplifier.pluginapi.rest.PluginHeaders.RequestSource
import io.simplifier.pluginbase.PluginSettings
import io.simplifier.pluginbase.interfaces.AppServerDispatcher
import io.simplifier.pluginbase.permission.PluginPermissionObject

import scala.annotation.tailrec
import scala.concurrent.duration.DurationInt
import scala.concurrent.{Await, ExecutionContext, Future}
import scala.util.Try

class PermissionHandler(appServerDispatcher: AppServerDispatcher, pluginSettings: PluginSettings) {
  import PermissionHandler._
  /**
    * Perform permission check for users (require plugin permission object!)
    * and return permission data holder for additional checks.
    */
  def checkPermissions()(implicit pluginUserSession: UserSession, requestSource: RequestSource, ec: ExecutionContext): Future[ContentRepoPermissions] = {
    loadPermissions().map { permissions =>
      if (!permissions.hasContentRepoPermission) {
        throw PermissionDeniedMissingPermissionObject
      }
      permissions
    }

  }

  def checkAdditionalPermission(characteristic: String)(implicit pluginUserSession: UserSession, requestSource: RequestSource, ec: ExecutionContext): Unit = {
    Await.result(loadPermissions().map { permissions =>
      permissions.checkAdditionalPermission(characteristic)
    }, 1.second)
  }

  /**
    * Check a folder and all parent folders and the parent repo for granted permissions.
    * If permissions are not all granted, throw a message exception.
    */
  @tailrec
  final def checkFolderPermissionRecursive(permissions: ContentRepoPermissions, folder: ContentFolder,
                                           repoDao: ContentRepositoryDao, folderDao: ContentFolderDao): Unit = {
    permissions.checkPermissionObjectId(folder)
    if (folder.parentFolderId.isEmpty) {
      val repo: ContentRepository = Try(repoDao.getById(folder.contentId))
        .fold(e => throw ContentRepoRetrievalFailure.initCause(e), res => res)
        .getOrElse(throw ContentFolderRepoNotFound)
      permissions.checkPermissionObjectId(repo)
    } else {
      val parent: ContentFolder = Try(folderDao.getById(folder.parentFolderId.get))
        .fold(e => throw ContentFolderRetrievalFailure().initCause(e), res => res)
        .getOrElse(throw ContentFolderParentNotFound)
      checkFolderPermissionRecursive(permissions, parent, repoDao, folderDao)
    }
  }

  /**
    * Check a folder and all parent folders and the parent repo for granted permissions.
    *
    * @return true if folder and all parent folder, and the parent repo has valid permissions
    */
  @tailrec
  final def hasFolderPermissionRecursive(permissions: ContentRepoPermissions, folder: ContentFolder,
                                         repoDao: ContentRepositoryDao, folderDao: ContentFolderDao): Boolean = {
    if (!permissions.hasPermissionObjectId(folder)) {
      false
    } else {
      if (folder.parentFolderId.isEmpty) {
        val repo: ContentRepository = Try(repoDao.getById(folder.contentId))
          .fold(e => throw ContentRepoRetrievalFailure.initCause(e), res => res)
          .getOrElse(throw ContentFolderRepoNotFound)
        permissions.hasPermissionObjectId(repo)
      } else {
        val parent: ContentFolder = Try(folderDao.getById(folder.parentFolderId.get))
          .fold(e => throw ContentFolderRetrievalFailure().initCause(e), res => res)
          .getOrElse(throw ContentFolderParentNotFound)
        hasFolderPermissionRecursive(permissions, parent, repoDao, folderDao)
      }
    }
  }

  /**
    * Load Content Repo permissions for user. If user is not authenticated, no permissions are granted by default.
    *
    * @param userSession User session
    * @return content repo permissions
    */
  protected def loadPermissions()(implicit userSession: UserSession, requestSource: RequestSource, ec: ExecutionContext): Future[ContentRepoPermissions] = {
    PluginPermissionObject.loadPermissions(ContentRepoPermission.technicalName, appServerDispatcher).map( getPermissionsResult =>
        new ContentRepoPermissions(getPermissionsResult.permissionObjects)
      )
  }
}

object PermissionHandler {
  def PermissionDeniedMissingPermissionObject = OperationFailureMessage("Permission Denied: PermissionObject " +
    ContentRepoPermission.technicalName + " not granted", StatusCodes.Forbidden)

  def PermissionDeniedMissingCharacteristic(characteristic: String) =
    OperationFailureMessage("Permission Denied: Permission characteristic '" + characteristic + "' of PermissionObject " +
      ContentRepoPermission.technicalName + " not granted", StatusCodes.Forbidden)

  def PermissionDeniedInsufficientPrivileges(userId: Option[Long]) =
    OperationFailureMessage(s"Permission denied: User ${userId.getOrElse("")} has insufficient privileges to access file")

  def PermissionDeniedMissingObjectId(objectType: String, objectId: String) =
    OperationFailureMessage("Permission Denied: Permission (" + objectType + " = " + objectId + ") of PermissionObject " +
      ContentRepoPermission.technicalName + " not granted", StatusCodes.Forbidden)
}
