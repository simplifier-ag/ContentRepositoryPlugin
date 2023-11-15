package io.simplifier.plugin.contentrepo

import akka.actor.ActorSystem
import akka.http.scaladsl.model.Uri
import akka.stream.Materializer
import io.simplifier.plugin.contentrepo.controller.BaseController.ContentRepoPermissions
import io.simplifier.plugin.contentrepo.controller.file.ClearFileSystemFileController
import io.simplifier.plugin.contentrepo.controller.mimeMapping.MimeMappingController
import io.simplifier.plugin.contentrepo.dao._
import io.simplifier.plugin.contentrepo.definitions.caseClasses.ContentFileCaseClasses._
import io.simplifier.plugin.contentrepo.definitions.caseClasses.MimeMappingCaseClasses.MimeMappingReturn
import io.simplifier.plugin.contentrepo.dto.RestMessages._
import io.simplifier.plugin.contentrepo.helper.database
import io.simplifier.plugin.contentrepo.helper.database.ContentFileHelper
import io.simplifier.plugin.contentrepo.model.ContentRepository
import io.simplifier.plugin.contentrepo.model.provider.ClearFileSystemProvider
import io.simplifier.plugin.contentrepo.model.provider.ClearFileSystemProvider.{FindFileInformation, GetFileInformation, ListFileInformation}
import io.simplifier.plugin.contentrepo.permission.PermissionHandler
import com.typesafe.config.Config
import io.simplifier.pluginapi.UserSession
import io.simplifier.pluginapi.helper.Base64Encoding
import io.simplifier.pluginapi.rest.PluginHeaders.{AppServer, RequestSource}
import io.simplifier.pluginbase.SimplifierPlugin.AppServerInformation
import io.simplifier.pluginbase.{PluginDescription, PluginSettings}
import io.simplifier.pluginbase.interfaces.AppServerDispatcher
import io.simplifier.pluginbase.model.UserTransaction
import io.simplifier.pluginbase.util.json.JSONCompatibility.parseJsonOrEmptyString
import org.json4s.JValue
import org.mockito.ArgumentMatchers._
import org.mockito.MockitoSugar
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

import java.nio.file.Paths
import java.sql.Timestamp
import java.util.Date
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{Await, Future}
import scala.language.postfixOps
import scala.util.{Success, Try}

class ClearFileSystemFileControllerTest extends AnyWordSpec with Matchers {
  "A ClearFileSystemFileController" when {
    "getting a file" should {
      "return the complete file information" in new GetClearFileFixture {
        Await.result(controller.getFile(input), timeout) shouldBe ClearContentFileGetResponse(
          filePath, dataString, getFileSuccess, fileSize, mimeInfo, url, None, Some(currentTime), Some(currentTime))
      }
    }
    "listing all files" should {
      "return the complete file information for all files" in new ListClearFileFixture {
        Await.result(controller.listFiles(input), timeout) shouldBe ContentFileListResponse(Vector(respItem1, respItem2, respItem3), listFileSuccess)
      }
    }
    "finding a file should return the complete file information" in new FindClearFileFixture {
      Await.result(controller.findFile(input), timeout) shouldBe ContentFileFindResponse(Seq(retItem1, retItem2), listFileSuccess)
    }
  }

  trait BaseFixture extends MockitoSugar {
    implicit val userSession: UserSession = UserSession.unauthenticated
    implicit val requestSource: RequestSource = AppServer(Some(Uri("http://localhost:8080")))
    import scala.concurrent.duration._
    val timeout: FiniteDuration = 10 seconds
    val pluginSettings: PluginSettings = mock[PluginSettings]
    val dispatcher: AppServerDispatcher = mock[AppServerDispatcher]
    val config: Config = mock[Config]
    val pluginDescription: PluginDescription = mock[PluginDescription]
    val appServerInformation: AppServerInformation = mock[AppServerInformation]
    val clearFileSystemProvider: ClearFileSystemProvider = mock[ClearFileSystemProvider]
    val mimeMappingController: MimeMappingController = mock[MimeMappingController]
    val permissionHandler: PermissionHandler = mock[PermissionHandler]
    val dataBaseHelper: ContentFileHelper = mock[database.ContentFileHelper]
    val contentFileDao: ContentFileDao = mock[ContentFileDao]
    val contentFolderDao: ContentFolderDao = mock[ContentFolderDao]
    val contentRepositoryDao: ContentRepositoryDao = mock[ContentRepositoryDao]
    val mimeMappingDao: MimeMappingDao = mock[MimeMappingDao]
    val userTransaction: UserTransaction = mock[UserTransaction]
    implicit val system: ActorSystem = mock[ActorSystem]
    implicit val materializer: Materializer = mock[Materializer]

