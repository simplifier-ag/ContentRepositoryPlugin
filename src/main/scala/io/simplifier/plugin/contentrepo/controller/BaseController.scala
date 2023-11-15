package io.simplifier.plugin.contentrepo.controller

import akka.http.scaladsl.marshalling.{PredefinedToResponseMarshallers, ToEntityMarshaller, ToResponseMarshaller}
import akka.http.scaladsl.model.{StatusCode, StatusCodes}
import io.simplifier.plugin.contentrepo.definitions.exceptions.CommonExceptions._
import io.simplifier.plugin.contentrepo.dto.RestMessages.RestMessage
import io.simplifier.plugin.contentrepo.model.{ContentFile, ContentFolder, ContentRepository, SecurityScheme}
import io.simplifier.plugin.contentrepo.permission.PermissionHandler._
import io.simplifier.pluginapi.{GrantedPermission, UserSession}
import io.simplifier.pluginapi.rest.PluginHeaders.RequestSource
import io.simplifier.pluginbase.helpers.SimplifierServerSettings
import io.simplifier.pluginbase.util.api.ApiMessage
import io.simplifier.pluginbase.util.json.SimplifierFormats
import io.simplifier.pluginbase.util.logging.Logging
import org.json4s.{Extraction, JValue, MappingException}

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}

/**
  * Base class for Content Repo controllers.
  */
abstract class BaseController() extends Logging with SimplifierFormats {

  import io.simplifier.plugin.contentrepo.controller.BaseController._

  /**
    * Extracts the json into the case class or throws a Custom Exception if it is not possible
    *
    * @param json the json to be cast into the case class
    * @param m the manifest for the case class
    * @tparam A the case class
    * @return the json extracted into the case class
    */
  def extractWithCustomError[A](json: JValue)(implicit m: Manifest[A]): A = {
    try {
      json.extract[A]
    } catch {
      case ex: Throwable => throw CommonMissingParametersException(ex.getMessage)
    }
  }

  /**
    * Normalize mime type extension
    */
  def normalizeExtension(extension: String): String = extension.trim.toLowerCase

  /**
    * Transforms an empty description into a non-empty one space character containing description for ORACLE.
    *
    * @param description the description.
    * @return the description or a description containing one space character '\u0020'
    */
  protected def sanitizeDescription(description: String): String = if (description.isEmpty) " " else description

  /**
    * Check if security scheme is valid and throw message exception otherwise.
    */
  protected def checkSecuritySchemeExists(securityScheme: String, exc: => OperationFailureMessage): Unit = {
    if (SecurityScheme.parse(securityScheme).isEmpty) {
      throw exc
    }
  }

  type AsyncSlotImplNoArg = (UserSession, RequestSource, ExecutionContext) => Future[ApiMessage]

  /**
    * Define slot operation without input argument.
    *
    * @param msgError error message producer
    * @param exec execute function
    * @param em marshaller for response type
    * @tparam B response type
    * @return slot function
    */
  def asyncSlotOperationNoArg(msgError: String => RestMessage)
                             (exec: (UserSession, RequestSource, ExecutionContext) => Future[ApiMessage]): AsyncSlotImplNoArg = {
    (userSession, requestSource, ec)=>
      implicit val executionContext = ec
      exec(userSession, requestSource, ec).recoverWith {
        case failure: OperationFailure =>
          Future.successful(failure.toResponse)
        case OperationFailureMessage(msg, statusCode) =>
          Future.successful(OperationFailure(msgError(msg), statusCode).toResponse)
        case other =>
          logger.error("Error in operation", other)
          Future.successful(OperationFailure(msgError("unexpected error")).toResponse)
      }
  }

  /**
    * Define slot operation.
    *
    * @param msgError error message producer
    * @param exec execute function
    * @param em marshaller for response type
    * @tparam A parameter type
    * @tparam B response type
    * @return slot function
    */
  def asyncSlotOperation[A: Manifest](msgError: String => RestMessage)
                                     (exec: (A, UserSession, RequestSource, ExecutionContext) => Future[ApiMessage]): AsyncSlotImpl = {
    (json, userSession, requestSource, ec) =>
      implicit val executionContext = ec
      Future(Extraction.extract[A](json)).flatMap(a =>
        exec(a, userSession, requestSource, ec)
      ).recoverWith {
        case failure: OperationFailure =>
          Future.successful(failure.toResponse)
        case OperationFailureMessage(msg, statusCode) =>
          Future.successful(OperationFailure(msgError(msg), statusCode).toResponse)
        case thr: MappingException =>
          logger.debug("Error decoding argument: " + json, thr)
          Future.successful(OperationFailure(msgError("Unable to parse argument: " + thr.getMessage), statusCode = StatusCodes.BadRequest).toResponse)
        case other =>
          logger.error("Error in operation", other)
          Future.successful(OperationFailure(msgError("unexpected error")).toResponse)
      }
  }

  /**
    * Check if field is non-empty and throw message exception otherwise.
    */
  def checkNotEmpty(toCheck: String, exc: => OperationFailureMessage): Unit = {
    if (toCheck == null || toCheck.length == 0) {
      throw exc
    }
  }
}

