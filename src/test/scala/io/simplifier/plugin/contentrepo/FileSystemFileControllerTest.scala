package io.simplifier.plugin.contentrepo

import akka.http.scaladsl.model.Uri
import akka.stream.Materializer
import io.simplifier.plugin.contentrepo.contentRepoIo.FileStreamSource
import io.simplifier.plugin.contentrepo.controller.BaseController.ContentRepoPermissions
import io.simplifier.plugin.contentrepo.controller.file.FileSystemFileController
import io.simplifier.plugin.contentrepo.controller.folder.ContentFolderController
import io.simplifier.plugin.contentrepo.controller.mimeMapping.MimeMappingController
import io.simplifier.plugin.contentrepo.controller.repository.ContentRepositoryController
import io.simplifier.plugin.contentrepo.dao._
import io.simplifier.plugin.contentrepo.definitions.caseClasses.ContentFileCaseClasses._
import io.simplifier.plugin.contentrepo.definitions.caseClasses.MimeMappingCaseClasses.MimeMappingReturn
import io.simplifier.plugin.contentrepo.dto.RestMessages._
import io.simplifier.plugin.contentrepo.helper.database
import io.simplifier.plugin.contentrepo.model.provider.{FileSystemIO, FileSystemProvider}
import io.simplifier.plugin.contentrepo.model.{ContentFile, ContentFolder, ContentRepository}
import io.simplifier.plugin.contentrepo.permission.PermissionHandler
import com.typesafe.config.Config
import io.simplifier.pluginapi.helper.Base64Encoding
import io.simplifier.pluginapi.{GrantedPermission, UserSession}
import io.simplifier.pluginapi.rest.PluginHeaders.{AppServer, RequestSource}
import io.simplifier.pluginbase.PluginDescription
import io.simplifier.pluginbase.SimplifierPlugin.AppServerInformation
import io.simplifier.pluginbase.interfaces.AppServerDispatcher
import io.simplifier.pluginbase.model.UserTransaction
import io.simplifier.pluginbase.util.json.JSONCompatibility.parseJsonOrEmptyString
import org.json4s._
import org.mockito.ArgumentMatchers.any
import org.mockito.MockitoSugar
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

import java.io.File
import java.sql.Timestamp
import java.util.Date
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{Await, Future}
import scala.language.postfixOps
import scala.util.Success

class FileSystemFileControllerTest extends AnyWordSpec with Matchers {

  "A FileSystemFileController" when {
    "Getting a file" should {
      "return all file information" in new GetFileFixture {
        Await.result(controller.getFile(input), timeout) shouldBe ContentFileGetResponse(fileId, folderId1, fileName, description,
          statusSchemaId, statusId, securitySchemeId, permObjectType, permObjectId, mimeMapping, url,None, dataString, fileSize, getFileSuccess, None, None, None, None)
      }
    }
    "Listing all files" should {
      "return all file information" in new ListFileFixture{
        Await.result(controller.listFiles(input), timeout) shouldBe ContentFileListResponse(Seq(respItem1, respItem2), listFileSuccess)
      }
    }
    "Finding a file" should {
      "return all file information" in new FindFileFixture {
        Await.result(controller.findFile(input), timeout) shouldBe ContentFileFindResponse(Seq(respItem), listFileSuccess)
      }
    }
  }

  trait BaseFixture extends MockitoSugar {

    implicit val userSession: UserSession = UserSession.unauthenticated
    implicit val requestSource: RequestSource = AppServer(Some(Uri("http://localhost:8080")))

    implicit val callUri:Option[String] = None
    val dispatcher: AppServerDispatcher = mock[AppServerDispatcher]
    val config: Config = mock[Config]
    val pluginDescription: PluginDescription = mock[PluginDescription]
    val appServerInformation: AppServerInformation = mock[AppServerInformation]
    val fileSystemProvider: FileSystemProvider = mock[FileSystemProvider]
    val mimeMappingController: MimeMappingController = mock[MimeMappingController]
    val contentRepositoryController: ContentRepositoryController = mock[ContentRepositoryController]
    val contentFolderController: ContentFolderController = mock[ContentFolderController]
    val permissionHandler: PermissionHandler = mock[PermissionHandler]
    val databaseHelper: database.ContentFileHelper = mock[database.ContentFileHelper]
    val contentRepoDao: ContentRepositoryDao = mock[ContentRepositoryDao]
    val contentFolderDao: ContentFolderDao = mock[ContentFolderDao]
    val contentFileDao: ContentFileDao = mock[ContentFileDao]
    val mimeMappingDao: MimeMappingDao = mock[MimeMappingDao]
    val userTransaction: UserTransaction = mock[UserTransaction]
    implicit val materializer: Materializer = mock[Materializer]
    import scala.concurrent.duration._
    val timeout: FiniteDuration = Duration(60, SECONDS)

