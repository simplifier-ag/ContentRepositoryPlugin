package io.simplifier.plugin.contentrepo.interfaces

import akka.actor.ActorSystem
import akka.http.scaladsl.model.StatusCodes
import akka.stream.Materializer
import io.simplifier.plugin.contentrepo.ContentRepoPluginErrorMessageUtils
import io.simplifier.plugin.contentrepo.controller.BaseController.{AsyncSlotImpl, AsyncSlotImplOpt, OperationFailure, OperationFailureMessage}
import io.simplifier.plugin.contentrepo.controller.file.FileControllerFactory
import io.simplifier.plugin.contentrepo.controller.folder.FolderControllerFactory
import io.simplifier.plugin.contentrepo.controller.mimeMapping.MimeMappingControllerFactory
import io.simplifier.plugin.contentrepo.controller.repository.RepositoryControllerFactory
import io.simplifier.plugin.contentrepo.definitions.Constants._
import io.simplifier.plugin.contentrepo.definitions.caseClasses.ContentFileCaseClasses._
import io.simplifier.plugin.contentrepo.definitions.caseClasses.ContentFolderCaseClasses._
import io.simplifier.plugin.contentrepo.definitions.caseClasses.ContentRepositoryCaseClasses._
import io.simplifier.plugin.contentrepo.definitions.caseClasses.MimeMappingCaseClasses._
import io.simplifier.plugin.contentrepo.dto.RestMessages._
import io.simplifier.plugin.contentrepo.interfaces.ContentRepoSlotInterface.{DEFAULT_MAXIMUM_LARGE_STRING_LENGTH, PATH_TO_MAXIMUM_LARGE_STRING_LENGTH}
import io.simplifier.plugin.contentrepo.permission.ContentRepoPermission.characteristicAdministrate
import io.simplifier.plugin.contentrepo.permission.PermissionHandler
import com.typesafe.config.Config
import io.simplifier.pluginapi.UserSession
import io.simplifier.pluginapi.rest.PluginHeaders.RequestSource
import io.simplifier.pluginbase.SimplifierPlugin.AppServerInformation
import io.simplifier.pluginbase.interfaces.DefaultSlotInterfaceService.SlotMagnet
import io.simplifier.pluginbase.interfaces.{AppServerDispatcher, DefaultSlotInterfaceService}
import io.simplifier.pluginbase.permission.PluginPermissionObject
import io.simplifier.pluginbase.util.api.ApiMessage
import io.simplifier.pluginbase.util.http.JsonMarshalling._
import io.simplifier.pluginbase.util.logging.{ExceptionFormatting, Logging}
import io.simplifier.pluginbase.{PluginDescription, PluginSettings}
import io.swagger.annotations._
import org.json4s._
import org.slf4j.Logger

import javax.ws.rs.Path
import scala.concurrent.{ExecutionContext, Future}

/**
  * Slot interface for Content Repository Plugin.
  */
