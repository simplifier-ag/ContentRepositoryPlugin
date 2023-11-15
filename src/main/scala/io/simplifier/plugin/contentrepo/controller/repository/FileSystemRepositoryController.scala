package io.simplifier.plugin.contentrepo.controller.repository

import io.simplifier.plugin.contentrepo.dao.ContentFolderDao
import io.simplifier.plugin.contentrepo.definitions.caseClasses.ContentRepositoryCaseClasses._
import io.simplifier.plugin.contentrepo.definitions.exceptions.ContentFolderExceptions.ContentFolderRetrievalFailure
import io.simplifier.plugin.contentrepo.dto.RestMessages.getRepositorySuccess
import io.simplifier.plugin.contentrepo.model.ContentRepository
import io.simplifier.plugin.contentrepo.model.provider.ClearFileSystemProvider
import io.simplifier.plugin.contentrepo.permission.PermissionHandler

/**
  * Implementation of the ContentRepositoryController for the file system provider
  */
class FileSystemRepositoryController(clearFileSystemProvider: ClearFileSystemProvider,
                                     permissionHandler: PermissionHandler,
                                     contentFolderDao: ContentFolderDao = new ContentFolderDao
                                    ) extends ContentRepositoryController(clearFileSystemProvider, permissionHandler) {

  val PROVIDER_NAME = "FileSystem"

  /**
    * Returns all information about the repository.
    * Contains a fix for the deprecated config return value
    *
    * @param repo the content repository
    * @return the id, name, description, permissionObjectType and -Id, provider and deprecated config
    */
  override def mapToGetResponse(repo: ContentRepository): ContentRepoGetResponse = {
    //Deprecated Config fix
    val config = Map("basedir" -> repo.name)
    ContentRepoGetResponse(repo.id, repo.name, repo.description.getOrElse(""), repo.permissionObjectType, repo.permissionObjectID, repo.provider, config, getRepositorySuccess)
  }

  /**
    * Maps the repo to the find repo response item
    *
    * @param repo the repo to be mapped
    * @return the mapped find response
    */
  override def mapToFindResponse(repo: ContentRepository): ContentRepoFindResponseItemTrait = {
    //Deprecated Config fix
    val config = Map("basedir" -> repo.name)
    ContentRepoFindResponseItem(repo.id, repo.name, repo.description.getOrElse(""), repo.permissionObjectType, repo.permissionObjectID, repo.provider, config)
  }

  /**
    * Checks if the repository with the given id has folders
    *
    * @param id the repository id
    * @return whether the repository has folders or not
    */
  override def hasFolders(id: Int): Boolean = contentFolderDao.getAllByRepository(id).fold(e =>
    throw ContentFolderRetrievalFailure(true).initCause(e), res => res).nonEmpty
}