package io.simplifier.plugin.contentrepo.controller.repository

import io.simplifier.plugin.contentrepo.dao.ContentRepositoryDao
import io.simplifier.plugin.contentrepo.definitions.caseClasses.ContentRepositoryCaseClasses._
import io.simplifier.plugin.contentrepo.definitions.exceptions.ContentRepositoryExceptions._
import io.simplifier.plugin.contentrepo.dto.RestMessages.getRepositorySuccess
import io.simplifier.plugin.contentrepo.model.ContentRepository
import io.simplifier.plugin.contentrepo.model.provider.ClearFileSystemProvider
import io.simplifier.plugin.contentrepo.permission.PermissionHandler
/**
  * Implementation of the ContentRepositoryController for the clear file system provider
  */
class ClearFileSystemRepositoryController(provider: ClearFileSystemProvider,
                                          permissionHandler: PermissionHandler,
                                          contentRepositoryDao: ContentRepositoryDao = new ContentRepositoryDao)
  extends ContentRepositoryController(provider, permissionHandler) {

  val PROVIDER_NAME: String = "ClearFileSystem"

  /**
    * Returns all information about the repository.
    *
    * @param repo the content repository
    * @return The id, name, description, permissionObjectType and -Id and provider of the repository
    */
  override def mapToGetResponse(repo: ContentRepository): ClearContentRepoGetResponse = {
    ClearContentRepoGetResponse(repo.id, repo.name, repo.description.getOrElse(""), repo.permissionObjectType, repo.permissionObjectID, repo.provider, getRepositorySuccess)
  }

  /**
    * Checks if the repository contains folders
    *
    * @param id the id of the repository
    * @return whether the repository contains folders
    */
  override def hasFolders(id: Int): Boolean = {
    val repo = contentRepositoryDao.getById(id).getOrElse(throw ContentRepoNotFound)
    provider.hasChildren(repo.name)
  }

  /**
    * Maps the repository to the find response item
    *
    * @param repo the repo
    * @return The mapped response item
    */
  override def mapToFindResponse(repo: ContentRepository): ContentRepoFindResponseItemTrait =
    ClearContentRepoFindResponseItem(repo.id, repo.name, repo.description.getOrElse(""), repo.permissionObjectType, repo.permissionObjectID, repo.provider)
}