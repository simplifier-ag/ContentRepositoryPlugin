package io.simplifier.plugin.contentrepo.definitions.caseClasses

import io.simplifier.plugin.contentrepo.definitions.caseClasses.ContentRepositoryCaseClasses.ContentRepoListResponseItem
import io.simplifier.plugin.contentrepo.dto.RestMessages.RestMessage
import io.simplifier.pluginbase.util.api.ApiMessage
import io.swagger.annotations.ApiModelProperty

object ContentFolderCaseClasses {


  case class ContentFolderGetRequest(@ApiModelProperty(value="The id of the requested folder.") id: Int)

  case class ClearContentFolderGetRequest(@ApiModelProperty(value="The repository if of the queried folder.") contentId: Int,
                                          @ApiModelProperty(value="The folder path to the requested folder.") folderPath: String)

  case class ContentFolderGetResponse(@ApiModelProperty(value="The id of the queried folder.") id: Int,
                                      @ApiModelProperty(value="The parent folder id of the queried folder.") parentFolderId: Option[Int],
                                      @ApiModelProperty(value="The name of the queried folder.") name: String,
                                      @ApiModelProperty(value="The description of the queried folder.") description: String,
                                      @ApiModelProperty(value="The status scheme id of the queried folder.") statusSchemeID: String,
                                      @ApiModelProperty(value="The status id of the queried folder.") statusID: String,
                                      @ApiModelProperty(value="The security scheme id of the queried folder.") securitySchemeID: String,
                                      @ApiModelProperty(value="The status of the queried folder.") currentStatus: String,
                                      @ApiModelProperty(value="The permission object type of the queried folder.") permissionObjectType: String,
                                      @ApiModelProperty(value="The permission object id of the queried folder.") permissionObjectID: String,
                                      @ApiModelProperty(value="The message returned by this request.") message: RestMessage,
                                      @ApiModelProperty(value="The flag indicating a successful or faulty request.") success: Boolean = true) extends ApiMessage

  case class ClearContentFolderGetResponse(@ApiModelProperty(value="The path to the queried folder.") path: String,
                                           @ApiModelProperty(value="The message returned by this request.") message: RestMessage,
                                           @ApiModelProperty(value="The flag indicating a successful or faulty request.") success: Boolean = true) extends ApiMessage

  case class ContentFolderGetResponseForAll(@ApiModelProperty(value="The id of the queried folder.") id: Int,
                                            @ApiModelProperty(value="The parent folder id of the queried folder.") parentFolderId: Option[Int],
                                            @ApiModelProperty(value="The repository id of the queried folder.") contentId: Int,
                                            @ApiModelProperty(value="The name of the queried folder.") name: String,
                                            @ApiModelProperty(value="The description of the queried folder.") description: String,
                                            @ApiModelProperty(value="The status scheme id of the queried folder.") statusSchemeID: String,
                                            @ApiModelProperty(value="The status id of the queried folder.") statusID: String,
                                            @ApiModelProperty(value="The security scheme id of the queried folder.") securitySchemeID: String,
                                            @ApiModelProperty(value="The status of the queried folder.") currentStatus: String,
                                            @ApiModelProperty(value="The permission object type of the queried folder.") permissionObjectType: String,
                                            @ApiModelProperty(value="The permission object id of the queried folder.") permissionObjectID: String)

  case class ContentFolderListRequest(@ApiModelProperty(value="The repository id used to identify all folders to return.") contentId: Option[Int],
                                      @ApiModelProperty(value="The parent folder id used to identify all folders to return.") parentFolderId: Option[Int])

  case class ClearContentFolderListRequest(@ApiModelProperty(value="The repository id used to identify all folders to return.") contentId: Int,
                                           @ApiModelProperty(value="The parent folder path used to identify all folders to return.") parentFolderPath: Option[String])

  trait ContentFolderListResponseItemTrait

