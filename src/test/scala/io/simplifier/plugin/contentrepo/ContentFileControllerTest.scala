package io.simplifier.plugin.contentrepo

import akka.http.scaladsl.model.Uri
import io.simplifier.plugin.contentrepo.contentRepoIo.StreamSource
import io.simplifier.plugin.contentrepo.controller.file.ContentFileController
import io.simplifier.plugin.contentrepo.dao._
import io.simplifier.plugin.contentrepo.definitions.caseClasses.ContentFileCaseClasses
import io.simplifier.pluginapi.UserSession
import io.simplifier.pluginapi.rest.PluginHeaders.{AppServer, RequestSource}
import io.simplifier.pluginbase.PluginDescription
import io.simplifier.pluginbase.SimplifierPlugin.AppServerInformation
import io.simplifier.pluginbase.interfaces.AppServerDispatcher
import io.simplifier.pluginbase.util.api.ApiMessage
import org.json4s._
import org.mockito.MockitoSugar
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try

class ContentFileControllerTest extends AnyWordSpec with Matchers {

  "A ContentFileController" when {
    "getting the url" should {
      "return the correct URL for localhost" in new GetUrlLocalhostFixture {
        controller.getUrl(filePathSegments) shouldBe s"$baseURL$route/$pluginName/$filePrefix/$filePath"
      }
      "return the correct URL for an instance" in new GetUrlInstanceFixture {
        controller.getUrl(filePathSegments) shouldBe s"$baseURL$route/$pluginName/$filePrefix/$filePath"
      }
    }
  }


  trait BaseFixture extends MockitoSugar {
    implicit val userSession: UserSession = UserSession.unauthenticated
    val dispatcher: AppServerDispatcher = mock[AppServerDispatcher]
    val pluginDescription: PluginDescription = mock[PluginDescription]
    val appServerInformation: AppServerInformation = mock[AppServerInformation]
    val contentFileDao: ContentFileDao = mock[ContentFileDao]
    val contentFolderDao: ContentFolderDao = mock[ContentFolderDao]
    val contentRepositoryDao: ContentRepositoryDao = mock[ContentRepositoryDao]

    val controller = new ControllerMock( dispatcher, pluginDescription,
      appServerInformation, contentFileDao, contentFolderDao, contentRepositoryDao)

    val pluginName = "contentRepo"
    val filePath = "this/is/the/path/to/the/file.foo"
    val filePathSegments: Seq[String] = Seq("this", "is", "the", "path","to","the","file.foo")
    val route = "client/2.0/plugin"
    val filePrefix = "file"

    when(pluginDescription.name).thenReturn(pluginName)
    when(appServerInformation.routes).thenReturn(None)
  }

  trait GetUrlLocalhostFixture extends BaseFixture {
    val baseURL = "http://localhost:8080/"
    implicit val requestSource: RequestSource = AppServer(Some(Uri(baseURL)))

    when(appServerInformation.prefix).thenReturn(None)
  }

  trait GetUrlInstanceFixture extends BaseFixture {
    val baseURL = "https://this.is.an.instance/"
    implicit val requestSource: RequestSource = AppServer(Some(Uri(baseURL)))

    when(appServerInformation.prefix).thenReturn(Some(baseURL))
  }


class ControllerMock(appServerDispatcher: AppServerDispatcher,
                     pluginDescription: PluginDescription,
                     appServerInformation: AppServerInformation,
                     contentFileDao: ContentFileDao = new ContentFileDao,
                     contentFolderDao: ContentFolderDao = new ContentFolderDao,
                     contentRepoDao: ContentRepositoryDao = new ContentRepositoryDao)
  extends ContentFileController(appServerDispatcher, pluginDescription,
    appServerInformation, contentFileDao, contentFolderDao, contentRepoDao) {
  override val PROVIDER_NAME: String = "TestProvider"

  /**
    * Base function to add file
    * Implemented in the provider specific controllers
    *
    * @param json        the json received from the REST call
    * @param userSession the implicit user session
    * @return depends on the implementation
    */
  override def addFile(json: JValue)(implicit userSession: UserSession, requestSource: RequestSource, ec: ExecutionContext): Future[ApiMessage] = ???

  /**
    * Base function to get file data
    * Implemented in the provider specific controllers
    *
    * @param json          the json received from the REST call
    * @param userSession   the implicit user session
    * @param requestSource the implicit request source.
    * @return depends on the implementation
    */
  override def getFile(json: JValue)(implicit userSession: UserSession, requestSource: RequestSource, ec: ExecutionContext): Future[ApiMessage] = ???

  /**
    * Base function to get only the file metadata without the raw file data
    *
    * @param json          the json received from the REST call
    * @param userSession   the implicit user session
    * @param requestSource the implicit request source.
    * @return depends on the implementation
    */
  override def getFileMetadata(json: JValue)(implicit userSession: UserSession, requestSource: RequestSource, ec: ExecutionContext): Future[ApiMessage] = ???

  override protected def getMetadataBatched(json: JValue)(implicit userSession: UserSession, requestSource: RequestSource, ec: ExecutionContext): Future[Seq[ContentFileCaseClasses.GetFileMetadataBatchedResponseItem]] = ???

  /**
    * Base function to edit file
    * Implemented in the provider specific controllers
    *
    * @param json        the json received from the REST call
    * @param userSession the implicit user session
    * @return depends on the implementation
    */
  override def editFile(json: JValue)(implicit userSession: UserSession, requestSource: RequestSource, ec: ExecutionContext): Future[ApiMessage] = ???

  /**
    * Base function to delete file
    * Implemented in the provider specific controllers
    *
    * @param json        the json received from the REST call
    * @param userSession the implicit user session
    * @return depends on the implementation
    */
  override def deleteFile(json: JValue)(implicit userSession: UserSession, requestSource: RequestSource, ec: ExecutionContext): Future[ApiMessage] = ???

  /**
    * Base function to find file
    * Implemented in the provider specific controllers
    *
    * @param json          the json received from the REST call
    * @param userSession   the implicit user session
    * @param requestSource the implicit request source.
    * @return depends on the implementation
    */
  override def findFile(json: JValue)(implicit userSession: UserSession, requestSource: RequestSource, ec: ExecutionContext): Future[ApiMessage] = ???

  /**
    * Base function to list files
    * Implemented in the provider specific controllers
    *
    * @param json          the json received from the REST call
    * @param userSession   the implicit user session
    * @param requestSource the implicit request source.
    * @return depends on the implementation
    */
  override def listFiles(json: JValue)(implicit userSession: UserSession, requestSource: RequestSource, ec: ExecutionContext): Future[ApiMessage] = ???

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
  override def resolveContentFileToStream(repoName: String, parentFolderNames: Seq[String], folderName: String, fileName: String)(implicit userSession: UserSession, requestSource: RequestSource, ec: ExecutionContext): Future[Option[Try[StreamSource]]] = ???
}


}