class ContentRepoSlotInterface(config: Config,
                               dispatcher: AppServerDispatcher,
                               pluginDescription: PluginDescription,
                               pluginPermission: PluginPermissionObject,
                               repositoryControllerFactory: RepositoryControllerFactory,
                               folderControllerFactory: FolderControllerFactory,
                               fileControllerFactory: FileControllerFactory,
                               mimeMappingControllerFactory: MimeMappingControllerFactory,
                               permissionHandler: PermissionHandler)
                              (implicit system: ActorSystem)
  extends DefaultSlotInterfaceService(dispatcher, pluginDescription, pluginPermission) with ContentRepoPluginErrorMessageUtils with Logging {

  override val Logger: Logger = logger

  private def asyncResultHandler(result: Future[ApiMessage], error: String => RestMessage, action: String, aspect: String)
                                (implicit ec: ExecutionContext): Future[ApiMessage] = {
    val messagePartLength: Int = determineMaximumMessageLength(config, DEFAULT_MAXIMUM_LARGE_STRING_LENGTH, PATH_TO_MAXIMUM_LARGE_STRING_LENGTH)

    result.recoverWith {
      case failure: OperationFailure =>
        val shortenedFailure = shortenOperationFailureMessageTexts(failure, messagePartLength)
        logger.error(slotError(action, aspect, shortenedFailure))

        //The original error will be logged as a trace not to lose it.
        logger.trace(slotError(action, aspect, failure))
        Future.successful(shortenedFailure.toResponse)
      case OperationFailureMessage(msg, statusCode) =>
        val failure: OperationFailure = OperationFailure(error(msg), statusCode)
        val shortenedFailure = shortenOperationFailureMessageTexts(failure, messagePartLength)
        logger.error(slotError(action, aspect, shortenedFailure))

        //The original error will be logged as a trace not to lose it.
        logger.trace(slotError(action, aspect, failure))
        Future.successful(shortenedFailure.toResponse)
      case ex: MappingException =>
        //The original message type is unfortunately lost in this process, it can be seen in the trace message.
        val shortenedMappingException: MappingException = new MappingException(shortenMessageText(ex.getMessage, messagePartLength), ex.cause)
        logger.error(slotError(action, aspect, shortenedMappingException))

        //The original error will be logged as a trace not to lose it.
        logger.trace(slotError(action, aspect, ex))
        Future.successful(
          OperationFailure(
            msg = error("Unable to parse argument: " + shortenedMappingException.getMessage),
            statusCode = StatusCodes.BadRequest
          ).toResponse
        )
      case other =>
        //The original message type is unfortunately lost in this process, it can be seen in the trace message.
        val shortenedThrowable: Throwable = new Throwable(shortenMessageText(other.getMessage, messagePartLength), other.getCause)
        logger.error(slotError(action, aspect, shortenedThrowable))

        //The original error will be logged as a trace not to lose it.
        logger.trace(slotError(action, aspect, other))
        Future.successful(OperationFailure(error("unexpected error")).toResponse)
    }
  }

  override protected def checkAdministratePermission()(implicit userSession: UserSession, requestSource: RequestSource): Unit = {
    implicit val ec: ExecutionContext = system.dispatcher
    permissionHandler.checkAdditionalPermission(characteristicAdministrate)
  }

  /** Base-URL relative to http service root */
  override val baseUrl: String = "slots"

  val addMimeMapping: AsyncSlotImpl = (json: JValue, userSession: UserSession, requestSource: RequestSource, ec: ExecutionContext) => {
    mimeMappingControllerFactory.getMimeMappingController().addMimeMappingSlot(json, userSession, requestSource, ec)
  }
  val getMimeMapping: AsyncSlotImpl = (json: JValue, userSession: UserSession, requestSource: RequestSource, ec: ExecutionContext) => {
    mimeMappingControllerFactory.getMimeMappingController().getMimeMappingSlot(json, userSession, requestSource, ec)
  }
  val editMimeMapping: AsyncSlotImpl = (json: JValue, userSession: UserSession, requestSource: RequestSource, ec: ExecutionContext) => {
    mimeMappingControllerFactory.getMimeMappingController().editMimeMappingSlot(json, userSession, requestSource, ec)
  }
  val deleteMimeMapping: AsyncSlotImpl = (json: JValue, userSession: UserSession, requestSource: RequestSource, ec: ExecutionContext) => {
    mimeMappingControllerFactory.getMimeMappingController().deleteMimeMappingSlot(json, userSession, requestSource, ec)
  }
  val listMimeMapping: AsyncSlotImpl = (_: JValue, userSession: UserSession, requestSource: RequestSource, ec: ExecutionContext) => {
    mimeMappingControllerFactory.getMimeMappingController().listMimeMappingsSlot(userSession, requestSource, ec)
  }


  // Mime Type Slots
  slotWithHttp("mimeMappingAdd", addMimeMapping)
  slotWithHttp("mimeMappingGet", getMimeMapping)
  slotWithHttp("mimeMappingEdit", editMimeMapping)
  slotWithHttp("mimeMappingDelete", deleteMimeMapping)
  slotWithHttp("mimeMappingList", listMimeMapping)

  // Repo Slots
  val addRepo: AsyncSlotImpl = (json: JValue, userSession: UserSession, requestSource: RequestSource, ec: ExecutionContext) => {
    implicit val executionContext: ExecutionContext = ec
    asyncResultHandler(repositoryControllerFactory.getRepositoryControllerByProvider(json).flatMap(_
      .addRepository(json)(userSession, requestSource, ec)), addRepositoryError, ACTION_SLOT_ADD, ASPECT_REPOSITORY)
  }

  val getRepo: AsyncSlotImpl = (json: JValue, userSession: UserSession, requestSource: RequestSource, ec: ExecutionContext) => {
    implicit val executionContext: ExecutionContext = ec
    asyncResultHandler(repositoryControllerFactory.getRepositoryControllerById(json).flatMap(_
      .getRepository(json)(userSession, requestSource, ec)), getRepositoryError, ACTION_SLOT_GET, ASPECT_REPOSITORY)
  }

  val editRepo: AsyncSlotImpl = (json: JValue, userSession: UserSession, requestSource: RequestSource, ec: ExecutionContext) => {
    implicit val executionContext: ExecutionContext = ec
    asyncResultHandler(repositoryControllerFactory.getRepositoryControllerById(json).flatMap(_
      .editRepository(json)(userSession, requestSource, ec)), editRepositoryError, ACTION_SLOT_EDIT, ASPECT_REPOSITORY)
  }

  val deleteRepo: AsyncSlotImpl = (json: JValue, userSession: UserSession, requestSource: RequestSource, ec: ExecutionContext) => {
    implicit val executionContext: ExecutionContext = ec
    asyncResultHandler(repositoryControllerFactory.getRepositoryControllerById(json).flatMap(_
      .deleteRepository(json)(userSession, requestSource, ec)), deleteRepositoryError, ACTION_SLOT_DELETE, ASPECT_REPOSITORY)
  }

  val findRepo: AsyncSlotImpl = (json: JValue, userSession: UserSession, requestSource: RequestSource, ec: ExecutionContext) => {
    implicit val executionContext: ExecutionContext = ec
    asyncResultHandler(repositoryControllerFactory.getRepositoryControllerByName(json).flatMap(_
      .findRepository(json)(userSession, requestSource, ec)), listRepositoryError, ACTION_SLOT_FIND, ASPECT_REPOSITORY)
  }

  val listRepo: AsyncSlotImplOpt = (jsonOpt: Option[JValue], userSession: UserSession, requestSource: RequestSource, ec: ExecutionContext) => {
    implicit val executionContext: ExecutionContext = ec
    val json = jsonOpt.getOrElse(JNothing)
    asyncResultHandler(repositoryControllerFactory.getRepositoryControllerByProvider(json).flatMap(_
      .listRepositories(json)(userSession, requestSource, ec)), listRepositoryError, ACTION_SLOT_LIST, ASPECT_REPOSITORY)
  }

  slotWithHttp("contentRepositoryAdd", addRepo)
  slotWithHttp("contentRepositoryGet", getRepo)
  slotWithHttp("contentRepositoryEdit", editRepo)
  slotWithHttp("contentRepositoryDelete", deleteRepo)
  slotWithHttp("contentRepositoryFind", findRepo)
  slotWithHttp("contentRepositoryList", listRepo)

  // Folder Slots

  val addFolder: AsyncSlotImpl = {
    (json: JValue, userSession: UserSession, requestSource: RequestSource, ec: ExecutionContext) =>
      implicit val uSession: UserSession = userSession
      implicit val rSource: RequestSource = requestSource
      implicit val executionContext: ExecutionContext = ec
      asyncResultHandler(folderControllerFactory.getFolderController(json, None).flatMap(_.addFolder(json)), addFolderError, ACTION_SLOT_ADD, ASPECT_FOLDER)
  }

  val addFolderIfNotExisting: AsyncSlotImpl = {
    (json: JValue, userSession: UserSession, requestSource: RequestSource, ec: ExecutionContext) =>
      implicit val uSession: UserSession = userSession
      implicit val rSource: RequestSource = requestSource
      implicit val executionContext: ExecutionContext = ec
      asyncResultHandler(folderControllerFactory.getFolderController(json, None).flatMap(_.addFolderIfNotExisting(json)), addFolderError, ACTION_SLOT_ADD, ASPECT_FOLDER)
  }

  val getFolder: AsyncSlotImpl = {
    (json: JValue, userSession: UserSession, requestSource: RequestSource, ec: ExecutionContext) =>
      implicit val uSession: UserSession = userSession
      implicit val rSource: RequestSource = requestSource
      implicit val executionContext: ExecutionContext = ec
      asyncResultHandler(folderControllerFactory.getFolderController(json, None).flatMap(_.getFolder(json)), getFolderError, ACTION_SLOT_GET, ASPECT_FOLDER)
  }

  val editFolder: AsyncSlotImpl = {
    (json: JValue, userSession: UserSession, requestSource: RequestSource, ec: ExecutionContext) =>
      implicit val uSession: UserSession = userSession
      implicit val rSource: RequestSource = requestSource
      implicit val executionContext: ExecutionContext = ec
      asyncResultHandler(folderControllerFactory.getFolderController(json, None).flatMap(_.editFolder(json)), editFolderError, ACTION_SLOT_EDIT, ASPECT_FOLDER)
  }

  val deleteFolder: AsyncSlotImpl = {
    (json: JValue, userSession: UserSession, requestSource: RequestSource, ec: ExecutionContext) =>
      implicit val uSession: UserSession = userSession
      implicit val rSource: RequestSource = requestSource
      implicit val executionContext: ExecutionContext = ec
      asyncResultHandler(folderControllerFactory.getFolderController(json, None).flatMap(_.deleteFolder(json)), deleteFolderError, ACTION_SLOT_DELETE, ASPECT_FOLDER)
  }

  val findFolder: AsyncSlotImpl = {
    (json: JValue, userSession: UserSession, requestSource: RequestSource, ec: ExecutionContext) =>
      implicit val uSession: UserSession = userSession
      implicit val rSource: RequestSource = requestSource
      implicit val executionContext: ExecutionContext = ec
      asyncResultHandler(folderControllerFactory.getFolderController(json, None).flatMap(_.findFolder(json)), listFolderError, ACTION_SLOT_FIND, ASPECT_FOLDER)
  }

  val listFolder: AsyncSlotImpl = {
    (json: JValue, userSession: UserSession, requestSource: RequestSource, ec: ExecutionContext) =>
      implicit val uSession: UserSession = userSession
      implicit val rSource: RequestSource = requestSource
      implicit val executionContext: ExecutionContext = ec
      asyncResultHandler(folderControllerFactory.getFolderController(json, None).flatMap(_.listFolders(json)), listFolderError, ACTION_SLOT_LIST, ASPECT_FOLDER)
  }

  slotWithHttp("contentFolderAdd", addFolder)
  slotWithHttp("contentFolderAddIfNotExisting", addFolderIfNotExisting)
  slotWithHttp("contentFolderGet", getFolder)
  slotWithHttp("contentFolderEdit", editFolder)
  slotWithHttp("contentFolderDelete", deleteFolder)
  slotWithHttp("contentFolderFind", findFolder)
  slotWithHttp("contentFolderList", listFolder)

  // File Slots

  val addFile: AsyncSlotImpl = {
    (json: JValue, userSession: UserSession, requestSource: RequestSource, ec: ExecutionContext) =>
      implicit val uSession: UserSession = userSession
      implicit val rSource: RequestSource = requestSource
      implicit val executionContext: ExecutionContext = ec
      asyncResultHandler(fileControllerFactory.getFileController(json, None).flatMap(_.addFile(json)), addFileError, ACTION_SLOT_ADD, ASPECT_FILE)(ec)
  }

  val getFile: AsyncSlotImpl = {
    (json: JValue, userSession: UserSession, requestSource: RequestSource, ec: ExecutionContext) =>
      implicit val uSession: UserSession = userSession
      implicit val rSource: RequestSource = requestSource
      implicit val executionContext: ExecutionContext = ec
      asyncResultHandler(fileControllerFactory.getFileController(json, None).flatMap(_.getFile(json)), getFileError, ACTION_SLOT_GET, ASPECT_FILE)
  }

  val getFileMetadata: AsyncSlotImpl = {
    (json: JValue, userSession: UserSession, requestSource: RequestSource, ec: ExecutionContext) =>
      implicit val uSession: UserSession = userSession
      implicit val rSource: RequestSource = requestSource
      implicit val executionContext: ExecutionContext = ec
      asyncResultHandler(fileControllerFactory.getFileController(json, None).flatMap(_.getFileMetadata(json)), getFileMetadataError, ACTION_SLOT_GET_METADATA, ASPECT_FILE)
  }

  val getFileMetadataBatched: AsyncSlotImpl = {
    (json: JValue, userSession: UserSession, requestSource: RequestSource, ec: ExecutionContext) =>
      implicit val uSession: UserSession = userSession
      implicit val rSource: RequestSource = requestSource
      implicit val executionContext: ExecutionContext = ec
      asyncResultHandler(fileControllerFactory.getFileController(json, None).flatMap(_.getFileMetadataBatched(json)), getFileMetadataBatchedError, ACTION_SLOT_GET_METADATA, ASPECT_FILE)
  }

  val editFile: AsyncSlotImpl = {
    (json: JValue, userSession: UserSession, requestSource: RequestSource, ec: ExecutionContext) =>
      implicit val uSession: UserSession = userSession
      implicit val rSource: RequestSource = requestSource
      implicit val executionContext: ExecutionContext = ec
      asyncResultHandler(fileControllerFactory.getFileController(json, None).flatMap(_.editFile(json)), editFileError, ACTION_SLOT_EDIT, ASPECT_FILE)
  }

  val deleteFile: AsyncSlotImpl = {
    (json: JValue, userSession: UserSession, requestSource: RequestSource, ec: ExecutionContext) =>
      implicit val uSession: UserSession = userSession
      implicit val rSource: RequestSource = requestSource
      implicit val executionContext: ExecutionContext = ec
      asyncResultHandler(fileControllerFactory.getFileController(json, None).flatMap(_.deleteFile(json)), deleteFileError, ACTION_SLOT_DELETE, ASPECT_FILE)
  }

  val findFile: AsyncSlotImpl = {
    (json: JValue, userSession: UserSession, requestSource: RequestSource, ec: ExecutionContext) =>
      implicit val uSession: UserSession = userSession
      implicit val rSource: RequestSource = requestSource
      implicit val executionContext: ExecutionContext = ec
      asyncResultHandler(fileControllerFactory.getFileController(json, None).flatMap(_.findFile(json)), listFileError, ACTION_SLOT_FIND, ASPECT_FILE)
  }

  val listFile: AsyncSlotImpl = {
    (json: JValue, userSession: UserSession, requestSource: RequestSource, ec: ExecutionContext) =>
      implicit val uSession: UserSession = userSession
      implicit val rSource: RequestSource = requestSource
      implicit val executionContext: ExecutionContext = ec
      asyncResultHandler(fileControllerFactory.getFileController(json, None).flatMap(_.listFiles(json)), listFileError, ACTION_SLOT_LIST, ASPECT_FILE)
  }

  slotWithHttp("contentFileAdd", addFile)
  slotWithHttp("contentFileGet", getFile)
  slotWithHttp("contentFileGetMetadata", getFileMetadata)
  slotWithHttp("contentFileGetMetadataBatched", getFileMetadataBatched)
  slotWithHttp("contentFileEdit", editFile)
  slotWithHttp("contentFileDelete", deleteFile)
  slotWithHttp("contentFileFind", findFile)
  slotWithHttp("contentFileList", listFile)


}

