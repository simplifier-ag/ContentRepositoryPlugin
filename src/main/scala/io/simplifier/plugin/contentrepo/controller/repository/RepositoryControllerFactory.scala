package io.simplifier.plugin.contentrepo.controller.repository

import io.simplifier.plugin.contentrepo.pluginBaseRelated.json.JSONCompatibility.LegacySearch
import akka.stream.Materializer
import io.simplifier.plugin.contentrepo.dao.ContentRepositoryDao
import io.simplifier.plugin.contentrepo.definitions.exceptions.ContentRepositoryExceptions._
import io.simplifier.plugin.contentrepo.model.provider.{ClearFileSystemProvider, FileSystemProvider}
import io.simplifier.plugin.contentrepo.permission.PermissionHandler
import com.typesafe.config.Config
import org.json4s._

import scala.concurrent.{ExecutionContext, Future}

/**
  * Factory to discern which repository controller is needed
  */
class RepositoryControllerFactory(
                                  config: Config,
                                  permissionHandler: PermissionHandler,
                                  contentRepositoryDao: ContentRepositoryDao = new ContentRepositoryDao)
                                 (implicit materializer: Materializer)
{

  /**
    * Returns the repository controller belonging to the given provider
    *
    * @param provider the provider name
    * @return the specific repository controller
    */
  def getRepositoryControllerByProviderName(provider: String): ContentRepositoryController = {
    provider match {
      case FileSystemProvider.providerId =>
        new FileSystemRepositoryController(
          new ClearFileSystemProvider(config),
          permissionHandler)
      case ClearFileSystemProvider.providerId =>
        new ClearFileSystemRepositoryController(
          new ClearFileSystemProvider(config),
          permissionHandler)
      case _ => throw ContentRepoUnknownProvider
    }
  }

  /**
    * Returns the repository controller belonging to the provider passed as parameter "provider" in the json
    *
    * @param json the json containing the provider
    * @return the specific repository controller
    */
  def getRepositoryControllerByProvider(json: JValue)(implicit ec: ExecutionContext): Future[ContentRepositoryController] = Future {
    LegacySearch(json) \ "provider" match {
      case JString(providerName) =>
        getRepositoryControllerByProviderName(providerName)
      case JNothing =>
        new FileSystemRepositoryController(
          new ClearFileSystemProvider(config),
          permissionHandler)
      case _ => throw ContentRepoWrongDataType("provider", "String")
    }
  }

  /**
    * Returns the repository controller belonging to the provider from the repository identified by the repo id
    * @param json the json containing the id
    * @return the specific repository controller
    */
  def getRepositoryControllerById(json: JValue)(implicit ec: ExecutionContext): Future[ContentRepositoryController] = Future {
    LegacySearch(json) \ "id" match {
      case JInt(providerId) =>
        val repo = contentRepositoryDao.getById(providerId.toInt).getOrElse(throw ContentRepoNotFound)
        getRepositoryControllerByProviderName(repo.provider)
      case JNothing =>
        new FileSystemRepositoryController(
          new ClearFileSystemProvider(config),
          permissionHandler)
      case _ => throw ContentRepoWrongDataType("id", "Integer")
    }
  }

  /**
    * Returns the repository controller belonging to the provider from the repository identified by the repo name
    * @param json the json containing the name of the repo
    * @return the specific repository controller
    */
  def getRepositoryControllerByName(json: JValue)(implicit ec: ExecutionContext): Future[ContentRepositoryController] = Future {
    LegacySearch(json) \ "name" match {
      case JString(name) =>
        val repo = contentRepositoryDao.getByName(name).get.getOrElse(throw ContentRepoNotFoundByName(name))
        getRepositoryControllerByProviderName(repo.provider)
      case JNothing =>
        new FileSystemRepositoryController(
          new ClearFileSystemProvider(config),
          permissionHandler)
      case _ => throw ContentRepoWrongDataType("name", "String")
    }
  }
}