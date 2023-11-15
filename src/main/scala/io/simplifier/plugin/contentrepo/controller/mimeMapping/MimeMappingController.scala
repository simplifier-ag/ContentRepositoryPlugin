package io.simplifier.plugin.contentrepo.controller.mimeMapping

import io.simplifier.plugin.contentrepo.controller.BaseController
 import io.simplifier.plugin.contentrepo.controller.BaseController.AsyncSlotImpl
 import io.simplifier.plugin.contentrepo.dao.MimeMappingDao
 import io.simplifier.plugin.contentrepo.definitions.caseClasses.MimeMappingCaseClasses._
 import io.simplifier.plugin.contentrepo.definitions.exceptions.MimeMappingExceptions._
 import io.simplifier.plugin.contentrepo.dto.RestMessages._
 import io.simplifier.plugin.contentrepo.model.MimeMapping
 import io.simplifier.plugin.contentrepo.permission.PermissionHandler
 import io.simplifier.pluginapi.UserSession
 import io.simplifier.pluginapi.rest.PluginHeaders.RequestSource
 import io.simplifier.pluginbase.util.json.SimplifierFormats
 import org.apache.commons.io.FilenameUtils
 import org.squeryl.PrimitiveTypeMode._

 import scala.concurrent.{ExecutionContext, Future}
 import scala.util.Try

/**
  * Controller for Mime Mappings.
  */
