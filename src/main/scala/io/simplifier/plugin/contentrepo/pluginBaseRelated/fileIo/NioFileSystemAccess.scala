package io.simplifier.plugin.contentrepo.pluginBaseRelated.fileIo

import akka.http.scaladsl.server.{Directives, Route}
import akka.stream.IOResult
import akka.stream.scaladsl.{FileIO, Source}
import akka.util.ByteString
import io.simplifier.plugin.contentrepo.pluginBaseRelated.fileIo.FileSystemAccess.{FSListItem, FSListItemType, FileMap}

import java.io.{File, IOException, InputStream}
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets.UTF_8
import java.nio.file._
import java.nio.file.attribute.BasicFileAttributes
import java.util.stream
import scala.collection.JavaConverters._
import scala.concurrent.Future
import scala.language.implicitConversions
import scala.util.Try

/**
  * Implementation of FileSystemAccess, relying on java NIO library to perform file access.
  */
abstract class NioFileSystemAccess extends FileSystemAccess {


  /**
    * Resolve logic path definition into java NIO path.
    * @param logicPath logic path to convert
    * @return resolved NIO path to use in FileSystemAccess operations
    * @throws IOException if the path is not valid or supported in this file system
    */
  @throws[IOException]
  implicit protected[fileIo] def resolveRealPath(logicPath: LogicPath): Path

  /**
    * Enforce absolute path from segments. If the given segments do not denote an absolute path by themselves,
    * resolve them relative to the default root of the given file system.
    * @param fs       underlying NIO file system implementation to use
    * @param segments segments of the path
    * @return nio path object with absolute
    */
  protected def enforceAbsolutePath(fs: FileSystem)(segments: Seq[String]): Path = {
    if (segments.isEmpty) {
      fileSystemRoot(fs)
    } else {
      val path = fs.getPath(segments.head, segments.tail: _*)
      if (path.isAbsolute) {
        path
      } else {
        fileSystemRoot(fs).resolve(path)
      }
    }
  }

  /** Root path, always "/" in Unix, and the first drive in Windows, like "C:\" */
  private def fileSystemRoot(fs: FileSystem) = fs.getRootDirectories.asScala.head

  override def resolveToRawFile(path: LogicPath): Option[File] = Some(path.toFile)

  override def normalize(path: LogicPath): LogicPath = path match {
    case abs: AbsolutePath => AbsolutePath(Seq(abs.normalize().toString))
    case storage: StoragePath => StoragePath(Seq(relativizeToString(resolveStorage(Seq.empty), storage)))
    case temp: TempPath => TempPath(Seq(relativizeToString(resolveTemp(Seq.empty), temp)))
  }

  override def isContained(parent: LogicPath, child: LogicPath): Boolean = {
    child.startsWith(parent)
  }

  override def fileSize(path: LogicPath): Try[Long] = Try {
    Files.size(path)
  }

  override def readBytes(path: LogicPath): Try[Array[Byte]] = Try {
    Files.readAllBytes(path)
  }

  override def readByteStream(path: LogicPath): Try[InputStream] = Try {
    Files.newInputStream(path)
  }

  override def readString(path: LogicPath, charset: Charset = UTF_8): Try[String] = readBytes(path) map {
    bytes => new String(bytes, charset)
  }

  override def readLines(path: LogicPath, charset: Charset = UTF_8): Try[Seq[String]] = Try {
    Seq(Files.readAllLines(path, charset).asScala: _*)
  }

  override def readStream(path: LogicPath): Source[ByteString, Future[IOResult]] = FileIO.fromPath(path)

  override def mkDirs(path: LogicPath): Try[Unit] = Try {
    Files.createDirectories(path)
  }

  override def ensureParentExists(path: LogicPath): Try[Unit] = Try {
    val parent: Option[Path] = Option(path.getParent) // If file is root, it has no parent
    parent.foreach(Files.createDirectories(_))
  }

  override def writeBytes(path: LogicPath, bytes: Array[Byte]): Try[Unit] = {
    withExistingParent(path) {
      Files.write(path, bytes)
    }
  }

  override def writeString(path: LogicPath, str: String, charset: Charset = UTF_8): Try[Unit] = {
    writeBytes(path, str.getBytes(charset))
  }

