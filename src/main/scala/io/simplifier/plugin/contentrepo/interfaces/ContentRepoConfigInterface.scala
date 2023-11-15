package io.simplifier.plugin.contentrepo.interfaces

import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.stream.Materializer
import io.simplifier.plugin.contentrepo.controller.file.FileSystemFileController
import io.simplifier.plugin.contentrepo.controller.folder.FileSystemFolderController
import io.simplifier.plugin.contentrepo.controller.mimeMapping.MimeMappingController
import io.simplifier.plugin.contentrepo.controller.repository.FileSystemRepositoryController
import io.simplifier.plugin.contentrepo.definitions.caseClasses.ContentFileCaseClasses.PublicFileData
import io.simplifier.plugin.contentrepo.model.provider.{ClearFileSystemProvider, FileSystemProvider}
import io.simplifier.plugin.contentrepo.permission.PermissionHandler
import com.typesafe.config.Config
import io.simplifier.pluginapi.UserSession
import io.simplifier.pluginapi.rest.PluginHeaders.RequestSource
import io.simplifier.pluginbase.SimplifierPlugin.AppServerInformation
import io.simplifier.pluginbase.interfaces.{AppServerDispatcher, DefaultConfigurationInterfaceService}
import io.simplifier.pluginbase.util.api.PredefinedApiFailures.IdNotFoundFailure
import io.simplifier.pluginbase.util.logging.Logging
import io.simplifier.pluginbase.{PluginDescription, PluginSettings}
import io.swagger.annotations._

import javax.ws.rs.{Path, QueryParam}
import scala.concurrent.duration.{Duration, SECONDS}
import scala.util.matching.Regex

class ContentRepoConfigInterface(contentFileController: FileSystemFileController)
  extends DefaultConfigurationInterfaceService("assets", "assets/", Seq.empty) with Logging {

  val downloadPattern: Regex = "([0-9]+)\\.[a-zA-Z0-9]+".r

  /** Service route to deliver assets */
  override def serviceRoute(implicit requestSource: RequestSource, userSession: UserSession): Route = {
    super.serviceRoute ~
    path("download" / downloadPattern) { fileIdStr =>
      extractExecutionContext { implicit ec =>
        val fileId = fileIdStr.toInt
        logger.debug(s"Requested download fileId $fileId")
        contentFileController.getPublicFile(fileId) match {
          case None => throw IdNotFoundFailure(fileId, "fileId")
          case Some(fileData) =>
            complete {
              fileData
            }
        }
      }
    }
  }
}

object ContentRepoConfigInterface {
  def apply(dispatcher: AppServerDispatcher, pluginSettings: PluginSettings, pluginDescription: PluginDescription,
            appServerInformation: AppServerInformation, config: Config)
           (implicit materializer: Materializer): ContentRepoConfigInterface = {
    val timeoutDuration: Duration = Duration(config.getInt("plugin.timeoutSeconds"), SECONDS)
    val fileSystemProvider = new FileSystemProvider(config)
    val clearFileSystemProvider = new ClearFileSystemProvider(config)
    val permissionHandler = new PermissionHandler(dispatcher, pluginSettings)
    val mimeMappingController = new MimeMappingController(permissionHandler)
    val repoController = new FileSystemRepositoryController(clearFileSystemProvider, permissionHandler)
    val folderController = new FileSystemFolderController(repoController, permissionHandler, None)
    val fileController = new FileSystemFileController(
      dispatcher,
      pluginDescription,
      appServerInformation,
      fileSystemProvider,
      mimeMappingController,
      repoController,
      folderController,
      permissionHandler,
      None,
      timeoutDuration
    )
    new ContentRepoConfigInterface(fileController)
  }

  @Api(tags = Array("Download"), authorizations = Array(new Authorization("basicAuth")))
  @ApiResponses(Array(new ApiResponse(code = 401, message = "Unauthorized")))
  @Path("/client/2.0/pluginSlot/contentRepoPlugin/")
  trait DownloadDocumentation {
    @ApiOperation(httpMethod = "POST", value="Download a file from repository.")
    @Path("/download/{fileId}")
    def download(
      @ApiParam(value="The id of the requested file.")
      @QueryParam("fileId") fileId: Long
    ): PublicFileData
  }

}