object BaseController extends SimplifierFormats with Logging {

  type AsyncSlotImpl = (JValue, UserSession, RequestSource, ExecutionContext) => Future[ApiMessage]

  type AsyncSlotImplOpt = (Option[JValue], UserSession, RequestSource, ExecutionContext) => Future[ApiMessage]

  case class OperationFailureMessage(msg: String, statusCode: StatusCode = StatusCodes.InternalServerError) extends Exception(msg)

  case class OperationFailure(msg: RestMessage, statusCode: StatusCode = StatusCodes.InternalServerError)
    extends Exception(msg.msgText) {

    def toResponse: OperationFailureResponse = OperationFailureResponse(msg)

  }

  object OperationFailure {

    implicit def responseMarshaller: ToResponseMarshaller[OperationFailure] =
      PredefinedToResponseMarshallers
        .fromStatusCodeAndValue[StatusCode, OperationFailureResponse](status => status, implicitly[ToEntityMarshaller[ApiMessage]])
        .compose(of => (of.statusCode, of.toResponse))

  }

  case class OperationFailureResponse(message: RestMessage, success: Boolean = false) extends ApiMessage {

    def withStatus(statusCode: StatusCode = StatusCodes.InternalServerError): OperationFailure = OperationFailure(message, statusCode)

  }

  /**
    * Data model holding all relevant PermissionObjects of a user session.
    */
  class ContentRepoPermissions(granted: Seq[GrantedPermission]) {

    import io.simplifier.plugin.contentrepo.permission.ContentRepoPermission._

    val characteristicSets: Seq[Map[String, Set[String]]] = granted.map(_.characteristics)

    def hasContentRepoPermission: Boolean = characteristicSets.nonEmpty

    def hasMimeMapping: Boolean = hasCharacteristic(characteristicManageMimeMappings)

    private def hasCharacteristic(characteristic: String): Boolean = characteristicSets.exists(c => c.getOrElse(characteristic, Set()).contains("true"))

    private def checkCharacteristic(characteristic: String): Unit = {
      if (!hasCharacteristic(characteristic)) {
        throw PermissionDeniedMissingCharacteristic(characteristic)
      }
    }

    def checkAdditionalPermission(characteristic: String): Unit = {
      if (SimplifierServerSettings.activatePluginPermissionCheck) {
          checkCharacteristic(characteristic)
      }
    }

    def checkMimeMapping(): Unit = checkCharacteristic(characteristicManageMimeMappings)

    def checkCreateRepo(): Unit = checkCharacteristic(characteristicCreateRepository)

    def hasPermissionObjectId(objectType: String, objectId: String): Boolean = {
      characteristicSets.exists {
        characteristicMap =>
          characteristicMap.getOrElse(characteristicPermissionObjectType, Set()).contains(objectType) &&
            characteristicMap.getOrElse(characteristicPermissionObjectID, Set()).contains(objectId)
      }
    }

    def hasPermissionObjectId(repo: ContentRepository): Boolean = {
      hasPermissionObjectId(repo.permissionObjectType, repo.permissionObjectID)
    }

    def hasPermissionObjectId(folder: ContentFolder): Boolean =
      hasPermissionObjectId(folder.permissionObjectType, folder.permissionObjectID)

     def hasPermissionObjectId(file: ContentFile): Boolean =
      hasPermissionObjectId(file.permissionObjectType, file.permissionObjectID)

     def checkPermissionObjectId(objectType: String, objectId: String): Unit = {
      if (!hasPermissionObjectId(objectType, objectId)) {
        throw PermissionDeniedMissingObjectId(objectType, objectId)
      }
    }

    def checkPermissionObjectId(repo: ContentRepository): Unit = {
      checkPermissionObjectId(repo.permissionObjectType, repo.permissionObjectID)
    }

     def checkPermissionObjectId(folder: ContentFolder): Unit =
      checkPermissionObjectId(folder.permissionObjectType, folder.permissionObjectID)

     def checkPermissionObjectId(file: ContentFile): Unit =
      checkPermissionObjectId(file.permissionObjectType, file.permissionObjectID)

    def checkPermissionToAccessFile(file: ContentFile, allowPublicAccess: Boolean)
                                   (implicit userSession: UserSession): Unit = {
      val hasPermission = Try {
        checkCharacteristic(characteristicAccessForeignFile)
      } match {
        case Success(_) => true
        case Failure(_) => file.userId match {
          case Some(v) => userSession.userIdOpt match {
            case Some(u) => v == u
            case None => false
          }
          case None => true
        }
      }

      val fileIsPrivate = file.securitySchemeID == SecurityScheme.Private.name

      if((!allowPublicAccess && fileIsPrivate && !hasPermission)
        || (allowPublicAccess && fileIsPrivate && !hasPermission)
        || (!allowPublicAccess && !fileIsPrivate && !hasPermission)) {
        val error = PermissionDeniedInsufficientPrivileges(userSession.userIdOpt)
        logger.info(error.getMessage)
        throw error
      }
    }

  }
}