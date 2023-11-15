package io.simplifier.plugin.contentrepo.pluginBaseRelated.fileIo

import akka.http.scaladsl.server.Route
import akka.stream.IOResult
import akka.stream.scaladsl.Source
import akka.util.ByteString
import io.simplifier.plugin.contentrepo.pluginBaseRelated.ads.ZipStructure
import io.simplifier.plugin.contentrepo.pluginBaseRelated.fileIo.FileSystemAccess.{AkkaFileSource, AkkaFileSourceWithSize, FSListItem, FileMap}

import java.io.{File, InputStream, InputStreamReader, Reader}
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets.UTF_8
import java.nio.file.Path
import java.util.stream
import scala.concurrent.Future
import scala.util.{Success, Try}

/**
 * Abstraction for all file-based IO-Operations.
 * Paths are denoted absolute (if given from the outside, e.g. config), or relative to the configured storage or temp path.
 *
 * The default implementation [[DefaultFileSystemAccess]] is Java-NIO based and allows configured storage/temp path.
 * A Unit Test implementation [[VirtualFileSystemAccess]] will run on a memory-only file system and does therefore not
 * leak data to the outside of the unit test.
 * If only the absolute file system is required (e.g. for loading resources configured with absolute path), the
 * implementation [[BasicFileSystemAccess]] can be used, which doesn't need a configuration to work.
 */
trait FileSystemAccess {

  /**
   * Resolve a [[StoragePath]] from segments given as VARARGS with at least one entry
   * @param segment first segment
   * @param segments optional other segments as VARARGS
   * @return resolved Storage Path
   */
  def resolveStorage(segment: String, segments: String*): StoragePath = resolveStorage(segment +: segments)

  /**
   * Resolve a [[StoragePath]] from segments given as Sequence.
   * The segments can also be empty, which will yield the storage root path.
   * @param segments segments relative to the storage root
   * @return resolved Storage Path
   */
  def resolveStorage(segments: Seq[String]): StoragePath = StoragePath(segments)

  /**
   * Resolve a [[TempPath]] from segments given as VARARGS with at least one entry
   * @param segment first segment
   * @param segments optional other segments as VARARGS
   * @return resolved Temp Path
   */
  def resolveTemp(segment: String, segments: String*): TempPath = resolveTemp(segment +: segments)

  /**
   * Resolve a [[TempPath]] from segments given as Sequence.
   * The segments can also be empty, which will yield the temp root path.
   * @param segments segments relative to the temp root
   * @return resolved Temp Path
   */
  def resolveTemp(segments: Seq[String]): TempPath = TempPath(segments)

  /**
   * Resolve a [[AbsolutePath]] from segments given as VARARGS with at least one entry
   * @param segment first segment
   * @param segments optional other segments as VARARGS
   * @return resolved absolute Path
   */
  def resolveAbsolute(segment: String, segments: String*): AbsolutePath = resolveAbsolute(segment +: segments)

  /**
   * Resolve a [[AbsolutePath]] from segments given as Sequence.
   * The segments can also be empty, which will yield the default root path of the underlying file system.
   * @param segments absolute segments
   * @return resolved absolute Path
   */
  def resolveAbsolute(segments: Seq[String]): AbsolutePath = AbsolutePath(segments)

  /**
   * Normalize a logic path by collapsing all segments into a single segment, and also by normalizing redundant
   * path fragments (like "./" or "subdir/../") inside this segment.
   * The type of the path (Absolute, Storage, Temp) is retained in the result,
   * so e.g. a StoragePath as input with always yield a StoragePath as a result.
   * @param path path to normalize
   * @return formalized path
   */
  def normalize(path: LogicPath): LogicPath

  /**
   * Try to resolve a logic path to a raw java.io File object.
   * This is intended for use with external libraries which require a File object to operate on; all internal modules
   * should work with the LogicPath and FileSystemAccess abstraction and not call Java's IO API directly.
   * @param path path to resolve to a file
   * @return Some file if possible, None otherwise
   */
  def resolveToRawFile(path: LogicPath): Option[File]

  /**
   * Check if one path contains another path, i.e. the child path "starts with" the parent path.
   * In case both paths are equal, this check will always yield true.
   * @param parent the parent path
   * @param child the child path
   * @return true if the child path is contained in the parent path
   */
  def isContained(parent: LogicPath, child: LogicPath): Boolean

  /**
   * Check if a child path is contained in a parent path, but is also a real sub-path (meaning that it is not equal to the parent path).
   * @param parent the parent path
   * @param child the child path
   * @return true if the parent path contains the child path, but is not equal to it
   */
  def isContainedNotEqual(parent: LogicPath, child: LogicPath): Boolean = {
    isContained(parent, child) && !isContained(child, parent)
  }

  /**
   * Get the file size of a path denoting a file.
   * @param path path to get the size from
   * @return Success containing the file size in bytes, or a Failure containing errors, e.g. when the file does not exist or is not a file
   */
  def fileSize(path: LogicPath): Try[Long]

  /**
   * Read all bytes from a file.
   * @param path path to read
   * @return Success containing the bytes, or Failure when an error occurs, e.g. when the file does not exist or read access is denied
   */
  def readBytes(path: LogicPath): Try[Array[Byte]]

  def readByteStream(path: LogicPath): Try[InputStream]