  override def appendBytes(path: LogicPath, bytes: Array[Byte]): Try[Unit] = {
    withExistingParent(path) {
      Files.write(path, bytes, StandardOpenOption.CREATE, StandardOpenOption.APPEND)
    }
  }

  override def withInputStream[A](path: LogicPath)(consumer: (InputStream) => A): Try[A] = Try {
    val inputStream = Files.newInputStream(path)
    try {
      consumer(inputStream)
    } finally {
      try {
        inputStream.close()
      } catch {
        case _: Throwable =>
      }
    }
  }

  override def list(path: LogicPath): stream.Stream[Path] = Files.list(path)

  override def listFilesAndFolders(path: LogicPath): Vector[Path] = {
    val stream = Files.list(path)
    val retVector = stream.iterator().asScala.toVector
    stream.close()
    retVector
  }

  override def exists(path: LogicPath): Boolean = Files.exists(path)

  override def getNioPath(path: LogicPath): Path = Paths.get(path.toString)

  override def isDirectory(path: LogicPath): Boolean = Files.isDirectory(path)

  override def isFile(path: LogicPath): Boolean = Files.isRegularFile(path)

  override def delete(path: LogicPath): Try[Unit] = Try {
    Files.delete(path)
  }

  override def deleteDirectoryRecursive(path: LogicPath): Try[Unit] = Try {
    Files.walkFileTree(path, new SimpleFileVisitor[Path] {

      override def visitFile(file: Path, attrs: BasicFileAttributes) = {
        Files.delete(file)
        FileVisitResult.CONTINUE
      }

      override def postVisitDirectory(dir: Path, exc: IOException) = {
        Files.delete(dir)
        FileVisitResult.CONTINUE
      }
    })
  }

  override def createTempDir(parentDir: TempPath = TempPath(Seq.empty), prefix: Option[String] = None): Try[TempPath] = {
    mkDirs(parentDir) map { _ =>
      val tempPath = Files.createTempDirectory(parentDir, prefix.orNull)
      resolveTemp(parentDir.segments :+ tempPath.getFileName.toString)
    }
  }

  override def writeFiles(path: LogicPath, files: FileMap): Try[Unit] = Try {
    (files foldLeft Try {}) {
      case (result, (relativePath, data)) =>
        result flatMap { _ =>
          val resolvedPath = path.resolve(relativePath)
          if (!isContained(path, resolvedPath)) {
            throw new IOException(s"${resolvedPath.pathDescriptionUpperCase} is not contained in ${path.pathDescription}")
          }
          writeBytes(resolvedPath, data)
        }
    }
  }

  def findFiles(path: LogicPath, fileName: String): Try[FileMap] = Try {
    val basePath: Path = path
    var fileBuffer = Map.empty[String, Array[Byte]]
    Files.walkFileTree(basePath, new SimpleFileVisitor[Path] {

      override def visitFile(file: Path, attrs: BasicFileAttributes) = {
        if (attrs.isRegularFile && file.toFile.getName == fileName) {
          val relativePath = relativizeToString(basePath, file)
          val data = Files.readAllBytes(file)
          fileBuffer += relativePath -> data
        }
        FileVisitResult.CONTINUE
      }
    })
    fileBuffer

  }

  override def readFiles(path: LogicPath): Try[FileMap] = Try {
    val basePath: Path = path
    var fileBuffer = Map.empty[String, Array[Byte]]
    Files.walkFileTree(basePath, new SimpleFileVisitor[Path] {

      override def visitFile(file: Path, attrs: BasicFileAttributes) = {
        if (attrs.isRegularFile) {
          val relativePath = relativizeToString(basePath, file)
          val data = Files.readAllBytes(file)
          fileBuffer += relativePath -> data
        }
        FileVisitResult.CONTINUE
      }
    })
    fileBuffer
  }

  override def copyFile(srcFile: LogicPath, destFile: LogicPath, forceOverwrite: Boolean = true): Try[Unit] = {
    ensureParentExists(destFile) map { _ =>
      if (forceOverwrite) {
        Files.copy(srcFile, destFile, StandardCopyOption.REPLACE_EXISTING)
      } else {
        Files.copy(srcFile, destFile)
      }
    }
  }

  override def copyFileToDirectory(srcFile: LogicPath, destDirectory: LogicPath): Try[LogicPath] = {
    val fileName = srcFile.getFileName.toString
    val destFile = destDirectory.resolve(fileName)
    copyFile(srcFile, destFile) map {
      _ => destFile
    }
  }

