package io.simplifier.plugin.contentrepo.controller.file

import akka.stream.Materializer
import io.simplifier.plugin.contentrepo.controller.BaseController
import io.simplifier.plugin.contentrepo.controller.BaseController.ContentRepoPermissions
import io.simplifier.plugin.contentrepo.controller.folder.FolderControllerFactory
import io.simplifier.plugin.contentrepo.controller.mimeMapping.MimeMappingControllerFactory
import io.simplifier.plugin.contentrepo.controller.repository.RepositoryControllerFactory
import io.simplifier.plugin.contentrepo.dao.ContentRepositoryDao
import io.simplifier.plugin.contentrepo.definitions.exceptions.ContentRepositoryExceptions._
import io.simplifier.plugin.contentrepo.model.ContentRepository
import io.simplifier.plugin.contentrepo.model.provider.{ClearFileSystemProvider, FileSystemProvider}
import io.simplifier.plugin.contentrepo.permission.PermissionHandler
import io.simplifier.plugin.contentrepo.pluginBaseRelated.json.JSONCompatibility.LegacySearch
import com.typesafe.config.Config
import io.simplifier.pluginapi.UserSession
import io.simplifier.pluginapi.rest.PluginHeaders.RequestSource
import io.simplifier.pluginbase.PluginDescription
import io.simplifier.pluginbase.SimplifierPlugin.AppServerInformation
import io.simplifier.pluginbase.interfaces.AppServerDispatcher
import org.json4s._

import scala.concurrent.duration.{Duration, SECONDS}
import scala.concurrent.{ExecutionContext, Future}

/**
  * Factory to discern which file controller is needed
  */
class FileControllerFactory(
                             dispatcher: AppServerDispatcher,
                             config: Config,
                             pluginDescription: PluginDescription,
                             appServerInformation: AppServerInformation,
                             mimeMappingControllerFactory: MimeMappingControllerFactory,
                             repositoryControllerFactory: RepositoryControllerFactory,
                             folderControllerFactory: FolderControllerFactory,
                             permissionHandler: PermissionHandler,
                             contentRepositoryDao: ContentRepositoryDao = new ContentRepositoryDao
                           )(implicit materializer: Materializer)
  extends BaseController {

  val fileSystemProvider = "FileSystem"

  /**
    * Get file controller belonging to the given provider
    *
    * @param json the json received from the REST call
    * @return the specific file controller
    */
  def getFileController(json: JValue, permissionsOpt: Option[ContentRepoPermissions])
                       (implicit userSession: UserSession,
                        requestSource: RequestSource,
                        ec: ExecutionContext): Future[ContentFileController] = {
    Future(permissionsOpt.get).recoverWith { case _ => permissionHandler.checkPermissions() }.flatMap { permissions =>
      LegacySearch(json) \ "contentId" match {
        case JInt(repoId) =>
          val repo = contentRepositoryDao.getById(repoId.toInt).getOrElse(throw ContentRepoNotFound)
          getFileController(repo, Some(permissions))
        case JNothing =>
          val timeoutDuration: Duration = Duration(config.getInt("plugin.timeoutSeconds"), SECONDS)
          folderControllerFactory.getFolderController(json, Some(permissions)).map { folderController =>
            new FileSystemFileController(dispatcher, pluginDescription, appServerInformation,
              new FileSystemProvider(config), mimeMappingControllerFactory.getMimeMappingController(),
              repositoryControllerFactory.getRepositoryControllerByProviderName(fileSystemProvider),
              folderController, permissionHandler, Some(permissions), timeoutDuration)
          }
        case _ => throw ContentRepoWrongDataType("contentId", "Integer")
      }
    }
  }

  /**
    * Get file controller belonging to the provider of the given repo name
    *
    * @param repoName repo name
    * @return the specific file controller
    */
  def getFileControllerByRepoName(repoName: String, permissionsOpt: Option[ContentRepoPermissions])
                                 (implicit userSession: UserSession,
                                  requestSource: RequestSource,
                                  ec: ExecutionContext): Future[ContentFileController] = {
    val repo = contentRepositoryDao.getByName(repoName).get.getOrElse(throw ContentRepoNotFound)
    getFileController(repo, permissionsOpt)
  }

  private def getFileController(repo: ContentRepository, permissionsOpt: Option[ContentRepoPermissions])
                               (implicit userSession: UserSession,
                                requestSource: RequestSource,
                                ec: ExecutionContext): Future[ContentFileController] = {
    Future(permissionsOpt.get).recoverWith { case _ => permissionHandler.checkPermissions() }.flatMap { permissions =>
      permissions.checkPermissionObjectId(repo)
      repo.provider match {
        case FileSystemProvider.providerId =>
          val timeoutDuration: Duration = Duration(config.getInt("plugin.timeoutSeconds"), SECONDS)
          folderControllerFactory.getFolderControllerByRepoName(repo.name, Some(permissions)).map { folderController =>
            new FileSystemFileController(dispatcher, pluginDescription, appServerInformation,
              new FileSystemProvider(config), mimeMappingControllerFactory.getMimeMappingController(),
              repositoryControllerFactory.getRepositoryControllerByProviderName(repo.provider),
              folderController, permissionHandler, Some(permissions), timeoutDuration)
          }
        case ClearFileSystemProvider.providerId =>
          Future.successful(new ClearFileSystemFileController(dispatcher, pluginDescription,
            appServerInformation, new ClearFileSystemProvider(config), mimeMappingControllerFactory.getMimeMappingController(), repo, permissionHandler))
        case _ => Future.failed(ContentRepoUnknownProvider)
      }
    }
  }
}