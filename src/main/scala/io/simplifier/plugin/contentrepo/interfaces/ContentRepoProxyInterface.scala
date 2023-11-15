package io.simplifier.plugin.contentrepo.interfaces

import akka.actor.ActorSystem
import akka.http.scaladsl.model._
import akka.http.scaladsl.model.headers._
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.stream.Materializer
import io.simplifier.plugin.contentrepo.controller.file.FileControllerFactory
import io.simplifier.plugin.contentrepo.controller.folder.FolderControllerFactory
import io.simplifier.plugin.contentrepo.controller.mimeMapping.{MimeMappingController, MimeMappingControllerFactory}
import io.simplifier.plugin.contentrepo.controller.repository.RepositoryControllerFactory
import io.simplifier.plugin.contentrepo.permission.PermissionHandler
import com.typesafe.config.Config
import io.simplifier.pluginapi.UserSession
import io.simplifier.pluginapi.rest.PluginHeaders
import io.simplifier.pluginbase.SimplifierPlugin.AppServerInformation
import io.simplifier.pluginbase.interfaces.{AppServerDispatcher, ProxyInterfaceService}
import io.simplifier.pluginbase.util.api.PredefinedApiFailures.NotFoundFailure
import io.simplifier.pluginbase.util.logging.Logging
import io.simplifier.pluginbase.{PluginDescription, PluginSettings}

import scala.util.{Failure, Success}


class ContentRepoProxyInterface(
                                 mimeMappingController: MimeMappingController,
                                 fileControllerFactory: FileControllerFactory
                               )
  extends ProxyInterfaceService with Logging {
  /** Base-URL relative to http service root */
  override val baseUrl: String = "api"

  /**
    * Plugin-specific inner route handling proxy requests
    *
    * @param requestSource plugin request source
    * @param userSession   user session
    * @return service route
    */
  override def serviceRoute(implicit requestSource: PluginHeaders.RequestSource, userSession: UserSession): Route =
    pathPrefix(ContentRepoProxyInterface.FILE_PREFIX) {
      path(Segment / Segments(min = 2, max = 128)) { (repo, segments) =>
        extractExecutionContext { implicit ec =>
          val (firstSegments, lastTwo) = segments.toVector.splitAt(segments.length - 2)
          val (parentFolders, folder, fileName) = (firstSegments, lastTwo(0), lastTwo(1))
          get {
            optionalHeaderValueByName("Range") {
              case None =>
                logger.trace("Providing whole file")
                onComplete(fileControllerFactory.getFileControllerByRepoName(repo, None)
                  .flatMap(_.resolveContentFileToStream(repo, parentFolders, folder, fileName).map(_.getOrElse(throw NotFoundFailure())))) {
                  case Success(streamSource) =>
                    val contentType = resolveContentType(mimeMappingController.getMimeTypeForDownload(fileName))
                    complete {
                      HttpEntity.Default(contentType, streamSource.get.length.get, streamSource.get.stream().get)
                      //HttpEntity.Chunked(contentType, streamSource.length, streamSource.stream())}
                    }
                  case Failure(e) =>
                    complete(e)
                }
              case Some(range) =>
                logger.trace(s"Received range $range")
                onComplete(fileControllerFactory.getFileControllerByRepoName(repo, None)
                  .flatMap(_.resolveContentFileToStream(repo, parentFolders, folder, fileName).map(_.getOrElse(throw NotFoundFailure())))) {
                  case Success(streamSource) =>
                    complete {
                      val fileSize = streamSource.get.length.get
                      logger.trace(s"fileSize: $fileSize")

                      val rng = range.split("=")(1).split("-")
                      val start = rng(0).toInt
                      val end = if (rng.length > 1) {
                        rng(1).toLong
                      } else {
                        logger.trace("no end range found")
                        fileSize - 1L
                      }

                      logger.trace(s"start: $start, end: $end")
                      val contentType: ContentType = resolveContentType(mimeMappingController.getMimeTypeForDownload(fileName))
                      logger.trace(s"resovled contentType: $contentType")
                      val x = streamSource.get
                      val content = x.chunk(start, (end - start + 1).toInt).get
                      val entity = HttpEntity(contentType, end - start + 1, content)
                      logger.trace(s"entity length: ${entity.contentLength}")

                      HttpResponse(StatusCodes.PartialContent)
                        .addHeader(`Accept-Ranges`(RangeUnits.Bytes))
                        .addHeader(`Content-Range`(ContentRange(start, end, fileSize)))
                        .withEntity(entity)
                    }
                  case Failure(e) =>
                    complete(e)
                }

            }

          }
        }

      }
    }

  private def resolveContentType(mimeType: Option[String]): ContentType = mimeType match {
    case None => ContentTypes.`application/octet-stream`
    case Some(mime) => ContentType(MediaType.custom(mime, binary = true).asInstanceOf[MediaType.Binary])
  }
}

object ContentRepoProxyInterface {
  def apply(dispatcher: AppServerDispatcher, pluginSettings: PluginSettings, pluginDescription: PluginDescription,
            appServerInformation: AppServerInformation, config: Config)
           (implicit system: ActorSystem, materializer: Materializer): ContentRepoProxyInterface = {
    val permissionHandler = new PermissionHandler(dispatcher, pluginSettings)
    val mimeMappingController = new MimeMappingController(permissionHandler)
    val mimeMappingControllerFactory = new MimeMappingControllerFactory(permissionHandler)
    val repositoryControllerFactory = new RepositoryControllerFactory(config, permissionHandler)
    val folderControllerFactory = new FolderControllerFactory(config, repositoryControllerFactory, permissionHandler)
    new ContentRepoProxyInterface(
      mimeMappingController,
      new FileControllerFactory(
        dispatcher,
        config,
        pluginDescription,
        appServerInformation,
        mimeMappingControllerFactory,
        repositoryControllerFactory,
        folderControllerFactory,
        permissionHandler
      )
    )
  }

  final val FILE_PREFIX: String = "file"
}