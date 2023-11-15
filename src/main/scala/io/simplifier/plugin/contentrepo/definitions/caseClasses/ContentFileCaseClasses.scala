package io.simplifier.plugin.contentrepo.definitions.caseClasses

import akka.http.scaladsl.marshalling.{Marshaller, ToResponseMarshaller}
import akka.http.scaladsl.model.headers.ContentDispositionTypes.inline
import akka.http.scaladsl.model.headers.`Content-Disposition`
import akka.http.scaladsl.model.{ContentType, HttpEntity, HttpResponse}
import io.simplifier.plugin.contentrepo.definitions.caseClasses.ContentFolderCaseClasses.ContentFolderGetResponseForAll
import io.simplifier.plugin.contentrepo.definitions.caseClasses.ContentRepositoryCaseClasses.ContentRepoListResponseItem
import io.simplifier.plugin.contentrepo.definitions.caseClasses.MimeMappingCaseClasses.MimeMappingReturn
import io.simplifier.plugin.contentrepo.dto.RestMessages.RestMessage
import io.simplifier.plugin.contentrepo.contentRepoIo.StreamSource
import io.simplifier.plugin.contentrepo.model.{ContentFile => ContentFileModel}
import io.simplifier.pluginbase.util.api.ApiMessage

import java.sql.Timestamp
import scala.collection.immutable

object ContentFileCaseClasses {

  trait FileDataSource {
    def data: Option[String]

    def uploadSession: Option[String]

    def copyFrom: Option[Int]
  }

  case class ContentFileGetRequest(id: Int)

  case class ClearContentFileGetRequest(contentId: Int, filePath: String)

  case class ContentFileGetResponse(id: Int,
                                    folderId: Int,
                                    name: String,
                                    description: String,
                                    statusSchemeID: String,
                                    statusID: String,
                                    securitySchemeID: String,
                                    permissionObjectType: String,
                                    permissionObjectID: String,
                                    mimeType: MimeMappingReturn,
                                    url: String,
                                    urlWithToken: Option[String],
                                    data: String,
                                    length: Long,
                                    message: RestMessage,
                                    recDate: Option[Timestamp],
                                    recUser: Option[String],
                                    chgDate: Option[Timestamp],
                                    chgUser: Option[String],
                                    success: Boolean = true
                                   ) extends ApiMessage

  case class ClearContentFileGetResponse(filePath: String,
                                         data: String,
                                         message: RestMessage,
                                         length: Long,
                                         mimeType: MimeMappingReturn,
                                         url: String,
                                         urlWithToken: Option[String],
                                         recDate: Option[Timestamp],
                                         chgDate: Option[Timestamp],
                                         success: Boolean = true) extends ApiMessage

  case class FileInformation(id: Int)

  case class ContentFileGetMetadataBatchedRequest(files: Seq[FileInformation], contentId: Int)

  case class ClearFileInformation(filePath: String)

  case class ClearContentFileGetMetadataBatchedRequest(files: Seq[ClearFileInformation], contentId: Int)

  trait GetFileMetadataBatchedResponseItem

  case class ClearContentFileGetMetadataBatchedResponseItem(filePath: String,
                                                            mimeType: MimeMappingReturn,
                                                            url: String,
                                                            urlWithToken: Option[String],
                                                            recDate: Timestamp,
                                                            recUser: String,
                                                            chgDate: Timestamp,
                                                            length: Long
                                                           ) extends GetFileMetadataBatchedResponseItem

  case class ContentFileGetMetadataBatchedResponseItem(id: Int,
                                                       folderId: Int,
                                                       name: String,
                                                       description: String,
                                                       statusSchemeID: String,
                                                       statusID: String,
                                                       securitySchemeID: String,
                                                       permissionObjectType: String,
                                                       permissionObjectID: String,
                                                       mimeType: MimeMappingReturn,
                                                       url: String,
                                                       urlWithToken: Option[String],
                                                       recDate: Option[Timestamp],
                                                       recUser: Option[String],
                                                       chgDate: Option[Timestamp],
                                                       chgUser: Option[String]
                                                      ) extends GetFileMetadataBatchedResponseItem

  case class GetFileMetadataBatchedResponse(fileMetadata: Seq[GetFileMetadataBatchedResponseItem],
                                            message: RestMessage,
                                            success: Boolean = true) extends ApiMessage

  case class ClearContentFileGetMetadataResponse(filePath: String,
                                                 mimeType: MimeMappingReturn,
                                                 url: String,
                                                 urlWithToken: Option[String],
                                                 message: RestMessage,
                                                 recDate: Option[Timestamp],
                                                 chgDate: Option[Timestamp],
                                                 length: Long,
                                                 success: Boolean = true) extends ApiMessage

  case class ContentFileGetMetadataResponse(id: Int,
                                            folderId: Int,
                                            name: String,
                                            description: String,
                                            statusSchemeID: String,
                                            statusID: String,
                                            securitySchemeID: String,
                                            permissionObjectType: String,
                                            permissionObjectID: String,
                                            mimeType: MimeMappingReturn,
                                            url: String,
                                            urlWithToken: Option[String],
                                            message: RestMessage,
                                            recDate: Option[Timestamp],
                                            recUser: Option[String],
                                            chgDate: Option[Timestamp],
                                            chgUser: Option[String],
                                            length: Long,
                                            success: Boolean = true,
                                           ) extends ApiMessage

  case class ContentFileListRequest(folderId: Option[Int])

  case class ClearContentFileListRequest(contentId: Int, folderPath: Option[String])

  trait ContentFileListResponseItemTrait