object ContentRepoSlotInterface extends ExceptionFormatting {
  def apply(dispatcher: AppServerDispatcher, pluginSettings: PluginSettings, config: Config,
            pluginDescription: PluginDescription, pluginPermission: PluginPermissionObject, appServerInformation: AppServerInformation)
           (implicit system: ActorSystem, materializer: Materializer): ContentRepoSlotInterface = {
    val permissionHandler = new PermissionHandler(dispatcher, pluginSettings)
    val mimeMappingFactory = new MimeMappingControllerFactory(permissionHandler)
    val repoFactory = new RepositoryControllerFactory(config, permissionHandler)
    val folderFactory = new FolderControllerFactory(config, repoFactory, permissionHandler)
    val fileFactory = new FileControllerFactory(dispatcher, config, pluginDescription, appServerInformation, mimeMappingFactory, repoFactory, folderFactory, permissionHandler)
    new ContentRepoSlotInterface(
      config,
      dispatcher,
      pluginDescription,
      pluginPermission,
      repoFactory,
      folderFactory,
      fileFactory,
      mimeMappingFactory,
      permissionHandler
    )
  }


  /** The maximum length is 0 in order to keep the current behavior */
  private[ContentRepoSlotInterface] val DEFAULT_MAXIMUM_LARGE_STRING_LENGTH: Int = 0
  private[ContentRepoSlotInterface] val PATH_TO_MAXIMUM_LARGE_STRING_LENGTH: String = "log.reducing.contentRepoSlotInterface.length"