    val id = 1
    val repoName = "repoName"
    val description: Option[String] = Some("Description")
    val permissionObjectType = "App"
    val permissionObjectId = "Test"
    val provider = "ClearFileSystem"
    val repository = new ContentRepository(id, repoName, description, permissionObjectType, permissionObjectId, provider)
    val pluginName = "contentRepoPlugin"
    val controller: ClearFileSystemFileController = spy(new ClearFileSystemFileController(dispatcher, pluginDescription,
      appServerInformation, clearFileSystemProvider, mimeMappingController, repository, permissionHandler, contentFileDao,
      contentFolderDao, contentRepositoryDao))

    when(appServerInformation.prefix).thenReturn(None)
    when(appServerInformation.routes).thenReturn(None)
    when(pluginDescription.name).thenReturn(pluginName)
    when(permissionHandler.checkPermissions()).thenReturn(Future.successful(new ContentRepoPermissions(Nil)))

    val currentTime = new Timestamp(new Date().getTime)
    val fileSize: Long = 27L

    doReturn(
      ClearContentFileGetMetadataBatchedResponseItem("test", MimeMappingReturn("jpg", ""), "/test", None, currentTime, "recUser", currentTime, fileSize))
        .when(controller).getMetadata(any())(any(), any())
    doReturn(Paths.get("my/path")).when(clearFileSystemProvider).getNioPath(any(), any(), any(), any())
  }

  trait GetClearFileFixture extends BaseFixture with Base64Encoding {
    val extension = "jpg"
    val inputString: String =
      s"""
         | {
         |   "contentId": 1,
         |   "filePath": "path/to/file.$extension"
         | }
    """.stripMargin

    val input = parseJsonOrEmptyString(inputString)
    val filePath = s"path/to/file.$extension"
    val filePathForDownload = s"path/to/download/file.$extension/"
    val filePathForDownloadSegments: Seq[String] = Seq("path", "to", "download", s"file.$extension")
    val dataArray: Array[Byte] = Array(1.toByte, 3.toByte, 3.toByte, 7.toByte)
    val dataString: String = encodeB64(dataArray)

    val fileData: Try[Array[Byte]] = Success(dataArray)
    val url: String = s"http://localhost:8080/client/2.0/plugin/$pluginName/file/$filePathForDownload"

    val mimeType = "mimeType"

    val mimeInfo: MimeMappingReturn = MimeMappingReturn(extension, mimeType)

    when(clearFileSystemProvider.getFile(repoName, filePath)).thenReturn(GetFileInformation(filePathForDownloadSegments, fileData))
    when(clearFileSystemProvider.getFileSize(repoName, filePath)).thenReturn(Success(fileSize))
    when(mimeMappingController.getMimeTypeAndExtensionForDownload(any())).thenReturn(mimeInfo)
  }

  trait ListClearFileFixture extends BaseFixture with Base64Encoding {
    val parentFolderPath: String = "parent/folder"
    val inputString: String =
      s"""
         | {
         |   "contentId": 1,
         |   "folderPath": "$parentFolderPath"
         | }
    """.stripMargin

    val input: JValue = parseJsonOrEmptyString(inputString)

    val extension1: String = ""
    val extension2: String = "png"
    val extension3: String = "txt"

    val fileName1: String = "fileName1"
    val fileName2: String = s"fileName2.$extension2"
    val fileName3: String = s"fileName3.$extension3"

    val filePathForDownload1: String = s"$repoName/$parentFolderPath/$fileName1"
    val filePathForDownload2: String = s"$repoName/$parentFolderPath/$fileName2"
    val filePathForDownload3: String = s"$repoName/$parentFolderPath/$fileName3"

    val filePathForDownloadSegments1: Seq[String] = Seq(repoName, "parent", "folder", fileName1)
    val filePathForDownloadSegments2: Seq[String] = Seq(repoName, "parent", "folder", fileName2)
    val filePathForDownloadSegments3: Seq[String] = Seq(repoName, "parent", "folder", fileName3)