  case class ContentFileListResponseItem(id: Int,
                                         name: String,
                                         description: String,
                                         statusSchemeID: String,
                                         statusID: String,
                                         securitySchemeID: String,
                                         permissionObjectType: String,
                                         permissionObjectID: String,
                                         folderId: Int,
                                         mimeType: MimeMappingReturn,
                                         url: String,
                                         urlWithToken: Option[String],
                                         recDate: Option[Timestamp],
                                         recUser: Option[String],
                                         chgDate: Option[Timestamp],
                                         chgUser: Option[String],
                                         length: Long
                                        ) extends ContentFileListResponseItemTrait

  case class ClearContentFileListResponseItem(fileName: String,
                                              mimeType: MimeMappingReturn,
                                              url: String,
                                              urlWithToken: Option[String],
                                              recDate: Option[Timestamp],
                                              chgDate: Option[Timestamp],
                                              length: Long
                                             ) extends ContentFileListResponseItemTrait

  case class ContentFileListResponse(files: Seq[ContentFileListResponseItemTrait],
                                     message: RestMessage,
                                     success: Boolean = true) extends ApiMessage

  case class ContentFileListResponseAll(repositories: Option[Seq[Repository]],
                                        message: RestMessage,
                                        success: Boolean = true) extends ApiMessage

  case class Repository(data: Option[ContentRepoListResponseItem],
                        folders: Option[Seq[Folder]])

  case class Folder(data: ContentFolderGetResponseForAll,
                    files: Option[Seq[ContentFileListResponseItem]])

  case class ContentFileAddRequest(folderId: Int,
                                   name: String,
                                   description: Option[String],
                                   securitySchemeID: String,
                                   permissionObjectType: String,
                                   permissionObjectID: String,
                                   data: Option[String],
                                   uploadSession: Option[String],
                                   copyFrom: Option[Int]) extends FileDataSource

  case class ClearContentFileAddRequest(contentId: Int,
                                        folderPath: String,
                                        fileName: String,
                                        data: Option[String],
                                        uploadSession: Option[String],
                                        copyFrom: Option[String],
                                        forceOverwrite: Option[Boolean])

  case class ContentFileAddResponse(id: Int,
                                    name: String,
                                    message: RestMessage,
                                    success: Boolean = true) extends ApiMessage

  case class ClearContentFileAddResponse(filePath: String,
                                         message: RestMessage,
                                         success: Boolean = true) extends ApiMessage

  case class ContentFileAddResponseError(name: String,
                                         message: RestMessage,
                                         success: Boolean = false) extends ApiMessage

  case class ContentFileDeleteRequest(id: Int)

  case class ClearContentFileDeleteRequest(contentId: Int, filePath: String)

  case class ContentFileDeleteResponse(message: RestMessage,
                                       success: Boolean = true) extends ApiMessage

  case class ContentFileEditRequest(id: Int,
                                    name: String,
                                    description: Option[String],
                                    securitySchemeID: String,
                                    permissionObjectType: String,
                                    permissionObjectID: String,
                                    data: Option[String],
                                    uploadSession: Option[String],
                                    copyFrom: Option[Int]) extends FileDataSource

  case class ClearContentFileEditRequest(contentId: Int,
                                         sourceFilePath: String,
                                         destFilePath: String,
                                         forceOverwrite: Option[Boolean])

  case class ContentFileEditResponse(message: RestMessage,
                                     success: Boolean = true) extends ApiMessage

  case class ContentFileEditResponseError(name: String,
                                          message: RestMessage,
                                          success: Boolean = false) extends ApiMessage

  case class ContentFileFindRequest(folderId: Int,
                                    name: String)

  case class ClearContentFileFindRequest(contentId: Int,
                                         folderPath: Option[String],
                                         fileName: String)

  trait ContentFileFindResponseItemTrait

  case class ContentFileFindResponseItem(id: Int,
                                         name: String,
                                         description: String,
                                         statusSchemeID: String,
                                         statusID: String,
                                         securitySchemeID: String,
                                         permissionObjectType: String,
                                         permissionObjectID: String,
                                         folderId: Int,
                                         mimeType: MimeMappingReturn,
                                         url: String,
                                         urlWithToken: Option[String],
                                         recDate: Option[Timestamp],
                                         recUser: Option[String],
                                         chgDate: Option[Timestamp],
                                         chgUser: Option[String],
                                         length: Long
                                        ) extends ContentFileFindResponseItemTrait

  case class ClearContentFileFindResponseItem(filePath: String,
                                              mimeType: MimeMappingReturn,
                                              url: String,
                                              urlWithToken: Option[String],
                                              recDate: Option[Timestamp],
                                              chgDate: Option[Timestamp],
                                              length: Long
                                             ) extends ContentFileFindResponseItemTrait

  case class ContentFileFindResponse(files: Seq[ContentFileFindResponseItemTrait],
                                     message: RestMessage,
                                     success: Boolean = true) extends ApiMessage

  case class PublicFileData(fileName: String,
                            data: StreamSource,
                            contentType: ContentType.Binary)

  case class ContentFileWithUrl(contentFile: ContentFileModel,
                                mimeType: MimeMappingReturn,
                                url: String,
                                urlWithToken: Option[String],
                                length: Long)

  object PublicFileData {
    implicit val rm: ToResponseMarshaller[PublicFileData] =
      Marshaller.opaque {
        case PublicFileData(fileName, data, contentType) =>
          val header = `Content-Disposition`(dispositionType = inline, params = Map("filename" -> fileName))
          val entity = HttpEntity.Default(contentType, data.length.get, data.stream().get)
          HttpResponse(headers = immutable.Seq(header), entity = entity)
      }
  }

}