  @Api(tags = Array("Mime Mapping"), authorizations = Array(new Authorization("basicAuth")))
  @ApiResponses(Array(new ApiResponse(code = 401, message = "Unauthorized")))
  @Path("/client/2.0/pluginSlot/contentRepoPlugin/")
  trait MimeMappingDocumentation {

    @ApiOperation(httpMethod = "POST", value = "Add a new mime type.")
    @Path("/mimeMappingAdd")
    def addMimeMappingSlot(request: MimeMappingAddRequest): MimeMappingAddResponse

    @ApiOperation(httpMethod = "POST", value = "Query a mime mapping.")
    @Path("/mimeMappingGet")
    def getMimeMappingSlot(request: MimeMappingGetRequest): MimeMappingGetResponse

    @ApiOperation(httpMethod = "POST", value = "Edit a mime mapping.")
    @Path("/mimeMappingEdit")
    def editMimeMappingSlot(request: MimeMappingEditRequest): MimeMappingEditResponse

    @ApiOperation(httpMethod = "POST", value = "Delete a mime mapping.")
    @Path("/mimeMappingDelete")
    def deleteMimeMappingSlot(request: MimeMappingDeleteRequest): MimeMappingDeleteResponse

    @ApiOperation(httpMethod = "POST", value = "List all mime mappings.")
    @Path("/mimeMappingList")
    def listMimeMappingsSlot(): MimeMappingListResponse

  }