    val controller: FileSystemFileController = spy(new FileSystemFileController(
    dispatcher,
    pluginDescription,
    appServerInformation,
    fileSystemProvider,
    mimeMappingController,
    contentRepositoryController,
    contentFolderController,
    permissionHandler,
    None,
    timeout,
    databaseHelper,
    contentRepoDao,
    contentFolderDao,
    contentFileDao,
    mimeMappingDao,
    userTransaction))

    val permObjectType: String = "permObjectType"
    val permObjectId: String = "permObjectId"
    val technicalName: String = "io.plugin.ContentRepo"
    val permissionMap: Map[String, Set[String]] = Map("CreateRepository" -> Set("true"), "MimeMappings" -> Set("true"), "PermissionObjectID" -> Set(permObjectId), "PermissionObjectType" -> Set(permObjectType))
    val granted: Seq[GrantedPermission] = Seq(GrantedPermission(technicalName, permissionMap))

    val repoId: Int = 1
    val repoName: String = "repo"
    val repoDesc: Option[String] = Some("repoDesc")
    val repoProvider: String = "FileSystem"
    val repo: ContentRepository = new ContentRepository(repoId, repoName, repoDesc, permObjectType, permObjectId, repoProvider)

    val description: String = "desc"
    val statusSchemaId: String = "statusSchemaId"
    val statusId: String = "statusId"
    val securitySchemeId: String = "securitySchemeId"
    val extFileId: Option[String] = None

    val pluginName = "contentRepoPlugin"
    val permissions = new ContentRepoPermissions(granted)

    when(appServerInformation.prefix).thenReturn(None)
    when(appServerInformation.routes).thenReturn(None)
    when(pluginDescription.name).thenReturn(pluginName)
    when(permissionHandler.checkPermissions()).thenReturn(Future.successful(permissions))

    val currentTime = new Timestamp(new Date().getTime)
    val fileSize: Long = 27L

    doReturn(
      ContentFileWithUrl(
        new ContentFile(1, "MyFileName", "My description", "MyStatusScheme",
          "MyStatusId", "MySecuritySchemeId", "MyPermissionObjectType",
          "MyPermissionObjectId", None, 1, None, Some(currentTime), Some("recUser"),
          Some(currentTime), Some("chgUser"), None), MimeMappingReturn("jpg", "MyMimeType"), "/url", None, fileSize))
      .when(controller).getMetadata(any(), any())(any(), any())

  }

  trait GetFileFixture extends BaseFixture with Base64Encoding {
    val fileId: Int = 1
    val fileName: String = "fileName.foo"

    val folderId1: Int = 1
    var extension: String = "foo"
    val file: ContentFile = new ContentFile(fileId, fileName, description, statusSchemaId, statusId, securitySchemeId, permObjectType, permObjectId, extFileId, folderId1, None, None, None, None, None, Some(extension))

    val folderId2: Int = 2

    val folderName: String = "folderName"
    val folderDescription: String = "folderDesc"
    val folderCurrentStatus: String = "currentStatus"
    val parentFolder1: Option[Int] = Some(folderId2)
    val parentFolder2: Option[Int] = None

    val folder1: ContentFolder = new ContentFolder(folderId1, folderName, folderDescription, statusSchemaId, statusId, securitySchemeId, folderCurrentStatus, permObjectType, permObjectId, parentFolder1, repoId)
    val folder2: ContentFolder = new ContentFolder(folderId2, folderName, folderDescription, statusSchemaId, statusId, securitySchemeId, folderCurrentStatus, permObjectType, permObjectId, parentFolder2, repoId)
    val baseFolder: String = "baseFolder"
    val baseDir = new File(baseFolder, repoName)
    val folderDir = new File(baseDir, folderId2.toString)
    val fileIoFile = new File(folderDir, s"$fileId.dat")

    val fileSystemIO: FileSystemIO = mock[FileSystemIO]

    val streamSource = new FileStreamSource(fileIoFile.toPath)

    val mimeType = "bar"

    val mimeMapping: MimeMappingReturn = MimeMappingReturn(extension, mimeType)

    val dataArray: Array[Byte] = Array(1.toByte, 3.toByte, 3.toByte, 7.toByte)

    val length: Int = dataArray.length

    val inputString: String =
        s"""
           | {
           |   "id": $fileId
           | }
      """.stripMargin

    val input: JValue = parseJsonOrEmptyString(inputString)

    val url: String = s"http://localhost:8080/client/2.0/plugin/$pluginName/file/$repoName/$folderName/$folderName/$fileName/"

    val dataString: String = encodeB64(dataArray)

    when(databaseHelper.getDatabaseEntry(fileId)).thenReturn(Success(Some(file)))
    when(contentFolderDao.getById(folderId1)).thenReturn(Some(folder1))
    when(contentFolderDao.getById(folderId2)).thenReturn(Some(folder2))
    when(contentRepoDao.getById(repoId)).thenReturn(Some(repo))
    when(contentFolderDao.getById(parentFolder1.get)).thenReturn(Some(folder2))
    when(fileSystemProvider.resolveFileSystem(file, folder1, repoName)).thenReturn(Success(fileSystemIO))
    when(fileSystemIO.read(file)).thenReturn(Success(streamSource))
    when(mimeMappingController.getMimeTypeAndExtensionForDownload(fileName)).thenReturn(mimeMapping)
    when(fileSystemIO.readStreamSource(streamSource, file)).thenReturn(Future.successful(dataArray))
  }