class MimeMappingController(permissionHandler: PermissionHandler,
                            mimeMappingDao: MimeMappingDao = new MimeMappingDao)
  extends BaseController with SimplifierFormats {

  def addMimeMapping(extension: String, mimeType: String)(implicit userSession: UserSession,
                                                          requestSource: RequestSource,
                                                          ec: ExecutionContext): Future[MimeMapping] = {
    permissionHandler.checkPermissions().map { permissions =>
      val extensionNormalized = normalizeExtension(extension)
      checkNotEmpty(extensionNormalized, MimeMappingEmptyExtension)
      checkNotEmpty(mimeType.trim, MimeMappingEmptyMime)
      permissions.checkMimeMapping()
      mimeMappingDao.insertIfNotExisting(new MimeMapping(0, extensionNormalized, mimeType), _.ext === extensionNormalized) match {
        case Left(mm) =>
          logger.debug("Found existing: " + mm)
          throw MimeMappingAlreadyExisting
        case Right(inserted) => inserted
      }
    }
  }

  def editMimeMapping(extension: String, mimeType: String)(implicit userSession: UserSession,
                                                           requestSource: RequestSource,
                                                           ec: ExecutionContext): Future[MimeMapping] = {
    permissionHandler.checkPermissions().map { permissions =>
      val extensionNormalized = normalizeExtension(extension)
      checkNotEmpty(extensionNormalized, MimeMappingEmptyExtension)
      checkNotEmpty(mimeType.trim, MimeMappingEmptyMime)
      permissions.checkMimeMapping()
      val mapping: MimeMapping = getMimeTypeByExtension(extensionNormalized, MimeMappingNotFound)
      mapping.mimeType = mimeType
      mimeMappingDao.update(mapping)
      mapping
    }
  }

  def deleteMimeMapping(extension: String)(implicit userSession: UserSession,
                                           requestSource: RequestSource,
                                           ec: ExecutionContext): Future[MimeMapping] = {
    permissionHandler.checkPermissions().map { permissions =>
      val extensionNormalized = normalizeExtension(extension)
      checkNotEmpty(extensionNormalized, MimeMappingEmptyExtension)
      permissions.checkMimeMapping()
      val mapping: MimeMapping = getMimeTypeByExtension(extensionNormalized, MimeMappingNotFound)
      mimeMappingDao.delete(mapping.id)
      mapping
    }
  }

  def getMimeMapping(extension: String)(implicit userSession: UserSession,
                                        requestSource: RequestSource,
                                        ec: ExecutionContext): Future[Option[MimeMapping]] = {
    permissionHandler.checkPermissions().map { permissions =>
      val extensionNormalized = normalizeExtension(extension)
      checkNotEmpty(extensionNormalized, MimeMappingEmptyExtension)
      permissions.checkMimeMapping()
      getMimeTypeByExtensionOpt(extensionNormalized)
    }
  }

  def listMimeMappings()(implicit userSession: UserSession, requestSource: RequestSource, ec: ExecutionContext): Future[Seq[MimeMapping]] = {
    permissionHandler.checkPermissions().map { permissions =>
      permissions.checkMimeMapping()
      Try(mimeMappingDao.getAll()).fold(e => throw MimeMappingRetrievalFailure(true).initCause(e), mm => mm)
    }
  }

  def getMimeTypeForDownload(fileName: String): Option[String] = {
    val extensionNormalized = normalizeExtension(FilenameUtils.getExtension(fileName))
    getMimeTypeByExtensionOpt(extensionNormalized) map (_.mimeType)
  }

  def getMimeTypeAndExtensionForDownload(fileName: String): MimeMappingReturn = {
    val extensionNormalized: String = normalizeExtension(FilenameUtils.getExtension(fileName))
    (getMimeTypeByExtensionOpt(extensionNormalized) map (mime => MimeMappingReturn(mime.ext, mime.mimeType))).getOrElse(MimeMappingReturn(extensionNormalized, ""))
  }

  /**
    * Gets the Mimetype for the specified extension
    *
    * @param extension the extension to get the mime type for
    * @return the extension and the mime type or an empty string
    */
  def getMimeTypeAndExtensionForDownloadByExtension(extension: String): MimeMappingReturn = {
    val extensionNormalized: String = normalizeExtension(extension)
    (getMimeTypeByExtensionOpt(extensionNormalized) map (mime => MimeMappingReturn(mime.ext, mime.mimeType))).getOrElse(MimeMappingReturn(extensionNormalized, ""))
  }

  private[this] def getMimeTypeByExtensionOpt(extension: String): Option[MimeMapping] = {
    mimeMappingDao.findByExt(extension).fold(e => throw MimeMappingRetrievalFailure().initCause(e), mm => mm)
  }

  private[this] def getMimeTypeByExtension(extension: String, exception: Throwable): MimeMapping = {
    mimeMappingDao.findByExt(extension).fold(e => throw MimeMappingRetrievalFailure().initCause(e), mm => mm).getOrElse(throw exception)
  }

  /*
   * Slots
   */

  val addMimeMappingSlot: AsyncSlotImpl = asyncSlotOperation(addMimeMappingError) {
    (arg: MimeMappingAddRequest, userSession: UserSession, requestSource: RequestSource, ec: ExecutionContext) =>
      implicit val executionContext = ec
      logger.debug(s"Add Mime Mapping ${arg.extension} -> ${arg.mimetype}")
      addMimeMapping(arg.extension, arg.mimetype)(userSession, requestSource, ec).map { _ =>
        MimeMappingAddResponse(addMimeMappingSuccess)
      }
  }

  val getMimeMappingSlot: AsyncSlotImpl = asyncSlotOperation(getMimeMappingError) {
    (arg: MimeMappingGetRequest, userSession: UserSession, requestSource: RequestSource, ec: ExecutionContext) =>
      implicit val executionContext = ec
      logger.debug(s"Get Mime Mapping for ${arg.extension}")
      getMimeMapping(arg.extension)(userSession, requestSource, ec).map { resOpt =>
        val res = resOpt.getOrElse(throw MimeMappingNotFound)
        MimeMappingGetResponse(res.ext, res.mimeType, getMimeMappingSuccess)
      }
  }

  val listMimeMappingsSlot: AsyncSlotImplNoArg = asyncSlotOperationNoArg(listMimeMappingError) {
    (userSession: UserSession, requestSource: RequestSource, ec: ExecutionContext) =>
      implicit val executionContext = ec
      logger.debug("List Mime Mappings")
      listMimeMappings()(userSession, requestSource, ec).map { res =>
        MimeMappingListResponse(res map {
          mm => MimeMappingListResponseItem(mm.ext, mm.mimeType)
        }, listMimeMappingSuccess)
      }
  }

  val editMimeMappingSlot: AsyncSlotImpl = asyncSlotOperation(editMimeMappingError) {
    (arg: MimeMappingEditRequest, userSession: UserSession, requestSource: RequestSource, ec: ExecutionContext) =>
      implicit val executionContext = ec
      logger.debug(s"Edit Mime Mapping ${arg.extension} -> ${arg.mimetype}")
      editMimeMapping(arg.extension, arg.mimetype)(userSession, requestSource, ec).map { _ =>
        MimeMappingEditResponse(editMimeMappingSuccess)
      }
  }

  val deleteMimeMappingSlot: AsyncSlotImpl = asyncSlotOperation(deleteMimeMappingError) {
    (arg: MimeMappingDeleteRequest, userSession: UserSession, requestSource: RequestSource, ec: ExecutionContext) =>
      implicit val executionContext = ec
      logger.debug(s"Delete Mime Mapping for ${arg.extension}")
      deleteMimeMapping(arg.extension)(userSession, requestSource, ec).map { _ =>
        MimeMappingDeleteResponse(deleteMimeMappingSuccess)
      }
  }

}