  @Api(tags = Array("Repository"), authorizations = Array(new Authorization("basicAuth")))
  @ApiResponses(Array(new ApiResponse(code = 401, message = "Unauthorized")))
  @Path("/client/2.0/pluginSlot/contentRepoPlugin/")
  trait ContentRepositoryDocumentation {

    @ApiOperation(httpMethod = "POST", value = "Add new repository.")
    @Path("/contentRepositoryAdd")
    def addRepository(request: ContentRepoAddRequest): ContentRepoAddResponse

    @ApiOperation(httpMethod = "POST", value = "Get a repository.")
    @Path("/contentRepositoryGet")
    def getRepository(request: ContentRepoGetRequest): ContentRepoGetResponse

    @ApiOperation(httpMethod = "POST", value = "Get a repository.")
    @Path("/contentRepositoryGet")
    def getClearRepository(request: ContentRepoGetRequest): ClearContentRepoGetResponse

    @ApiOperation(httpMethod = "POST", value = "Edit a repository.")
    @Path("/contentRepositoryEdit")
    def editRepository(request: ContentRepoEditRequest): ContentRepoEditResponse

    @ApiOperation(httpMethod = "POST", value = "Delete a repository.")
    @Path("/contentRepositoryDelete")
    def deleteRepository(request: ContentRepoDeleteRequest): ContentRepoDeleteResponse

