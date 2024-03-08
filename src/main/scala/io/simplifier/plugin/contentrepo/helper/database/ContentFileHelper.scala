package io.simplifier.plugin.contentrepo.helper.database

import io.simplifier.plugin.contentrepo.controller.BaseController.ContentRepoPermissions
 import io.simplifier.plugin.contentrepo.dao.ContentFileDao
 import io.simplifier.plugin.contentrepo.definitions.Constants._
 import io.simplifier.plugin.contentrepo.definitions.LogMessages._
 import io.simplifier.plugin.contentrepo.definitions.exceptions.ContentFileExceptions.{ContentFileExternalStorageUpdateFailure, ContentFileRetrievalFailure, ContentFileUpdateFailure}
 import io.simplifier.plugin.contentrepo.model.ContentFile
 import io.simplifier.plugin.contentrepo.model.provider.ContentFileIO
 import io.simplifier.pluginbase.util.logging.Logging

 import scala.util.{Failure, Try}

class ContentFileHelper(contentFileDao: ContentFileDao = new ContentFileDao) extends Logging {

  private[contentrepo] def insertDatabaseEntry(file: ContentFile): Try[ContentFile] = {
    logger.trace(contentActionTraceBegin[ContentFile](createContentFileMetaContent, file, ASPECT_FILE, ACTION_DATABASE_INSERTION_OF_ENTRY))
    Try(contentFileDao.insert(file))
      .transform(res => Try(logAndReturn[ContentFile](res, logger.trace, contentActionTraceEnd[ContentFile](createContentFileMetaContent, res, ASPECT_FILE, ACTION_INSERT))),
        e => Failure(ContentFileUpdateFailure.initCause(e)))
  }

  private[contentrepo] def deleteDatabaseEntry(file: ContentFile): Try[ContentFile] = {
    logger.trace(contentActionTraceBegin[ContentFile](createContentFileMetaContent, file, ASPECT_FILE, ACTION_DATABASE_DELETION_OF_ENTRY))
    Try(contentFileDao.delete(file))
      .transform(_ => Try(logAndReturn[ContentFile](file, logger.trace,
        contentActionTraceEnd[ContentFile](createContentFileMetaContent, file, ASPECT_FILE, ACTION_DELETE))),
        e => Failure(ContentFileUpdateFailure.initCause(e)))
  }

  private[contentrepo] def getDatabaseEntry(fileId: Int): Try[Option[ContentFile]] = {
    logger.trace(dataBaseOperationStart(fileId, ASPECT_FILE, ACTION_READ, ASPECT_FILE))
    Try(contentFileDao.getById(fileId))
      .transform(res => Try(logAndReturn[Option[ContentFile]](res, logger.trace,
        res.map(contentActionTraceEnd[ContentFile](createContentFileMetaContent, _, ASPECT_FILE, ACTION_READ)).getOrElse(fileIsNotInDatabase(fileId)))),
        e => Failure(ContentFileUpdateFailure.initCause(e)))
  }

  //REMOVE PERMISSION FILTERING
  private[contentrepo] def getDatabaseEntry(folderId: Int, fileName: String, permissions: ContentRepoPermissions): Try[Option[ContentFile]] = {
    logger.trace(dataBaseOperationStart(folderId, ASPECT_FOLDER, fileName, ASPECT_FILE, ACTION_READ, ASPECT_FILE))
    contentFileDao.getByFolderAndName(folderId, fileName)
      .transform(res => Try(logAndReturn[Option[ContentFile]](res filter permissions.hasPermissionObjectId, logger.trace,
        res.map(contentActionTraceEnd[ContentFile](createContentFileMetaContent, _, ASPECT_FILE, ACTION_READ)).getOrElse(fileIsNotInDatabase(folderId, fileName)))),
        e => throw ContentFileRetrievalFailure().initCause(e))
  }

  //REMOVE PERMISSION FILTERING
  private[contentrepo] def listDatabaseEntries(folderId: Int, permissions: ContentRepoPermissions): Try[Seq[ContentFile]] = {
    logger.trace(dataBaseOperationStart(folderId, ASPECT_FOLDER, ACTION_READ, ASPECT_FILE))
    contentFileDao.getAllByFolder(folderId)
      .transform(files => Try {
        val allowedElements: Seq[ContentFile] = files.filter(permissions.hasPermissionObjectId)
        allowedElements
          .zipWithIndex
          .map { case (file, index) => logAndReturn[ContentFile](file, logger.trace,
            contentActionTraceEnd[ContentFile](createContentFileMetaContent, file, ASPECT_FILE, s"$ACTION_LISTED ($index of ${allowedElements.size})"))
          }
      }, e => throw ContentFileRetrievalFailure(list = true).initCause(e))
  }

  private[contentrepo] def updateExternalStorageFieldFileDefinition(file: ContentFile, fileIO: ContentFileIO): Try[Unit] =
    Try {
      if (file.externalStorageFileId != fileIO.externalStorageFileId) {
        logger.trace(contentActionTraceBegin[ContentFile](createContentFileMetaContent, file, ASPECT_FILE, ACTION_DATABASE_UPDATE_OF_EXTERNAL_STORAGE))
        file.externalStorageFileId = fileIO.externalStorageFileId
        Try(contentFileDao.update(file))
      }.fold(e => throw ContentFileExternalStorageUpdateFailure.initCause(e),
        res => logAndReturn[ContentFile](res, logger.trace,
          contentActionTraceEnd[ContentFile](createContentFileMetaContent, res, ASPECT_FILE, ACTION_DATABASE_UPDATE_OF_EXTERNAL_STORAGE)))
    }
}