  override def copyDirectoryToDirectory(srcDirectory: LogicPath, destDirectory: LogicPath): Try[Unit] = Try {
    mkDirs(destDirectory)
    val srcPath: Path = srcDirectory
    val destPath: Path = destDirectory
    Files.walkFileTree(srcDirectory, new SimpleFileVisitor[Path] {

      override def preVisitDirectory(dir: Path, attrs: BasicFileAttributes) = {
        val translatedDir = destPath.resolve(relativizeToPath(srcPath, dir))
        Files.createDirectories(translatedDir)
        FileVisitResult.CONTINUE
      }

      override def visitFile(file: Path, attrs: BasicFileAttributes) = {
        val translatedFile = destPath.resolve(relativizeToPath(srcPath, file))
        Files.copy(file, translatedFile, StandardCopyOption.REPLACE_EXISTING)
        FileVisitResult.CONTINUE
      }
    })
  }

  override def moveFile(srcFile: LogicPath, destFile: LogicPath): Try[Unit] = Try {
    withExistingParent(destFile) {
      Files.move(srcFile, destFile, StandardCopyOption.REPLACE_EXISTING)
    }
  }

  override def moveDirectoryToDirectory(srcDirectory: LogicPath, destDirectory: LogicPath): Try[Unit] = {
    Try {
      if (exists(destDirectory)) {
        throw new IOException(s"Cannot move directory to ${destDirectory.pathDescription}: destination already exists")
      }
      // If a directory is moved from one file system to another, this method will fail, because then the files will have
      // to be copied to the new fs, instead of a single rename of the directory inode.
      Files.move(srcDirectory, destDirectory)
    } recoverWith {
      case e: IOException =>
        // Renaming failed, try copy & delete
        copyDirectoryToDirectory(srcDirectory, destDirectory) flatMap { _ =>
          deleteDirectoryRecursive(srcDirectory)
        }
    } map {
      _ => // Map to Unit
    }
  }

  override def httpDirectoryRoute(path: LogicPath): Route = {
    // Note: Akka Http provides no interface for a Path element and requires a String instead
    // So this will only work, if the returned path String can be resolved using the File API
    // which leaves NIO tricks like JimJS out
    Directives.getFromDirectory(resolveRealPath(path).toString)
  }

  override def httpFileRoute(path: LogicPath): Route = {
    Directives.getFromFile(resolveRealPath(path).toFile)
  }

  override def listDirectoryRecursive(base: LogicPath): Try[Seq[FSListItem]] = Try {
    val basePath: Path = base
    var itemBuffer: Seq[FSListItem] = Seq.empty[FSListItem]

    def fsItem(item: Path, itemType: FSListItemType.ItemType): FSListItem = {
      val parent = normalize(resolveRelativeLogicPath(base, item.getParent))
      val relativePath = relativizeToString(basePath, item)
      FSListItem(base, parent, item.getFileName.toString, relativePath, itemType)
    }

    Files.walkFileTree(basePath, new SimpleFileVisitor[Path] {

      override def preVisitDirectory(dir: Path, attrs: BasicFileAttributes) = {
        itemBuffer :+= fsItem(dir, FSListItemType.DIRECTORY)
        FileVisitResult.CONTINUE
      }

      override def visitFile(file: Path, attrs: BasicFileAttributes) = {
        itemBuffer :+= fsItem(file, FSListItemType.FILE)
        FileVisitResult.CONTINUE
      }
    })
    itemBuffer
  }

  // TODO: Doku

  private def resolveRelativeLogicPath(basePath: LogicPath, subPath: Path): LogicPath = {
    basePath.resolve(relativizeToLogic(basePath, subPath))
  }

  private def relativizeToLogic(basePath: LogicPath, subPath: Path): Seq[String] = {
    if (resolveRealPath(basePath) == subPath) {
      Seq.empty[String]
    } else {
      Seq(relativizeToString(basePath, subPath))
    }
  }

  private def relativizeToPath(basePath: Path, subPath: Path): Path = {
    basePath.relativize(subPath)
  }

  private def relativizeToString(basePath: Path, subPath: Path): String = {
    relativizeToPath(basePath, subPath).toString.replace("\\", "/")
  }

}