    protected case class ContentRepoFindResponse(@ApiModelProperty(value = "The list of repositories to find.") repositories: Seq[ContentRepoFindResponseItem],
                                                 @ApiModelProperty(value = "The message of this request.") message: RestMessage,
                                                 @ApiModelProperty(value = "The flag indicating a successful or faulty request.") success: Boolean = true) extends ApiMessage

    protected case class ClearContentRepoFindResponse(@ApiModelProperty(value = "The list of repositories to find.") repositories: Seq[ClearContentRepoFindResponseItem],
                                                      @ApiModelProperty(value = "The message of this request.") message: RestMessage,
                                                      @ApiModelProperty(value = "The flag indicating a successful or faulty request.") success: Boolean = true) extends ApiMessage

    @ApiOperation(httpMethod = "POST", value = "Find a repository with repository provider = 'FileSystem'.")
    @Path("/contentRepositoryFind")
    def findRepository(request: ContentRepoFindRequest): ContentRepoFindResponse

    @ApiOperation(httpMethod = "POST", value = "Find a repository with repository provider = 'ClearFileSystem'.")
    @Path("/contentRepositoryFind")
    def findClearRepository(request: ContentRepoFindRequest): ClearContentRepoFindResponse

    protected case class ContentRepoListResponse(@ApiModelProperty(value = "The list of all queried repositories.") repositories: Seq[ContentRepoListResponseItem],
                                                 @ApiModelProperty(value = "The message of this request.") message: RestMessage,
                                                 @ApiModelProperty(value = "The flag indicating a successful or faulty request.") success: Boolean = true) extends ApiMessage

    protected case class ClearContentRepoListResponse(@ApiModelProperty(value = "The list of all queried repositories.") repositories: Seq[ClearContentRepoListResponseItem],
                                                      @ApiModelProperty(value = "The message of this request.") message: RestMessage,
                                                      @ApiModelProperty(value = "The flag indicating a successful or faulty request.") success: Boolean = true) extends ApiMessage

    @ApiOperation(httpMethod = "POST", value = "List all repositories with repository provider = 'FileSystem'.")
    @Path("/contentRepositoryList")
    def listRepositories(request: ContentRepoListRequest): ContentRepoListResponse

    @ApiOperation(httpMethod = "POST", value = "List all repositories with repository provider = 'ClearFileSystem'.")
    @Path("/contentRepositoryList")
    def listClearRepositories(request: ContentRepoListRequest): ClearContentRepoListResponse

  }

  @Api(tags = Array("Folder"), authorizations = Array(new Authorization("basicAuth")))
  @ApiResponses(Array(new ApiResponse(code = 401, message = "Unauthorized")))
  @Path("/client/2.0/pluginSlot/contentRepoPlugin/")
  trait ContentFolderDocumentation {

    @ApiOperation(httpMethod = "POST", value = "Add new folder with repository provider = 'FileSystem'.")
    @Path("/contentFolderAdd")
    def addFolder(request: ContentFolderAddRequest): ContentFolderAddResponse

    @ApiOperation(httpMethod = "POST", value = "Add new folder with repository provider = 'ClearFileSystem'.")
    @Path("/contentFolderAdd")
    def addFolder(request: ClearContentFolderAddRequest): ClearContentFolderAddResponse

    @ApiOperation(httpMethod = "POST", value = "Add new folder with repository provider = 'FileSystem'. Folder is added if not already existing.")
    @Path("/contentFolderAddIfNotExisting")
    def addFolderIfNotExisting(request: ContentFoldersAddRequest): ContentFoldersAddResponse

    @ApiOperation(httpMethod = "POST", value = "Add new folder with repository provider = 'ClearFileSystem'. Folder is added if not already existing.")
    @Path("/contentFolderAddIfNotExisting")
    def addFolderIfNotExisting(request: ClearContentFoldersAddRequest): ClearContentFoldersAddResponse

    @ApiOperation(httpMethod = "POST", value = "Get a folder with repository provider = 'FileSystem'.")
    @Path("/contentFolderGet")
    def getFolder(request: ContentFolderGetRequest): ContentFolderGetResponse

    @ApiOperation(httpMethod = "POST", value = "Get a folder with repository provider = 'ClearFileSystem'.")
    @Path("/contentFolderGet")
    def getFolder(request: ClearContentFolderGetRequest): ClearContentFolderGetResponse

    @ApiOperation(httpMethod = "POST", value = "Edit a folder with repository provider = 'FileSystem'.")
    @Path("/contentFolderEdit")
    def editFolder(request: ContentFolderEditRequest): ContentFolderEditResponse

    @ApiOperation(httpMethod = "POST", value = "Edit a folder with repository provider = 'ClearFileSystem'.")
    @Path("/contentFolderEdit")
    def editFolder(request: ClearContentFolderEditRequest): ContentFolderEditResponse