  trait ListFileFixture extends BaseFixture {
    val folderId: Int = 1
    val inputString: String =
      s"""
         | {
         |  "folderId" : $folderId
         | }
       """.stripMargin

    val input: JValue = parseJsonOrEmptyString(inputString)

    val folderName: String = "folderName"
    val folderDescription: String = "folderDesc"
    val folderCurrentStatus: String = "currentStatus"
    val parentFolder: Option[Int] = None

    val fileId1: Int = 1
    val fileName1: String = "fileName.foo"
    var extension1: String = "foo"
    val file1: ContentFile = new ContentFile(fileId1, fileName1, description, statusSchemaId, statusId, securitySchemeId, permObjectType, permObjectId, extFileId, folderId, None, None, None, None, None, Some(extension1))
    val fileId2: Int = 2
    val fileName2: String = "fileName.bar"
    val extension2: String = "bar"
    val file2: ContentFile = new ContentFile(fileId2, fileName2, description, statusSchemaId, statusId, securitySchemeId, permObjectType, permObjectId, extFileId, folderId, None, None, None, None, None, Some(extension2))
    val files: Seq[ContentFile] = Seq(file1, file2)

    val mimeType1 = "bar"
    val mimeType2 = "foo"

    val mime1: MimeMappingReturn = MimeMappingReturn(extension1, mimeType1)
    val mime2: MimeMappingReturn = MimeMappingReturn(extension2, mimeType2)

    val folder: ContentFolder = new ContentFolder(folderId, folderName, folderDescription, statusSchemaId, statusId, securitySchemeId, folderCurrentStatus, permObjectType, permObjectId, parentFolder, repoId)

    val url1 = s"http://localhost:8080/client/2.0/plugin/$pluginName/file/$repoName/$folderName/$fileName1/"
    val url2 = s"http://localhost:8080/client/2.0/plugin/$pluginName/file/$repoName/$folderName/$fileName2/"

    val respItem1: ContentFileListResponseItem = ContentFileListResponseItem(fileId1, fileName1, description,
      statusSchemaId, statusId, securitySchemeId, permObjectType, permObjectId, folderId, mime1, url1,None,None, None, None, None, fileSize)
    val respItem2: ContentFileListResponseItem = ContentFileListResponseItem(fileId2, fileName2, description,
      statusSchemaId, statusId, securitySchemeId, permObjectType, permObjectId, folderId, mime2, url2,None,None, None, None, None, fileSize)

    when(contentFolderDao.getById(folderId)).thenReturn(Some(folder))
    when(contentRepoDao.getById(repoId)).thenReturn(Some(repo))
    when(databaseHelper.listDatabaseEntries(folderId, permissions)).thenReturn(Success(files))
    when(mimeMappingController.getMimeTypeAndExtensionForDownloadByExtension(extension1)).thenReturn(mime1)
    when(mimeMappingController.getMimeTypeAndExtensionForDownloadByExtension(extension2)).thenReturn(mime2)
  }

  trait FindFileFixture extends BaseFixture {
    val fileName = "fileName.foo"
    val fileId1 = 1
    val folderId1 = 1
    val extension = "foo"
    val currentStatus = "currentStatus"
    val file1 = new ContentFile(fileId1, fileName, description, statusSchemaId, statusId, securitySchemeId,
      permObjectType, permObjectId, extFileId, folderId1, None, None, None, None, None, Some(extension))
    var folderName = "folderName"
    val parentFolderId: Option[Int] = None
    val folder = new ContentFolder(folderId1, folderName, description, statusSchemaId, statusId, securitySchemeId,
      currentStatus, permObjectType, permObjectId, parentFolderId, repoId)

    val mimeType = "bar"
    val mime: MimeMappingReturn = MimeMappingReturn(extension, mimeType)

    val inputString: String =
      s"""
         | {
         |  "folderId" : $folderId1,
         |  "name": "$fileName"
         | }
       """.stripMargin

    val input: JValue = parseJsonOrEmptyString(inputString)

    val url = s"http://localhost:8080/client/2.0/plugin/$pluginName/file/$repoName/$folderName/$fileName/"

    val respItem: ContentFileFindResponseItem = ContentFileFindResponseItem(fileId1, fileName, description, statusSchemaId, statusId, securitySchemeId,
      permObjectType, permObjectId, folderId1, mime, url,None, None, None, None, None, fileSize)

    when(contentFolderDao.getById(folderId1)).thenReturn(Some(folder))
    when(contentRepoDao.getById(repoId)).thenReturn(Some(repo))
    when(databaseHelper.getDatabaseEntry(any(), any(), any())).thenReturn(Success(Some(file1)))
    when(mimeMappingController.getMimeTypeAndExtensionForDownloadByExtension(extension)).thenReturn(mime)
  }
}