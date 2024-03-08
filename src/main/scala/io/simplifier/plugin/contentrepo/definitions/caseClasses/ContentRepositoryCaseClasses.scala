package io.simplifier.plugin.contentrepo.definitions.caseClasses

import io.simplifier.plugin.contentrepo.dto.RestMessages.RestMessage
 import io.simplifier.pluginbase.util.api.ApiMessage
 import io.swagger.annotations.ApiModelProperty

object ContentRepositoryCaseClasses {

    case class ContentRepoGetRequest(@ApiModelProperty(value="The id of the repository.") id: Int)

    case class ContentRepoGetResponse(@ApiModelProperty(value="The id of the queried repository.") id: Int,
                                      @ApiModelProperty(value="The name of the queried repository.") name: String,
                                      @ApiModelProperty(value="The description of the queried repository.") description: String,
                                      @ApiModelProperty(value="The permission object type of the queried repository.") permissionObjectType: String,
                                      @ApiModelProperty(value="The permission object id of the queried repository.") permissionObjectID: String,
                                      @ApiModelProperty(value="The provider of the queried repository.") provider: String,
                                      @ApiModelProperty(value="The config of the queried repository.") config: Map[String, String],
                                      @ApiModelProperty(value="The message of this request.") message: RestMessage,
                                      @ApiModelProperty(value="The flag indicating a successful or faulty request.") success: Boolean = true) extends ApiMessage

    case class ClearContentRepoGetResponse(@ApiModelProperty(value="The id of the queried repository.") id: Int,
                                           @ApiModelProperty(value="The name of the queried repository.") name: String,
                                           @ApiModelProperty(value="The description of the queried repository.") description: String,
                                           @ApiModelProperty(value="The permission object type of the queried repository.") permissionObjectType: String,
                                           @ApiModelProperty(value="The permission object id of the queried repository.") permissionObjectID: String,
                                           @ApiModelProperty(value="The provider of the queried repository.") provider: String,
                                           @ApiModelProperty(value="The message of this request.") message: RestMessage,
                                           @ApiModelProperty(value="The flag indicating a successful or faulty request.") success: Boolean = true) extends ApiMessage

    case class ContentRepoListRequest(@ApiModelProperty(value="The provider of the repository.") provider: Option[String])

    trait ContentRepoListResponseItemTrait

    case class ContentRepoListResponseItem(@ApiModelProperty(value="The id of the queried repository.") id: Int,
                                           @ApiModelProperty(value="The name of the queried repository.") name: String,
                                           @ApiModelProperty(value="The description of the queried repository.") description: String,
                                           @ApiModelProperty(value="The permission object type of the queried repository.") permissionObjectType: String,
                                           @ApiModelProperty(value="The permission object id of the queried repository.") permissionObjectID: String,
                                           @ApiModelProperty(value="The provider of the queried repository.") provider: String,
                                           @ApiModelProperty(value="The config of the queried repository.") config: Map[String, String]) extends ContentRepoListResponseItemTrait

    case class ClearContentRepoListResponseItem(@ApiModelProperty(value="The id of the queried repository.") id: Int,
                                                @ApiModelProperty(value="The name of the queried repository.") name: String,
                                                @ApiModelProperty(value="The description of the queried repository.") description: String,
                                                @ApiModelProperty(value="The permission object type of the queried repository.") permissionObjectType: String,
                                                @ApiModelProperty(value="The permission object id of the queried repository.") permissionObjectID: String,
                                                @ApiModelProperty(value="The provider of the queried repository.") provider: String) extends ContentRepoListResponseItemTrait

    case class ContentRepoListResponse(@ApiModelProperty(value="The list of all queried repositories.") repositories: Seq[ContentRepoListResponseItemTrait],
                                       @ApiModelProperty(value="The message of this request.") message: RestMessage,
                                       @ApiModelProperty(value="The flag indicating a successful or faulty request.") success: Boolean = true) extends ApiMessage

