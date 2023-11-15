package io.simplifier.plugin.contentrepo.helper.fileSystem

import akka.stream.Materializer
import io.simplifier.pluginbase.util.logging.Logging
import io.simplifier.plugin.contentrepo.dao.ContentRepositoryDao
import io.simplifier.plugin.contentrepo.definitions.Constants._
import io.simplifier.plugin.contentrepo.definitions.LogMessages._
import io.simplifier.plugin.contentrepo.definitions.exceptions.ContentFileExceptions._
import io.simplifier.plugin.contentrepo.model.provider.{ContentFileIO, FileSystemProvider}
import io.simplifier.plugin.contentrepo.model.{ContentFile, ContentFolder, ContentRepository}

import scala.concurrent.ExecutionContext
import scala.concurrent.duration.Duration
import scala.util.{Failure, Try}

class ContentFileHelper(duration: Duration,
                        contentRepoDao: ContentRepositoryDao,
                        provider: FileSystemProvider)
                       (implicit mat: Materializer) extends Logging {

  implicit val executionContext: ExecutionContext = mat.executionContext

  private[contentrepo] def loadFileIO(file: ContentFile, folder: ContentFolder): Try[ContentFileIO] = {
    logger.trace(contentActionTraceBegin[ContentFile](createContentFileMetaContent, file, ASPECT_FILE, ACTION_FILE_LOADING_OF_FILE_IO))
    Try {
      val repo: ContentRepository = Try(contentRepoDao.getById(folder.contentId))
        .fold(e => throw ContentFileRetrievalFailure().initCause(e), res => res)
        .getOrElse(throw ContentFileRepoNotFound)
      provider.resolveFileSystem(file, folder, repo.name)
    }.transform(res => res
      .fold(e => throw e, res => Try(logAndReturn[ContentFileIO](res, logger.trace, contentFileAction(file,
        contentActionTraceEnd[ContentFile](createContentFileMetaContent, file, ASPECT_FILE, ACTION_FILE_LOADED_IO))))),
      e => Failure(ContentFileIOLoadFailure.initCause(e)))
  }
}