    @ApiOperation(httpMethod = "POST", value = "Delete a folder with repository provider = 'FileSystem'.")
    @Path("/contentFolderDelete")
    def deleteFolder(request: ContentFolderDeleteRequest): ContentFolderDeleteResponse

    @ApiOperation(httpMethod = "POST", value = "Delete a folder with repository provider = 'ClearFileSystem'.")
    @Path("/contentFolderDelete")
    def deleteFolder(request: ClearContentFolderDeleteRequest): ContentFolderDeleteResponse

    protected case class ContentFolderFindResponse(@ApiModelProperty(value = "The list of queried folders.") folders: Seq[ContentFolderFindResponseItem],
                                                   @ApiModelProperty(value = "The message returned by this request.") message: RestMessage,
                                                   @ApiModelProperty(value = "The flag indicating a successful or faulty request.") success: Boolean = true) extends ApiMessage

    protected case class ClearContentFolderFindResponse(@ApiModelProperty(value = "The list of queried folders.") folders: Seq[ClearContentFolderFindResponseItem],
                                                        @ApiModelProperty(value = "The message returned by this request.") message: RestMessage,
                                                        @ApiModelProperty(value = "The flag indicating a successful or faulty request.") success: Boolean = true) extends ApiMessage

    @ApiOperation(httpMethod = "POST", value = "Find a folder with repository provider = 'FileSystem'.")
    @Path("/contentFolderFind")
    def findFolder(request: ContentFolderFindRequest): ContentFolderFindResponse

    @ApiOperation(httpMethod = "POST", value = "Find a folder with repository provider = 'ClearFileSystem'.")
    @Path("/contentFolderFind")
    def findFolder(request: ClearContentFolderFindRequest): ClearContentFolderFindResponse

    protected case class ContentFolderListResponse(@ApiModelProperty(value = "The list of queried folders.") folders: Seq[ContentFolderListResponseItem],
                                                   @ApiModelProperty(value = "The message returned by this request.") message: RestMessage,
                                                   @ApiModelProperty(value = "The flag indicating a successful or faulty request.") success: Boolean = true) extends ApiMessage

    protected case class ClearContentFolderListResponse(@ApiModelProperty(value = "The list of queried folders.") folders: Seq[ClearContentFolderListResponseItem],
                                                        @ApiModelProperty(value = "The message returned by this request.") message: RestMessage,
                                                        @ApiModelProperty(value = "The flag indicating a successful or faulty request.") success: Boolean = true) extends ApiMessage

    @ApiOperation(httpMethod = "POST", value = "List all folders with repository provider = 'FileSystem'.")
    @Path("/contentFolderList")
    def listFolders(request: ContentFolderListRequest): ContentFolderListResponse

    @ApiOperation(httpMethod = "POST", value = "List all folders with repository provider = 'ClearFileSystem'.")
    @Path("/contentFolderList")
    def listFolders(request: ClearContentFolderListRequest): ClearContentFolderListResponse

  }

  @Api(tags = Array("File"), authorizations = Array(new Authorization("basicAuth")))
  @ApiResponses(Array(new ApiResponse(code = 401, message = "Unauthorized")))
  @Path("/client/2.0/pluginSlot/contentRepoPlugin/")
  trait ContentFileDocumentation {

    @ApiOperation(httpMethod = "POST", value = "Add new file with repository provider = 'FileSystem'.")
    @Path("/contentFileAdd")
    def addFile(request: ContentFileAddRequest): ContentFileAddResponse

    @ApiOperation(httpMethod = "POST", value = "Add new file with repository provider = 'ClearFileSystem'.")
    @Path("/contentFileAdd")
    def addFile(request: ClearContentFileAddRequest): ClearContentFileAddResponse

    @ApiOperation(httpMethod = "POST", value = "Get a file with repository provider = 'FileSystem'.")
    @Path("/contentFileGet")
    def getFile(request: ContentFileGetRequest): ContentFileGetResponse

    @ApiOperation(httpMethod = "POST", value = "Get a file with repository provider = 'ClearFileSystem'.")
    @Path("/contentFileGet")
    def getFile(request: ClearContentFileGetRequest): ClearContentFileGetResponse

    @ApiOperation(httpMethod = "POST", value = "Get metadata of a file with repository provider = 'FileSystem'.")
    @Path("/contentFileGetMetadata")
    def getFileMetadata(request: ContentFileGetRequest): ContentFileGetMetadataResponse

    @ApiOperation(httpMethod = "POST", value = "Get metadata of a file with repository provider = 'ClearFileSystem'.")
    @Path("/contentFileGetMetadata")
    def getFileMetadata(request: ClearContentFileGetRequest): ClearContentFileGetMetadataResponse

    protected case class GetFileMetaBatchResponseFileProvider(
                                                               @ApiModelProperty(value = "The meta data of the queried files.") fileMetadata: Seq[ContentFileGetMetadataBatchedResponseItem],
                                                               @ApiModelProperty(value = "The message returned by this request.") message: RestMessage,
                                                               @ApiModelProperty(value = "The flag indicating a successful or faulty request.") success: Boolean = true) extends ApiMessage