    val fileInfo1: ListFileInformation = ListFileInformation(fileName1, filePathForDownloadSegments1)
    val fileInfo2: ListFileInformation = ListFileInformation(fileName2, filePathForDownloadSegments2)
    val fileInfo3: ListFileInformation = ListFileInformation(fileName3, filePathForDownloadSegments3)

    val fileInfo: Vector[ListFileInformation] = Vector(fileInfo1, fileInfo2, fileInfo3)

    val downloadUrl1 = s"http://localhost:8080/client/2.0/plugin/$pluginName/file/$filePathForDownload1/"
    val downloadUrl2 = s"http://localhost:8080/client/2.0/plugin/$pluginName/file/$filePathForDownload2/"
    val downloadUrl3 = s"http://localhost:8080/client/2.0/plugin/$pluginName/file/$filePathForDownload3/"

    val mimeType1 = ""
    val mimeType2 = "mt2"
    val mimeType3 = "mt3"

    val mimeInfo1: MimeMappingReturn = MimeMappingReturn(extension1, mimeType1)
    val mimeInfo2: MimeMappingReturn = MimeMappingReturn(extension2, mimeType2)
    val mimeInfo3: MimeMappingReturn = MimeMappingReturn(extension3, mimeType3)

    val respItem1: ClearContentFileListResponseItem = ClearContentFileListResponseItem(fileName1, mimeInfo1, downloadUrl1,None, Some(currentTime), Some(currentTime), fileSize)
    val respItem2: ClearContentFileListResponseItem = ClearContentFileListResponseItem(fileName2, mimeInfo2, downloadUrl2,None, Some(currentTime), Some(currentTime), fileSize)
    val respItem3: ClearContentFileListResponseItem = ClearContentFileListResponseItem(fileName3, mimeInfo3, downloadUrl3,None, Some(currentTime), Some(currentTime), fileSize)

    when(clearFileSystemProvider.listFiles(repoName, Some(parentFolderPath))).thenReturn(fileInfo)
    when(mimeMappingController.getMimeTypeAndExtensionForDownload(fileName1)).thenReturn(mimeInfo1)
    when(mimeMappingController.getMimeTypeAndExtensionForDownload(fileName2)).thenReturn(mimeInfo2)
    when(mimeMappingController.getMimeTypeAndExtensionForDownload(fileName3)).thenReturn(mimeInfo3)
  }

  trait FindClearFileFixture extends BaseFixture {
    val extension = "foo"
    val parentFolderPath: String = "parent/folder"
    val fileName = s"fileName.$extension"
    val inputString: String =
      s"""
         | {
         |   "contentId": 1,
         |   "folderPath": "$parentFolderPath",
         |   "fileName": "$fileName"
         | }
    """.stripMargin
    val input: JValue = parseJsonOrEmptyString(inputString)
    val filePath1 = s"this/is/the/filepath/$fileName"
    val segments1: Seq[String] = Seq(repoName, "this", "is", "the", "filepath", fileName)
    val downloadPath1 = s"$repoName/$filePath1"
    val filePath2 = s"this/is/the/other/filepath/$fileName"
    val segments2: Seq[String] = Seq(repoName, "this", "is", "the", "other", "filepath", fileName)
    val downloadPath2 = s"$repoName/$filePath2"
    val findFileInfo: Vector[FindFileInformation] = Vector(FindFileInformation(filePath1, segments1), FindFileInformation(filePath2, segments2))

    val mimeType: String = "bar"
    val mimeInfo: MimeMappingReturn = MimeMappingReturn(extension, mimeType)

    val url1 = s"http://localhost:8080/client/2.0/plugin/$pluginName/file/$repoName/$filePath1/"
    val url2 = s"http://localhost:8080/client/2.0/plugin/$pluginName/file/$repoName/$filePath2/"

    val retItem1: ClearContentFileFindResponseItem = ClearContentFileFindResponseItem(filePath1, mimeInfo, url1,None, Some(currentTime), Some(currentTime), fileSize)
    val retItem2: ClearContentFileFindResponseItem = ClearContentFileFindResponseItem(filePath2, mimeInfo, url2,None, Some(currentTime), Some(currentTime), fileSize)

    when(clearFileSystemProvider.findFile(repoName, Some(parentFolderPath), fileName)).thenReturn(findFileInfo)
    when(mimeMappingController.getMimeTypeAndExtensionForDownload(fileName)).thenReturn(mimeInfo)
  }
}