  case class ContentFolderListResponseItem(@ApiModelProperty(value="The id of the queried folder.") id: Int,
                                           @ApiModelProperty(value="The name of the queried folder.") name: String,
                                           @ApiModelProperty(value="The description of the queried folder.") description: String,
                                           @ApiModelProperty(value="The status scheme id of the queried folder.") statusSchemeID: String,
                                           @ApiModelProperty(value="The status id of the queried folder.") statusID: String,
                                           @ApiModelProperty(value="The security scheme id of the queried folder.") securitySchemeID: String,
                                           @ApiModelProperty(value="The current status of the queried folder.") currentStatus: String,
                                           @ApiModelProperty(value="The permission object type of the queried folder.") permissionObjectType: String,
                                           @ApiModelProperty(value="The permission object id of the queried folder.") permissionObjectID: String) extends ContentFolderListResponseItemTrait

  case class ClearContentFolderListResponseItem(@ApiModelProperty(value="The queried folder.") folderName: String) extends ContentFolderListResponseItemTrait

  case class ContentFolderListResponse(@ApiModelProperty(value="The list of queried folders.") folders: Seq[ContentFolderListResponseItemTrait],
                                       @ApiModelProperty(value="The message returned by this request.") message: RestMessage,
                                       @ApiModelProperty(value="The flag indicating a successful or faulty request.") success: Boolean = true) extends ApiMessage

  case class ContentFileListResponseAll(@ApiModelProperty(value="The list of queried files.") repositories: Option[Seq[Repository]],
                                        @ApiModelProperty(value="The message returned by this request.") message: RestMessage,
                                        @ApiModelProperty(value="The flag indicating a successful or faulty request.") success: Boolean = true) extends ApiMessage

  case class Repository(@ApiModelProperty(value="The queried repository.") data: Option[ContentRepoListResponseItem],
                        @ApiModelProperty(value="The list of all queried folders.") folders: Option[Seq[ContentFolderGetResponseForAll]])

  case class ContentFolderAddRequest(@ApiModelProperty(value="The id of the repository folder.") contentId: Int,
                                     @ApiModelProperty(value="The id of the parent folder.") parentFolderId: Option[Int],
                                     @ApiModelProperty(value="The name of the new folder.") name: String,
                                     @ApiModelProperty(value="The description of the new folder.") description: String,
                                     @ApiModelProperty(value="The security scheme id of the new folder.") securitySchemeID: String,
                                     @ApiModelProperty(value="The permission object type of the new folder.") permissionObjectType: String,
                                     @ApiModelProperty(value="The permission object id of the new folder.") permissionObjectID: String)

  case class ContentFoldersAddRequest(@ApiModelProperty(value="The list of folders to create.") contentFolders: Seq[ContentFolderAddRequest])

  case class ClearContentFolderAddRequest(@ApiModelProperty(value="The repository id of the new folder.") contentId: Int,
                                          @ApiModelProperty(value="The path to the parent folder.") parentFolderPath: Option[String],
                                          @ApiModelProperty(value="The name of the new folder to create.") name: String)

  case class ClearContentFoldersAddRequest(@ApiModelProperty(value="The repository id of the new folder.") contentId: Int,
                                           @ApiModelProperty(value="The path to the parent folder.") parentFolderPath: Option[String],
                                           @ApiModelProperty(value="The name of the new folder to create.") name: Seq[String])

  case class ContentFolderAddResponse(@ApiModelProperty(value="The id of the created folder.") id: Int,
                                      @ApiModelProperty(value="The name of the created folder.") name: String,
                                      @ApiModelProperty(value="The description of the created folder.") folderDescription: String,
                                      @ApiModelProperty(value="The message returned by this request.") message: RestMessage,
                                      @ApiModelProperty(value="The flag indicating a successful or faulty request.") success: Boolean = true) extends ApiMessage

  case class ContentFoldersAddResponse(@ApiModelProperty(value="The list of created folders.") folders: Seq[ContentFolderAddResponse]) extends ApiMessage

  case class ClearContentFolderAddResponse(@ApiModelProperty(value="The path to the created folder.") path: String,
                                           @ApiModelProperty(value="The message returned by this request.") message: RestMessage,
                                           @ApiModelProperty(value="The flag indicating a successful or faulty request.") success: Boolean = true) extends ApiMessage