    protected case class GetFileMetaBatchResponseClearFileProvider(
                                                                    @ApiModelProperty(value = "The meta data of the queried files.") fileMetadata: Seq[ClearContentFileGetMetadataBatchedResponseItem],
                                                                    @ApiModelProperty(value = "The message returned by this request.") message: RestMessage,
                                                                    @ApiModelProperty(value = "The flag indicating a successful or faulty request.") success: Boolean = true) extends ApiMessage

    @ApiOperation(httpMethod = "POST", value = "Get metadata of files with repository provider = 'FileSystem'.")
    @Path("/contentFileGetMetadataBatched")
    def getMetadataBatched(request: ContentFileGetMetadataBatchedRequest): GetFileMetaBatchResponseFileProvider

    @ApiOperation(httpMethod = "POST", value = "Get metadata of files with repository provider = 'ClearFileSystem'.")
    @Path("/contentFileGetMetadataBatched")
    def getMetadataBatched(request: ClearContentFileGetMetadataBatchedRequest): GetFileMetaBatchResponseClearFileProvider

    @ApiOperation(httpMethod = "POST", value = "Edit a file with repository provider = 'FileSystem'.")
    @Path("/contentFileEdit")
    def editFile(request: ContentFileEditRequest): ContentFileEditResponse

    @ApiOperation(httpMethod = "POST", value = "Edit a file with repository provider = 'ClearFileSystem'.")
    @Path("/contentFileEdit")
    def editFile(request: ClearContentFileEditRequest): ContentFileEditResponse

    @ApiOperation(httpMethod = "POST", value = "Delete a file with repository provider = 'FileSystem'.")
    @Path("/contentFileDelete")
    def deleteFile(request: ContentFileDeleteRequest): ContentFileDeleteResponse

    @ApiOperation(httpMethod = "POST", value = "Delete a file with repository provider = 'ClearFileSystem'.")
    @Path("/contentFileDelete")
    def deleteFile(request: ClearContentFileDeleteRequest): ContentFileDeleteResponse

    protected case class ContentFileFindResponse(@ApiModelProperty(value = "The list of queried files.") files: Seq[ContentFileFindResponseItem],
                                                 @ApiModelProperty(value = "The message returned by this request.") message: RestMessage,
                                                 @ApiModelProperty(value = "The flag indicating a successful or faulty request.") success: Boolean = true) extends ApiMessage

    protected case class ClearContentFileFindResponse(@ApiModelProperty(value = "The list of queried files.") files: Seq[ClearContentFileFindResponseItem],
                                                      @ApiModelProperty(value = "The message returned by this request.") message: RestMessage,
                                                      @ApiModelProperty(value = "The flag indicating a successful or faulty request.") success: Boolean = true) extends ApiMessage

    @ApiOperation(httpMethod = "POST", value = "Find a file with repository provider = 'FileSystem'.")
    @Path("/contentFileFind")
    def findFile(request: ContentFileFindRequest): ContentFileFindResponse

    @ApiOperation(httpMethod = "POST", value = "Find a file with repository provider = 'ClearFileSystem'.")
    @Path("/contentFileFind")
    def findFile(request: ClearContentFileFindRequest): ClearContentFileFindResponse

    protected case class ContentFileListResponse(@ApiModelProperty(value = "The list of queried files.") files: Seq[ContentFileListResponseItem],
                                                 @ApiModelProperty(value = "The message returned by this request.") message: RestMessage,
                                                 @ApiModelProperty(value = "The flag indicating a successful or faulty request.") success: Boolean = true) extends ApiMessage

    protected case class ClearContentFileListResponse(@ApiModelProperty(value = "The list of queried files.") files: Seq[ClearContentFileListResponseItem],
                                                      @ApiModelProperty(value = "The message returned by this request.") message: RestMessage,
                                                      @ApiModelProperty(value = "The flag indicating a successful or faulty request.") success: Boolean = true) extends ApiMessage

    @ApiOperation(httpMethod = "POST", value = "List all files with missing folder id.")
    @Path("/contentFileList")
    def listFilesWithoutFolderId(request: ContentFileListRequest): io.simplifier.plugin.contentrepo.definitions.caseClasses.ContentFolderCaseClasses.ContentFileListResponseAll

    @ApiOperation(httpMethod = "POST", value = "List all files with repository provider = 'FileSystem'.")
    @Path("/contentFileList")
    def listFiles(request: ContentFileListRequest): ContentFileListResponse

    @ApiOperation(httpMethod = "POST", value = "List all files with repository provider = 'ClearFileSystem'.")
    @Path("/contentFileList")
    def listFiles(request: ClearContentFileListRequest): ClearContentFileListResponse

  }

}