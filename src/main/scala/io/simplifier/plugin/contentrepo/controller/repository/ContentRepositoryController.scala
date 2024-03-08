package io.simplifier.plugin.contentrepo.controller.repository

import io.simplifier.plugin.contentrepo.controller.BaseController
 import io.simplifier.plugin.contentrepo.dao.ContentRepositoryDao
 import io.simplifier.plugin.contentrepo.definitions.Constants._
 import io.simplifier.plugin.contentrepo.definitions.LogMessages._
 import io.simplifier.plugin.contentrepo.definitions.caseClasses.ContentRepositoryCaseClasses._
 import io.simplifier.plugin.contentrepo.definitions.exceptions.ContentRepositoryExceptions._
 import io.simplifier.plugin.contentrepo.dto.RestMessages._
 import io.simplifier.plugin.contentrepo.model.ContentRepository
 import io.simplifier.plugin.contentrepo.model.provider.{ClearFileSystemProvider, FileSystemProvider}
 import io.simplifier.plugin.contentrepo.permission.PermissionHandler
 import io.simplifier.pluginapi.UserSession
 import io.simplifier.pluginapi.rest.PluginHeaders.RequestSource
 import io.simplifier.pluginbase.model.UserTransaction
 import io.simplifier.pluginbase.util.api.ApiMessage
 import org.json4s._
 import org.squeryl.PrimitiveTypeMode._

 import scala.concurrent.{ExecutionContext, Future}
 import scala.util.Try

/**
  * Controller for Content Repositories.
  */
