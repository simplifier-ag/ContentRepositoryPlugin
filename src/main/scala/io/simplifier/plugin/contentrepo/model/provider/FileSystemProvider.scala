package io.simplifier.plugin.contentrepo.model.provider

import akka.stream.Materializer
import akka.stream.scaladsl.FileIO
import io.simplifier.plugin.contentrepo.definitions.Constants._
import io.simplifier.plugin.contentrepo.definitions.LogMessages._
import io.simplifier.plugin.contentrepo.definitions.exceptions.ContentFileExceptions.ContentFileIO
import io.simplifier.plugin.contentrepo.contentRepoIo.{FileStreamSource, StreamSource}
import io.simplifier.plugin.contentrepo.model.{ContentFile, ContentFolder}
import com.typesafe.config.Config
import io.simplifier.pluginbase.util.io.StreamUtils.{ByteSource, foldToByteArray}
import io.simplifier.pluginbase.util.logging.Logging

import java.io.File
import java.nio.file.StandardOpenOption.{CREATE_NEW, TRUNCATE_EXISTING, WRITE}
import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}
import scala.util.control.NonFatal
import scala.util.{Failure, Success, Try}

/**
  * ContentProvider: Local File System.
  *
  * @author Christian Simon
  */
class FileSystemProvider(config: Config) extends ContentProvider {

  import FileSystemProvider._

  lazy val baseFolder: String = config.getString("fileSystemRepository.baseDirectory")
  lazy val timeoutDuration: Duration = Duration(config.getInt("plugin.timeoutSeconds"), SECONDS)

  override val id: String = providerId

  private[this] def resolveBaseDirectory(repoName: String): Try[File] = Try {
    new File(baseFolder, repoName)
  }

  def resolveFileSystem(file: ContentFile, folder: ContentFolder, repoName: String)
                       (implicit ec: ExecutionContext, materializer: Materializer): Try[FileSystemIO] = Try {
    val newFile: File = resolveFile(file, folder, repoName)
    new FileSystemIO(newFile)
  }

  def resolveFile(file: ContentFile, folder: ContentFolder, repoName: String): File = {
    val baseDir: File = resolveBaseDirectory(repoName).fold(throw _, res => res)
    val folderDir: File = new File(baseDir, folder.id.toString)
    new File(folderDir, s"${file.id}.dat")
  }

  def getBaseDirectory(repoName: String)
                      (implicit ec: ExecutionContext, materializer: Materializer): Try[FileSystemIO] =
    Try(new FileSystemIO(resolveBaseDirectory(repoName).get))

}

object FileSystemProvider {
  val providerId = "FileSystem"
}

/**
  * ContentFileIO implementation with files.
  *
  * @author Christian Simon
  */
class FileSystemIO(file: File)(implicit ec: ExecutionContext, materializer: Materializer) extends ContentFileIO with Logging {

  val parent: File = file.getParentFile

  val externalStorageFileId: Option[String] = None

  override def create(dataSource: ByteSource, contentFile: ContentFile): Future[ContentFile] = {
    if (!parent.isDirectory) {
      parent.mkdirs()
    }

    logger.trace(contentActionTraceBegin[ContentFile](createContentFileMetaContent, contentFile, ASPECT_FILE, ACTION_FILE_CREATION_OF_FILE))
    val fileSink = FileIO.toPath(file.toPath, options = Set(WRITE, CREATE_NEW))
    dataSource.runWith(fileSink).map {
      fileResult =>
        fileResult.status match {
          case Failure(e) =>
            logger.error(s"Error creating file $file", e)
            throw e
          case Success(_) =>
            logger.trace(s"Created file $file with ${fileResult.count} bytes")
            logAndReturn[ContentFile](contentFile, logger.trace, contentActionTraceEnd[ContentFile](createContentFileMetaContent, contentFile, ASPECT_FILE, ACTION_FILE_CREATED))
        }
    } recoverWith {
      case NonFatal(e) => Future.failed(ContentFileIO.initCause(e))
    }
  }

  override def delete(contentFile: ContentFile): Try[ContentFile] = {

    logger.trace(contentActionTraceBegin[ContentFile](createContentFileMetaContent, contentFile, ASPECT_FILE, ACTION_FILE_DELETION_OF_FILE))
    Try {
      file.delete()
      if (parent.listFiles().isEmpty) {
        parent.delete()
      }
    }.fold(e => throw ContentFileIO.initCause(e),
      _ => logAndReturn[Try[ContentFile]](Try(contentFile), logger.trace,
        contentActionTraceEnd[ContentFile](createContentFileMetaContent, contentFile, ASPECT_FILE, ACTION_FILE_DELETED)))
  }

  override def overwrite(dataSource: ByteSource, contentFile: ContentFile): Future[ContentFile] = {

    logger.trace(contentActionTraceBegin[ContentFile](createContentFileMetaContent, contentFile, ASPECT_FILE, ACTION_FILE_OVERWRITING_OF_ENTRY))
    val fileSink = FileIO.toPath(file.toPath, options = Set(WRITE, TRUNCATE_EXISTING))
    dataSource.runWith(fileSink)
      .map {
        fileResult =>
          fileResult.status match {
            case Failure(e) =>
              logger.error(s"Error overwriting file $file", e)
              throw e
            case Success(_) =>
              logger.trace(s"Overwritten file $file with ${fileResult.count} bytes")
              logAndReturn[ContentFile](contentFile, logger.trace,
                contentActionTraceEnd[ContentFile](createContentFileMetaContent, contentFile, ASPECT_FILE, ACTION_FILE_OVERWRITTEN))
          }
      }
      .recoverWith {
        case NonFatal(e) =>
          throw ContentFileIO.initCause(e)
      }
  }

  override def read(contentFile: ContentFile): Try[StreamSource] = {
    logger.trace(contentActionTraceBegin[ContentFile](createContentFileMetaContent, contentFile, ASPECT_FILE, ACTION_FILE_READING_OF_FILE))
    Try(new FileStreamSource(file.toPath))
      .fold(e => throw ContentFileIO.initCause(e),
        ss => logAndReturn[Try[StreamSource]](Try(ss), logger.trace, contentActionTraceEnd[ContentFile](createContentFileMetaContent, contentFile, ASPECT_FILE, ACTION_FILE_READ)))
  }

  override def readStreamSource(streamSource: StreamSource, contentFile: ContentFile): Future[Array[Byte]] = {
    logger.trace(contentActionTraceBegin[ContentFile](createContentFileMetaContent, contentFile, ASPECT_FILE, ACTION_FILE_READING_OF_STREAM))
    foldToByteArray(streamSource.stream().get)
      .recover { case NonFatal(e) => throw ContentFileIO.initCause(e) }
      .map { bs =>
        logAndReturn[Array[Byte]](bs, logger.trace, contentActionTraceEnd[ContentFile](createContentFileMetaContent, contentFile, ASPECT_FILE, ACTION_FILE_READ_STREAM))
      }

  }
}