    case class ContentRepoAddRequest(@ApiModelProperty(value="The name of the repository to create.") name: String,
                                     @ApiModelProperty(value="The description of the repository to create.") description: Option[String],
                                     @ApiModelProperty(value="The permission object type of the repository to create.") permissionObjectType: String,
                                     @ApiModelProperty(value="The permission object id of the repository to create.") permissionObjectID: String,
                                     @ApiModelProperty(value="The provider of the repository to create.",
                                         notes = "The provider indicates the type, files are persisted. The provider 'FileSystem' persists all files to database. " +
                                           "The provider 'ClearFileSystem' persists all files to file system.",
                                         allowableValues = "FileSystem, ClearFileSystem") provider: String)

    case class ContentRepoAddResponse(@ApiModelProperty(value="The id of the created repository.") id: Int,
                                      @ApiModelProperty(value="The description of the created repository.") description: String,
                                      @ApiModelProperty(value="The message of this request.") message: RestMessage,
                                      @ApiModelProperty(value="The flag indicating a successful or faulty request.") success: Boolean = true) extends ApiMessage

    case class ContentRepoDeleteRequest(@ApiModelProperty(value="The id of the deleted repository.") id: Int)

    case class ContentRepoDeleteResponse(@ApiModelProperty(value="The message of this request.") message: RestMessage,
                                         @ApiModelProperty(value="The flag indicating a successful or faulty request.") success: Boolean = true) extends ApiMessage

    case class ContentRepoEditRequest(@ApiModelProperty(value="The id of the repository to edit.") id: Int,
                                      @ApiModelProperty(value="The name of the repository to edit.") name: String,
                                      @ApiModelProperty(value="The description of the repository to edit.") description: Option[String],
                                      @ApiModelProperty(value="The permission object type of the repository to edit.") permissionObjectType: String,
                                      @ApiModelProperty(value="The permission object id of the repository to edit.") permissionObjectID: String)

    case class ContentRepoEditResponse(@ApiModelProperty(value="The message of this request.") message: RestMessage,
                                       @ApiModelProperty(value="The flag indicating a successful or faulty request.") success: Boolean = true) extends ApiMessage

    case class ContentRepoFindRequest(@ApiModelProperty(value="The name of the repository to find.") name: String)

    trait ContentRepoFindResponseItemTrait

    case class ContentRepoFindResponseItem(@ApiModelProperty(value="The id of the queried repository.") id: Int,
                                           @ApiModelProperty(value="The name of the queried repository.") name: String,
                                           @ApiModelProperty(value="The description of the queried repository.") description: String,
                                           @ApiModelProperty(value="The permission object type of the queried repository.") permissionObjectType: String,
                                           @ApiModelProperty(value="The permission object id of the queried repository.") permissionObjectID: String,
                                           @ApiModelProperty(value="The provider of the queried repository.") provider: String,
                                           @ApiModelProperty(value="The config of the queried repository.") config: Map[String, String]) extends ContentRepoFindResponseItemTrait

    case class ClearContentRepoFindResponseItem(@ApiModelProperty(value="The id of the queried repository.") id: Int,
                                                @ApiModelProperty(value="The name of the queried repository.") name: String,
                                                @ApiModelProperty(value="The description of the queried repository.") description: String,
                                                @ApiModelProperty(value="The permission object type of the queried repository.") permissionObjectType: String,
                                                @ApiModelProperty(value="The permission object id of the queried repository.") permissionObjectID: String,
                                                @ApiModelProperty(value="The provider of the queried repository.") provider: String) extends ContentRepoFindResponseItemTrait

    case class ContentRepoFindResponse(@ApiModelProperty(value="The list of repositories to find.") repositories: Seq[ContentRepoFindResponseItemTrait],
                                       @ApiModelProperty(value="The message of this request.") message: RestMessage,
                                       @ApiModelProperty(value="The flag indicating a successful or faulty request.") success: Boolean = true) extends ApiMessage
}