  /**
   * Read a file as String.
   * @param path path to read
   * @param charset character set to read, by default UTF_8
   * @return Success containing the read String, or Failure when an error occurs, e.g. when the file does not exist or read access is denied
   */
  def readString(path: LogicPath, charset: Charset = UTF_8): Try[String]

  /**
   * Read the lines of a text file as String sequence.
   * @param path path to read
   * @param charset character set to read, by default UTF_8
   * @return Success containing the read lines, or Failure when an error occurs, e.g. when the file does not exist or read access is denied
   */
  def readLines(path: LogicPath, charset: Charset = UTF_8): Try[Seq[String]]

  /**
   * Open the file for reading as Akka stream source.
   * @param path path to read
   * @return Akka stream source, for use in streaming
   */
  def readStream(path: LogicPath): AkkaFileSource

  /**
   * Open the file for reading as Akka stream source, and also return its file size, so it can be streamed into known-length Akka Http entities.
   * @param path path to read
   * @return Success with Akka stream source and length, or Failure when the reading of the file size failed
   */
  def readStreamWithSize(path: LogicPath): Try[AkkaFileSourceWithSize] = {
    fileSize(path) map { size => AkkaFileSourceWithSize(readStream(path), size) }
  }

  /**
   * Create a directory and also create all its parent directories which are missing (equal to the unix "mkdirs" command).
   * Of the directory already exists, nothing happens.
   * @param path path to create as a directory
   * @return Success with Unit, or a Failure containing errors, e.g. when the directory could not be created due to missing write access.
   */
  def mkDirs(path: LogicPath): Try[Unit]

  def ensureParentExists(path: LogicPath): Try[Unit]

  def withExistingParent[A](path: LogicPath)(fun: => A): Try[A] = {
    ensureParentExists(path) map { _ => fun }
  }

  def withExistingParentFlat[A](path: LogicPath)(fun: => Try[A]): Try[A] = {
    ensureParentExists(path) flatMap { _ => fun }
  }

  def writeBytes(path: LogicPath, bytes: Array[Byte]): Try[Unit]

  def writeString(path: LogicPath, str: String, charset: Charset = UTF_8): Try[Unit]

  def appendBytes(path: LogicPath, bytes: Array[Byte]): Try[Unit]

  def withInputStream[A](path: LogicPath)(consumer: InputStream => A): Try[A]

  def withReader[A](path: LogicPath)(consumer: Reader => A): Try[A] = withInputStream(path) {
    is =>
      val reader = new InputStreamReader(is)
      try {
        consumer(reader)
      } finally {
        try { reader.close() } catch { case _: Throwable => }
      }
  }

  def list(path: LogicPath): stream.Stream[Path]

  def listFilesAndFolders(path: LogicPath): Vector[Path]

  def exists(path: LogicPath): Boolean

  def getNioPath(path: LogicPath): Path

  def isDirectory(path: LogicPath): Boolean

  def isFile(path: LogicPath): Boolean

  def delete(path: LogicPath): Try[Unit]

  def deleteIfExists(path: LogicPath): Try[Unit] = if(exists(path)) delete(path) else Success(())

  def deleteDirectoryRecursive(path: LogicPath): Try[Unit]

  def createTempDir(parentDir: TempPath = TempPath(Seq.empty), prefix: Option[String] = None): Try[TempPath]

  def writeFiles(path: LogicPath, files: FileMap): Try[Unit]

  def findFiles(path: LogicPath, fileName: String): Try[FileMap]

  def readFiles(path: LogicPath): Try[FileMap]

  def deflate(path: LogicPath, zip: ZipStructure): Try[Unit] = writeFiles(path, zip.files)

  def compress(path: LogicPath): Try[ZipStructure] = readFiles(path) map ZipStructure.apply

  def copyFile(srcFile: LogicPath, destFile: LogicPath, forceOverwrite: Boolean = true): Try[Unit]

  def copyFileToDirectory(srcFile: LogicPath, destDirectory: LogicPath): Try[LogicPath]

  def copyDirectoryToDirectory(srcDirectory: LogicPath, destDirectory: LogicPath): Try[Unit]

  def moveFile(srcFile: LogicPath, destFile: LogicPath): Try[Unit]

  def moveDirectoryToDirectory(srcDirectory: LogicPath, destDirectory: LogicPath): Try[Unit]

  def httpDirectoryRoute(path: LogicPath): Route

  def httpFileRoute(path: LogicPath): Route

  def listDirectoryRecursive(path: LogicPath): Try[Seq[FSListItem]]

}

object FileSystemAccess {

  type AkkaFileSource = Source[ByteString, Future[IOResult]]

  case class AkkaFileSourceWithSize(source: AkkaFileSource, size: Long)

  type FileMap = Map[String, Array[Byte]]

  case class FSListItem(basePath: LogicPath, parent: LogicPath, name: String, relativePath: String,
                        itemType: FSListItemType.ItemType) {

    def path: LogicPath = basePath resolve relativePath

  }

  object DeniedOperation extends Enumeration {
    type DeniedOperation = Value
    val WRITE: DeniedOperation = Value("Write")
    val READ: DeniedOperation = Value("Read")
  }

  object FSListItemType extends Enumeration {
    type ItemType = Value
    val FILE: ItemType = Value("file")
    val DIRECTORY: ItemType = Value("directory")
  }

}