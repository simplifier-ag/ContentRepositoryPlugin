package io.simplifier.plugin.contentrepo.controller.folder

import akka.actor.ActorSystem
import akka.stream.Materializer
import io.simplifier.plugin.contentrepo.controller.BaseController
import io.simplifier.plugin.contentrepo.controller.BaseController.ContentRepoPermissions
import io.simplifier.plugin.contentrepo.controller.repository.RepositoryControllerFactory
import io.simplifier.plugin.contentrepo.dao.ContentRepositoryDao
import io.simplifier.plugin.contentrepo.definitions.exceptions.ContentRepositoryExceptions.{ContentRepoNotFound, ContentRepoUnknownProvider, ContentRepoWrongDataType}
import io.simplifier.plugin.contentrepo.model.ContentRepository
import io.simplifier.plugin.contentrepo.model.provider.{ClearFileSystemProvider, FileSystemProvider}
import io.simplifier.plugin.contentrepo.permission.PermissionHandler
import io.simplifier.plugin.contentrepo.pluginBaseRelated.json.JSONCompatibility.LegacySearch
import com.typesafe.config.Config
import io.simplifier.pluginapi.UserSession
import io.simplifier.pluginapi.rest.PluginHeaders.RequestSource
import org.json4s._

import scala.concurrent.{ExecutionContext, Future}

/**
  * Factory to discern which folder controller is needed
  */
class FolderControllerFactory(
                               config: Config,
                               repositoryControllerFactory: RepositoryControllerFactory,
                               permissionHandler: PermissionHandler,
                               contentRepositoryDao: ContentRepositoryDao = new ContentRepositoryDao)
                             (implicit system: ActorSystem, materializer: Materializer)
extends BaseController {

  val fileSystemProvider = "FileSystem"

  /**
    * Get the folder controller belonging to the given provider
    *
    * @param json the json received from the REST call
    * @return the specific folder controller
    */
  def getFolderController(json: JValue, permissionsOpt: Option[ContentRepoPermissions])
                         (implicit userSession: UserSession, requestSource: RequestSource, ec: ExecutionContext): Future[ContentFolderController] = {
    Future(permissionsOpt.get).recoverWith {
      case _ => permissionHandler.checkPermissions()
    }.flatMap { permissions =>
      LegacySearch(json) \ "contentId" match {
        case JInt(repoId) =>
          Future(contentRepositoryDao.getById(repoId.toInt).getOrElse(throw ContentRepoNotFound)).flatMap { repo =>
            getFolderController(repo, Some(permissions))
          }
        case JNothing =>
          Future.successful(new FileSystemFolderController(repositoryControllerFactory.getRepositoryControllerByProviderName(fileSystemProvider),
            permissionHandler, Some(permissions)))
        case _ => Future.failed(ContentRepoWrongDataType("contentId", "Integer"))
      }
    }
  }

  /**
    * Get the folder controller belonging to the provider of the given repo name
    *
    * @param repoName repo name
    * @return the specific folder controller
    */
  def getFolderControllerByRepoName(repoName: String, permissionsOpt: Option[ContentRepoPermissions])
                                   (implicit userSession: UserSession, requestSource: RequestSource, ec: ExecutionContext): Future[ContentFolderController] = {
    Future(contentRepositoryDao.getByName(repoName).get.getOrElse(throw ContentRepoNotFound)).flatMap( repo =>
      getFolderController(repo, permissionsOpt)
    )
  }

  private def getFolderController(repo: ContentRepository, permissionsOpt: Option[ContentRepoPermissions])
                                 (implicit userSession: UserSession, requestSource: RequestSource, ec: ExecutionContext): Future[ContentFolderController] = {
    Future(permissionsOpt.get).recoverWith {
      case _ => permissionHandler.checkPermissions()
    }.map { permissions =>
      permissions.checkPermissionObjectId(repo)
      repo.provider match {
        case FileSystemProvider.providerId =>
          new FileSystemFolderController(repositoryControllerFactory.getRepositoryControllerByProviderName(repo.provider),
            permissionHandler, Some(permissions))
        case ClearFileSystemProvider.providerId =>
          new ClearFileSystemFolderController(new ClearFileSystemProvider(config), repo, permissionHandler)
        case _ => throw ContentRepoUnknownProvider
      }
    }
  }
}