  case class ClearContentFoldersAddResponse(@ApiModelProperty(value="The list of paths to the created folders.") path: Seq[String],
                                            @ApiModelProperty(value="The message returned by this request.") message: RestMessage,
                                            @ApiModelProperty(value="The flag indicating a successful or faulty request.") success: Boolean = true) extends ApiMessage

  case class ContentFolderDeleteRequest(@ApiModelProperty(value="The id of the folder to delete.") id: Int)

  case class ClearContentFolderDeleteRequest(@ApiModelProperty(value="The repository id of the folder to delete.") contentId: Int,
                                             @ApiModelProperty(value="The path of the folder to delete inside the repository.") folderPath: String,
                                             @ApiModelProperty(value="The flag indicating if the file is forced to delete. If true, the directory is deleted recursively including all sub folders.")
                                              forceDelete: Option[Boolean])

  case class ContentFolderDeleteResponse(@ApiModelProperty(value="The message returned by this request.") message: RestMessage,
                                         @ApiModelProperty(value="The flag indicating a successful or faulty request.") success: Boolean = true) extends ApiMessage

  case class ContentFolderEditRequest(@ApiModelProperty(value="The id of the folder to edit.") id: Int,
                                      @ApiModelProperty(value="The name of the folder to edit.") name: String,
                                      @ApiModelProperty(value="The description of the folder to edit.") description: String,
                                      @ApiModelProperty(value="The security scheme id of the folder to edit.") securitySchemeID: String,
                                      @ApiModelProperty(value="The permission object type of the folder to edit.") permissionObjectType: String,
                                      @ApiModelProperty(value="The permission object id of the folder to edit.") permissionObjectID: String)

  case class ClearContentFolderEditRequest(@ApiModelProperty(value="The id of the folder to edit.") contentId: Int,
                                           @ApiModelProperty(value="The source path of the folder to edit.") sourceFolderPath: String,
                                           @ApiModelProperty(value="The target path of the folder to edit.") destFolderPath: String)

  case class ContentFolderEditResponse(@ApiModelProperty(value="The message returned by this request.") message: RestMessage,
                                       @ApiModelProperty(value="The flag indicating a successful or faulty request.") success: Boolean = true) extends ApiMessage

  case class ContentFolderFindRequest(@ApiModelProperty(value="The repository id of the folder to find.") contentId: Int,
                                      @ApiModelProperty(value="The name of the folder to find.") name: String,
                                      @ApiModelProperty(dataType="java.lang.Integer", value="The parent folder id of the folder to find.") parentFolderId: Option[Int])

  case class ClearContentFolderFindRequest(@ApiModelProperty(value="The repository id of the folder to find.") contentId: Int,
                                           @ApiModelProperty(value="The name of the folder to find.") name: String,
                                           @ApiModelProperty(value="The parent folder path of the folder to find.") parentFolderPath: Option[String])

  trait ContentFolderFindResponseItemTrait

  case class ContentFolderFindResponseItem(@ApiModelProperty(value="The id of the queried folder.") id: Int,
                                           @ApiModelProperty(value="The name of the queried folder.") name: String,
                                           @ApiModelProperty(value="The description of the queried folder.") description: String,
                                           @ApiModelProperty(value="The status scheme id of the queried folder.") statusSchemeID: String,
                                           @ApiModelProperty(value="The status id of the queried folder.") statusID: String,
                                           @ApiModelProperty(value="The security scheme id of the queried folder.") securitySchemeID: String,
                                           @ApiModelProperty(value="The status of the queried folder.") currentStatus: String,
                                           @ApiModelProperty(value="The permission object type of the queried folder.") permissionObjectType: String,
                                           @ApiModelProperty(value="The permission object id of the queried folder.") permissionObjectID: String) extends ContentFolderFindResponseItemTrait

  case class ClearContentFolderFindResponseItem(@ApiModelProperty(value="The path of the queried folder.") path: String) extends ContentFolderFindResponseItemTrait

  case class ContentFolderFindResponse(@ApiModelProperty(value="The list of queried folders.") folders: Seq[ContentFolderFindResponseItemTrait],
                                       @ApiModelProperty(value="The message returned by this request.") message: RestMessage,
                                       @ApiModelProperty(value="The flag indicating a successful or faulty request.") success: Boolean = true) extends ApiMessage
}