abstract class ContentRepositoryController(clearFileSystemProvider: ClearFileSystemProvider,
                                           permissionHandler: PermissionHandler,
                                           contentRepositoryDao: ContentRepositoryDao = new ContentRepositoryDao,
                                           userTransaction: UserTransaction = new UserTransaction)
  extends BaseController {

  type ConfigMap = Map[String, String]

  val PROVIDER_NAME: String

  /**
    * Adds a repository if all permissions are granted and all values are valid
    *
    * @param json        the json received from the REST call
    * @param userSession the implicit user session
    * @return the id and description of the newly created repo
    */
  def addRepository(json: JValue)(implicit userSession: UserSession, requestSource: RequestSource, ec: ExecutionContext): Future[ContentRepoAddResponse] = {
    logger.debug(slotMessageBegin(ACTION_SLOT_ADD, ASPECT_REPOSITORY, PROVIDER_NAME, json))

    permissionHandler.checkPermissions().map { permissions =>
      val repoData = extractWithCustomError[ContentRepoAddRequest](json)

      checkNotEmpty(repoData.permissionObjectType, ContentRepoEmptyPermissionObjectType)
      checkNotEmpty(repoData.provider, ContentRepoEmptyProvider)
      checkNotEmpty(repoData.name, ContentRepoEmptyName)
      permissions.checkCreateRepo()
      permissions.checkPermissionObjectId(repoData.permissionObjectType, repoData.permissionObjectID)

      val newRepo = new ContentRepository(
        0,
        repoData.name,
        repoData.description.map(sanitizeDescription),
        repoData.permissionObjectType,
        repoData.permissionObjectID,
        repoData.provider)

      //TODO LOG in Simplifier
      val contentRepo = createRepo(newRepo)
      logger.debug(slotMessageEnd(ACTION_SLOT_ADD, ASPECT_REPOSITORY, PROVIDER_NAME, contentRepo))
      ContentRepoAddResponse(contentRepo.id, contentRepo.description.getOrElse(" "), addRepositorySuccess)
    }
  }

  /**
    * Base function to get repository
    * Implemented in the provider specific controllers
    *
    * @param json        the json received from the REST call
    * @param userSession the implicit user session
    * @return depends on the implementation
    */
  def getRepository(json: JValue)(implicit userSession: UserSession, requestSource: RequestSource, ec: ExecutionContext): Future[ApiMessage] = {
    logger.debug(slotMessageBegin(ACTION_SLOT_GET, ASPECT_REPOSITORY, PROVIDER_NAME, json))
    permissionHandler.checkPermissions().map { permissions =>
      val repo = getRepo(json).getOrElse(throw ContentRepoNotFound)
      permissions.checkPermissionObjectId(repo)
      logger.debug(slotMessageEnd(ACTION_SLOT_GET, ASPECT_REPOSITORY, PROVIDER_NAME, repo))
      mapToGetResponse(repo)
    }
  }

  protected def mapToGetResponse(repo: ContentRepository): ApiMessage

  /**
    * Edits the repository
    * Possible changes are the description, the permission object type/id and the name
    *
    * @param json        the json received from the REST call
    * @param userSession the implicit user session
    * @return success or error message
    */
  def editRepository(json: JValue)(implicit userSession: UserSession, requestSource: RequestSource, ec: ExecutionContext): Future[ContentRepoEditResponse] = {
    logger.debug(slotMessageBegin(ACTION_SLOT_EDIT, ASPECT_REPOSITORY, PROVIDER_NAME, json))
    permissionHandler.checkPermissions().map { permissions =>
      val repoData = extractWithCustomError[ContentRepoEditRequest](json)

      checkNotEmpty(repoData.permissionObjectType, ContentRepoEmptyPermissionObjectType)
      checkNotEmpty(repoData.name, ContentRepoEmptyName)
      permissions.checkPermissionObjectId(repoData.permissionObjectType, repoData.permissionObjectID)

      contentRepositoryDao.getByName(repoData.name).get.foreach(r => if (r.id != repoData.id) throw ContentRepoNameExisting)

      val repo = contentRepositoryDao.getById(repoData.id).getOrElse(throw ContentRepoNotFound)
      permissions.checkPermissionObjectId(repo)

      val newRepo = new ContentRepository(repo.id, repoData.name, repoData.description, repoData.permissionObjectType, repoData.permissionObjectID, repo.provider)
      updateRepo(newRepo)
      if (repo.name != repoData.name) clearFileSystemProvider.renameRepo(repo.name, repoData.name)
      logger.debug(slotMessageEnd(ACTION_SLOT_EDIT, ASPECT_REPOSITORY, PROVIDER_NAME))
      ContentRepoEditResponse(editRepositorySuccess)
    }
  }

  /**
    * Deletes the repository from the database and the file system if it is empty
    *
    * @param json        the json received from the REST call
    * @param userSession the implicit user session
    * @return Success or Error message
    */
  def deleteRepository(json: JValue)(implicit userSession: UserSession,
                                     requestSource: RequestSource,
                                     ec: ExecutionContext): Future[ContentRepoDeleteResponse] = {
    logger.debug(slotMessageBegin(ACTION_SLOT_DELETE, ASPECT_REPOSITORY, PROVIDER_NAME, json))
    permissionHandler.checkPermissions().map { permissions =>
      val repoData = json.extract[ContentRepoDeleteRequest]
      val repo: ContentRepository = getContentRepository(repoData.id, ContentRepoNotFound)
      permissions.checkPermissionObjectId(repo)

      if (hasFolders(repoData.id)) throw ContentRepoNotEmptyDelete

      //TODO LOG in Simplifier
      userTransaction.inSingleTransaction {

        Try(contentRepositoryDao.delete(repo))
          .fold(e => throw e,
            _ => logAndReturn[ContentRepository](repo, logger.trace, contentActionTraceEnd[ContentRepository]
              (createContentRepositoryMetaContent, repo, ASPECT_REPOSITORY_BASE, ACTION_DELETE)))

        clearFileSystemProvider.deleteRepo(repo.name)
      }.get

      logger.debug(slotMessageEnd(ACTION_SLOT_DELETE, ASPECT_REPOSITORY, PROVIDER_NAME))
      ContentRepoDeleteResponse(deleteRepositorySuccess)
    }
  }

  /**
    * Finds the repository with the given name and filters the result concerning permissions
    *
    * @param json        the json received from the REST call
    * @param userSession the implicit user session
    * @return information for all found repositories
    */
  def findRepository(json: JValue)(implicit userSession: UserSession, requestSource: RequestSource, ec: ExecutionContext): Future[ContentRepoFindResponse] = {
    logger.debug(slotMessageBegin(ACTION_SLOT_FIND, ASPECT_REPOSITORY, PROVIDER_NAME, json))
    permissionHandler.checkPermissions().map { permissions =>
      val repoData = json.extract[ContentRepoFindRequest]

      //TODO LOG in Simplifier
      val repos = contentRepositoryDao.getByName(repoData.name)
        .fold(e => throw e,
          res => res map { repo =>
            permissions.checkPermissionObjectId(repo.permissionObjectType, repo.permissionObjectID)
            logAndReturn[ContentRepository](repo, logger.trace, contentActionTraceEnd[ContentRepository](createContentRepositoryMetaContent, repo, ASPECT_REPOSITORY_BASE, ACTION_READ))
          })
      logger.debug(slotMessageEnd(ACTION_SLOT_FIND, ASPECT_REPOSITORY, PROVIDER_NAME, repos))
      ContentRepoFindResponse(repos.toSeq map (repo => mapToFindResponse(repo)), listRepositorySuccess)
    }

  }

  /**
    * Lists all repositories and filters the result concerning permissions
    * If the parameter provider is given the result is filtered to contain only repositories for the specified provider
    * Contains the deprecated config return value
    *
    * @param json        the json received from the REST call
    * @param userSession the implicit user session
    * @return information for all listed repositories
    */
  def listRepositories(json: JValue)(implicit userSession: UserSession,
                                     requestSource: RequestSource,
                                     ec: ExecutionContext): Future[ContentRepoListResponse] = {
    logger.debug(slotMessageBegin(ACTION_SLOT_LIST, ASPECT_REPOSITORY, PROVIDER_NAME, json))
    permissionHandler.checkPermissions().flatMap { _ =>
      val repoData = json.extract[ContentRepoListRequest]
      listContentRepositories(repoData.provider)
    }.map { res =>
      logger.debug(slotMessageEnd(ACTION_SLOT_LIST, ASPECT_REPOSITORY, PROVIDER_NAME, res))
      ContentRepoListResponse(res map {
        repo =>
          repo.provider match {
            case FileSystemProvider.providerId =>
              //Deprecated config
              val repoConfig = Map("basedir" -> repo.name)
              ContentRepoListResponseItem(repo.id, repo.name, repo.description.getOrElse(""), repo.permissionObjectType,
                repo.permissionObjectID, repo.provider, repoConfig)
            case ClearFileSystemProvider.providerId =>
              ClearContentRepoListResponseItem(repo.id, repo.name, repo.description.getOrElse(""), repo.permissionObjectType,
                repo.permissionObjectID, repo.provider)
            case _ => throw ContentRepoUnknownProvider
          }
      }, listRepositorySuccess)
    }
  }

  /**
    * Maps the content repo to the list repo response Item
    * Contains the deprecated config return value
    *
    * @param repoOpt the repo to be mapped
    * @return the mapped list response item
    */
  def mapContentRepoModelToListResponseItem(repoOpt: Option[ContentRepository]): Option[ContentRepoListResponseItem] = {
    repoOpt.map { repo =>
      //deprecated config
      val config = Map("basedir" -> repo.name)
      ContentRepoListResponseItem(repo.id, repo.name, repo.description.getOrElse(""), repo.permissionObjectType, repo.permissionObjectID, repo.provider, config)
    }
  }

  protected def getContentRepository(id: Int, exception: Throwable): ContentRepository = {
    Try(contentRepositoryDao.getById(id))
      .fold(e => throw ContentRepoRetrievalFailure.initCause(e), cr => cr.getOrElse(throw exception))
  }

  protected def getContentRepositoryOpt(id: Int): Option[ContentRepository] = Try(contentRepositoryDao.getById(id))
    .fold(e => throw ContentRepoRetrievalFailure.initCause(e), cr => cr)

  protected def createRepo(contentRepo: ContentRepository): ContentRepository = {
    userTransaction.inSingleTransaction {
      val insertionResult: Either[Throwable, ContentRepository] =
        Try(contentRepositoryDao.insertIfNotExisting(
          contentRepo, _.name === contentRepo.name).fold(_ => throw ContentRepoNameExisting, newRepo => newRepo))
          .fold(e => {
            throw e
            Left(e)
          },
            insertedRepo => Right(insertedRepo)
          )

      if (insertionResult.isLeft) {
        val left = insertionResult.left.get
        logAndReturn[Throwable](left, logger.error, dataBaseOperationError(left, ACTION_DATABASE_INSERTION_OF_ENTRY, ASPECT_REPOSITORY_CONFIG, contentRepo.name))
        throw left
      } else {
        val repo = insertionResult.right.get
        logAndReturn[ContentRepository](repo, logger.trace,
          contentActionTraceEnd[ContentRepository](createContentRepositoryMetaContent, repo, ASPECT_REPOSITORY, ACTION_INSERT))
        clearFileSystemProvider.createFolder(repo.name)
        repo
      }
    }
  }.get

  protected def updateRepo(newRepo: ContentRepository): ContentRepository = {
    contentRepositoryDao.update(newRepo)
  }

  protected def getRepo(json: JValue)(implicit userSession: UserSession): Option[ContentRepository] = {
    val repoData = json.extract[ContentRepoGetRequest]
    Try(getContentRepositoryOpt(repoData.id)).fold(e => throw e,
      res => res.flatMap { cr =>
        logAndReturn[Option[ContentRepository]](res, logger.trace, contentActionTraceEnd[ContentRepository]
          (createContentRepositoryMetaContent, cr, ASPECT_REPOSITORY_BASE, ACTION_READ))
      })
  }

  protected[controller] def listContentRepositories(providerFilter: Option[String])
                                                   (implicit userSession: UserSession,
                                                    requestSource: RequestSource,
                                                    ec: ExecutionContext): Future[Seq[ContentRepository]] = {

    val repos = providerFilter match {
      case Some(provider) => Try(contentRepositoryDao.getAllByProvider(provider))
      case None => Try(contentRepositoryDao.getAll())
    }

    permissionHandler.checkPermissions().map { permissionObjects =>
      val res = repos.get
      res.filter(permissionObjects.hasPermissionObjectId).map { repo =>
        logAndReturn[ContentRepository](repo, logger.trace, contentActionTraceEnd[ContentRepository]
          (createContentRepositoryMetaContent, repo, ASPECT_REPOSITORY_BASE, ACTION_READ))
      }
    }
  }

  protected def mapToFindResponse(repo: ContentRepository): ContentRepoFindResponseItemTrait

  protected def hasFolders(id: Int): Boolean
}
