package io.simplifier.plugin.contentrepo.model.provider

import akka.stream.Materializer
import io.simplifier.plugin.contentrepo.definitions.exceptions.CommonExceptions._
import io.simplifier.plugin.contentrepo.definitions.exceptions.ContentFileExceptions._
import io.simplifier.plugin.contentrepo.definitions.exceptions.ContentFolderExceptions._
import io.simplifier.plugin.contentrepo.pluginBaseRelated.fileIo.{AbsolutePath, BasicFileSystemAccess, FileSystemAccess, LogicPath}
import com.typesafe.config.Config
import io.simplifier.pluginbase.util.io.StreamUtils
import io.simplifier.pluginbase.util.io.StreamUtils.ByteSource

import java.nio.file._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.duration.{Duration, SECONDS}
import scala.util.{Failure, Success, Try}
/**
 * ContentProvider: Clear File System.
 */
class ClearFileSystemProvider(config: Config,
                              fs: FileSystemAccess = BasicFileSystemAccess()
                             )
                             (implicit materializer: Materializer)
  extends ContentProvider {

  import ClearFileSystemProvider._

  lazy val baseFolder: String = config.getString("fileSystemRepository.baseDirectory").trim

  lazy val timeoutDuration: Duration = Duration(config.getInt("plugin.timeoutSeconds"), SECONDS)

  override val id: String = providerId

  /*
   * Repos
   */

  /**
    * Renames the repository folder
    *
    * @param sourceRepoName the source name
    * @param destRepoName the new name
    */
  def renameRepo(sourceRepoName: String, destRepoName: String): Unit = {
    val sourcePath = AbsolutePath(Seq(baseFolder, sourceRepoName))
    validateFolderExists(sourceRepoName, "")
    val destPath = AbsolutePath(Seq(baseFolder, destRepoName))
    fs.moveDirectoryToDirectory(sourcePath, destPath).get
  }

  /**
    * Deletes the repository folder
    *
    * @param repoName the name of the repo to be deleted
    */
  def deleteRepo(repoName: String): Unit = {
    deleteFolder(repoName, None, None)
  }

  /*
   * Folder
   */

  private def validatePath(repoName: String, folderPath: LogicPath): Unit = {
    val basePath = AbsolutePath(Seq(baseFolder, repoName))
    if(!fs.isContained(basePath, folderPath)) throw CommonInvalidPath
  }

  private def validateFolderName(folderName: String): Unit = {
    if(folderName == "/" || folderName == "\\") throw CommonInvalidCharacter(s"Invalid Character <$folderName> ", "folder")
  }

  private def wrapFileSystemAccess[A](path: LogicPath, function: LogicPath => A, kind: String) = {
    Try(function(path)) match {
      case Success(a) => a
      case Failure(ex: InvalidPathException) => throw CommonInvalidCharacter(ex.getReason, kind)
      case Failure(ex: Throwable) => throw CommonUnexpectedError(ex.getMessage)
    }
  }

  private def mkDirs(path: LogicPath): Unit = {
    fs.mkDirs(path) match {
      case Success(_) =>
      case Failure(ex: InvalidPathException) => throw ContentFolderInvalidCharacter(ex.getReason)
      case Failure(ex: Throwable) => throw ContentFolderUnexpectedError(ex.getMessage)
    }
  }

  /**
    * Checks if the folder has children
    *
    * @param folderPath the folder path
    * @return whether the folderpath has children or not
    */
  def hasChildren(folderPath: String): Boolean = {
    val path = AbsolutePath(Seq(baseFolder, folderPath))
    fs.listFilesAndFolders(path).nonEmpty
  }

  /**
    * Validates that the folder exists in the repo
    *
    * @param repoName the repo name
    * @param folderPath the folder path
    */
  def validateFolderExists(repoName: String, folderPath: String): Unit = {
    val path = AbsolutePath(Seq(baseFolder, repoName, folderPath))
    if(!wrapFileSystemAccess(path, fs.exists, "folder")) throw ClearContentFolderNotFound
  }

  /**
    * Creates a folder and all parent folders if they don't exist
    *
    * @param name the foldername
    * @param repoName the repo name (optional)
    * @param parentFolderPath the parent folder path (optional)
    * @return the path relative to the repository folder
    */
  def createFolder(name: String, repoName: Option[String] = None, parentFolderPath: Option[String] = None): String = {
    validateFolderName(name)
    val segments = Seq(baseFolder) ++ Seq(repoName, parentFolderPath).flatten ++ Seq(name)
    val path = AbsolutePath(segments)
    validatePath(repoName.getOrElse(""), path)
    mkDirs(path)
    path.toString.stripPrefix(baseFolder + "/" + repoName.getOrElse("") + "/")
  }

  /**
    * Edits a folder in a repo
    *
    * @param repoName the repo name
    * @param sourcePath the source path
    * @param destPath the new path
    */
  def editFolder(repoName: String, sourcePath: String, destPath: String): Unit = {
    validateFolderExists(repoName, sourcePath)
    val srcPath = AbsolutePath(Seq(baseFolder, repoName, sourcePath))
    val dstPath = AbsolutePath(Seq(baseFolder, repoName, destPath))
    if(fs.isContained(srcPath, dstPath)) throw CommonInvalidPath
    validatePath(repoName, dstPath)
    try {
      fs.moveDirectoryToDirectory(srcPath, dstPath).get
    } catch {
      case ex: FileSystemException => throw ContentFileIO(ex.getReason)
      case ex: Throwable => throw ex
    }
  }

  /**
    * Deletes the folder
    * If the folder is not empty and forceDelete is Some(false) or None an exception is thrown
    * If forceDelete is Some(true) the folder and all content within are deleted
    *
    * @param repoName the name of the repo
    * @param folderPath the folder path
    * @param forceDelete flag whether all content should be deleted too
    */
  def deleteFolder(repoName: String, folderPath: Option[String], forceDelete:  Option[Boolean]): Unit = {
    val path = AbsolutePath(Seq(baseFolder, repoName) ++ Seq(folderPath).flatten)
    validatePath(repoName, path)
    validateFolderExists(repoName, folderPath.getOrElse(""))
    try {
      forceDelete match {
        case Some(true) => fs.deleteDirectoryRecursive(path).get
        case _ =>
          if (fs.listFilesAndFolders(path).nonEmpty) throw ContentFolderNotEmpty
          fs.delete(path).get
      }
    } catch {
      case ex: FileSystemException => throw ContentFileIO(ex.getReason)
      case ex: Throwable => throw ex
    }
  }

  /**
    * Lists all folders contained in the specified parent folder
    *
    * @param repoName the name of the repo
    * @param parentPath the parent path
    * @return Vector of all folder names
    */
  def listFolders(repoName: String, parentPath: Option[String]): Vector[String] = {
    validateFolderExists(repoName, parentPath.getOrElse(""))
    val segments = Seq(baseFolder, repoName) ++ Seq(parentPath).flatten
    val path = AbsolutePath(segments)
    validatePath(repoName, path)
    fs.listFilesAndFolders(path).filter(p => fs.isDirectory(AbsolutePath(Seq(p.toString)))).map(_.getFileName.toString)
  }

  /**
    * Finds all folders with a given name that lie under the specified parent folder
    *
    * @param repoName the name of the repo
    * @param folderName the name to be searched for
    * @param parentFolderPath the parent folder path
    * @return Vector of all paths relative to the repo folder of the folders with the given name
    */
  def findFolder(repoName: String, folderName: String, parentFolderPath: Option[String]): Vector[String] = {
    validateFolderExists(repoName, parentFolderPath.getOrElse(""))
    val segments = Seq(baseFolder, repoName) ++ Seq(parentFolderPath).flatten
    val path = AbsolutePath(segments)
    validatePath(repoName, path)
    fs.listDirectoryRecursive(path).get.
      filter(_.name == folderName).
      filter(_.path.toString.stripSuffix("/") != path.toString).
      map(_.path.toString.stripPrefix(baseFolder + "/"  + repoName + "/")).toVector
  }

  private def folderExists(folderPath: LogicPath) = {
    wrapFileSystemAccess(folderPath, fs.exists, "folder")
  }

  /*
   * Files
   */

  private def fileExists(filePath: LogicPath): Boolean = {
    wrapFileSystemAccess(filePath, fs.exists, "file")
  }

  private def validateFileExists(filePath: LogicPath): Unit = {
    if(!fileExists(filePath)) throw ContentFileNotFound
  }

  /**
    * Creates the file in the specified folder
    * If force overwrite is Some(false) or None and the file already exists an exception is thrown
    * If force overwrite is Some(true) the file is overwritten if it exists
    *
    * @param repoName the name of the repo
    * @param parentFolder the parent folder
    * @param fileName the file name
    * @param dataSource the datasource
    * @param forceOverwrite flag whether an existing file should be overwritten
    * @return the path of the new file relative to the repository folder
    */
  def createFile(repoName: String, parentFolder: String, fileName: String, dataSource: ByteSource, forceOverwrite: Option[Boolean]): Future[String] = {
    val parentSegments = Seq(baseFolder, repoName, parentFolder)
    val parentPath = AbsolutePath(parentSegments)
    if(!folderExists(parentPath)) mkDirs(parentPath)
    val filePath = AbsolutePath(parentSegments ++ Seq(fileName))
    validatePath(repoName, filePath)
    StreamUtils.foldToByteArray(dataSource).map { array =>
      validateFileOverwriting(forceOverwrite, filePath)

      fs.writeFiles(parentPath, Map(fileName -> array)) match {
        case Failure(ex: Throwable) => throw CommonUnexpectedError(ex.getMessage)
        case Success(_) =>
      }
      filePath.toString.stripPrefix(baseFolder + "/" + repoName + "/")
    }

  }

  private def validateFileOverwriting(forceOverwrite: Option[Boolean], filePath: LogicPath): Unit = {
    if(!forceOverwrite.contains(true) && fileExists(filePath)) throw ContentFileAlreadyExists
  }

  /**
    * Copies the file to the specified folder
    * If force overwrite is Some(false) or None and the file already exists an exception is thrown
    * If force overwrite is Some(true) the file is overwritten if it exists
    *
    * @param repoName the name of the repo
    * @param sourceFile the source file
    * @param destFolder the destination folder
    * @param destFileName the name for the new file
    * @param forceOverwrite flag whether an existing file should be overwritten
    * @return the path of the new file relative to the repository folder
    */
  def copyFile(repoName: String, sourceFile: String, destFolder: String, destFileName: String, forceOverwrite: Boolean): String = {
    val srcPath = AbsolutePath(Seq(baseFolder, repoName, sourceFile))
    validateFileExists(srcPath)
    validatePath(repoName, srcPath)
    val dstPath = AbsolutePath(Seq(baseFolder, repoName, destFolder, destFileName))
    validatePath(repoName, dstPath)
    fs.copyFile(srcPath, dstPath, forceOverwrite) match {
      case Failure(_: FileAlreadyExistsException) => throw ContentFileAlreadyExists
      case Failure(ex: InvalidPathException) => throw CommonInvalidCharacter(ex.getReason, "file")
      case Failure(ex: Throwable) => throw CommonUnexpectedError(ex.getMessage)
      case Success(_) =>
    }
    dstPath.toString.stripPrefix(baseFolder + "/" + repoName + "/")
  }

  /**
    * Gets the byte content of the specified file
    *
    * @param repoName the name of the repository
    * @param filePath the path to the file
    * @return byte array representation of the file
    */
  def getFile(repoName: String, filePath: String): GetFileInformation = {
    val path = AbsolutePath(Seq(baseFolder, repoName, filePath))
    validatePath(repoName, path)
    validateFileExists(path)
    val data = fs.readBytes(path)
    GetFileInformation(getFilePathForDownloadUrl(repoName, filePath), data)
  }

  /**
    * Gets the filepath part of the downloadUrl
    *
    * @param repoName the name of the repository
    * @param filePath the path to the file
    * @return the file path for the download url
    */
  def getFilePathForDownloadUrl(repoName: String, filePath: String): Seq[String] = {
    (repoName + "/" + filePath.replace("\\", "/").replace("//", "/")).split("/")
  }

  /**
    * Gets the nio path for the file
    *
    * @param repoName the repo name
    * @param parentFolderNames the parent folders
    * @param folderName the folder containing the file
    * @param fileName the file name
    * @return the nio Path representation of the file
    */
  def getNioPath(repoName: String, parentFolderNames: Seq[String], folderName: String, fileName: String): Path = {
    val path = AbsolutePath(Seq(baseFolder, repoName) ++ parentFolderNames ++ Seq(folderName, fileName))
    fs.getNioPath(path)
  }

  /**
    * Moves or renames the file
    * If force overwrite is Some(false) or None and the file already exists an exception is thrown
    * If force overwrite is Some(true) the file is overwritten if it exists
    *
    * @param repoName the name of the repo
    * @param sourcePath the source path
    * @param destPath the destination path
    * @param forceOverwrite flag whether an existing file should be overwritten
    */
  def moveFile(repoName: String, sourcePath: String, destPath: String, forceOverwrite: Option[Boolean]): Unit = {
    val srcPath = AbsolutePath(Seq(baseFolder, repoName, sourcePath))
    validateFileExists(srcPath)
    validatePath(repoName, srcPath)
    val dstPath = AbsolutePath(Seq(baseFolder, repoName, destPath))
    validatePath(repoName, dstPath)
    validateFileOverwriting(forceOverwrite, dstPath)
    fs.moveFile(srcPath, dstPath).get
    if(!fileExists(dstPath)) throw ContentFileIO
  }

  /**
    * Deletes the file
    *
    * @param repoName the repo name
    * @param filePath the file path
    */
  def deleteFile(repoName: String, filePath: String): Unit = {
    val path = AbsolutePath(Seq(baseFolder, repoName, filePath))
    validateFileExists(path)
    validatePath(repoName, path)
    try {
      fs.deleteIfExists(path).get
    } catch {
      case ex: FileSystemException => throw ContentFileIO(ex.getReason)
      case ex: Throwable => throw ex
    }

  }

  /**
    * Lists  all files in the given parent folder
    *
    * @param repoName the repo name
    * @param folderPath the parent folder path
    * @return Vector of the names of all files contained in the parent folder
    */
  def listFiles(repoName: String, folderPath: Option[String]): Vector[ListFileInformation] = {
    validateFolderExists(repoName, folderPath.getOrElse(""))
    val segments = Seq(baseFolder, repoName) ++ Seq(folderPath).flatten
    val path = AbsolutePath(segments)
    validatePath(repoName, path)
    fs.listFilesAndFolders(path).filter(p => fs.isFile(AbsolutePath(Seq(p.toString)))).map { f =>
      val filePathSegments = (repoName + "/" + folderPath.getOrElse("") + "/" + f.getFileName.toString).replace("\\", "/").replace("//", "/").split("/").toSeq
      ListFileInformation(f.getFileName.toString, filePathSegments)
    }
  }

  /**
    * Finds all files with a given name that lie under the specified parent folder
    *
    * @param repoName the repo name
    * @param folderPath the parent folder path
    * @param fileName the file name
    * @return Vector of all paths of all files with the given name relative to the repository folder
    */
  def findFile(repoName: String, folderPath: Option[String], fileName: String): Vector[FindFileInformation] = {
    validateFolderExists(repoName, folderPath.getOrElse(""))
    val segments = Seq(baseFolder, repoName) ++ Seq(folderPath).flatten
    val path = AbsolutePath(segments)
    validatePath(repoName, path)
    fs.findFiles(path, fileName).get.keys.toVector.map { name =>
      val filePath = (folderPath match {
        case Some(fPath) => fPath + "/" + name
        case None => name
      }).replace("//", "/")
      val filePathForDownload = (repoName + "/" + filePath).replace("\\", "/").replace("//", "/").split("/").toSeq
      FindFileInformation(filePath, filePathForDownload)
    }
  }

  /**
    * Gets the file size
    *
    * @param repoName the name of the repo
    * @param filePath the path to the file
    * @return the filesize
    */
  def getFileSize(repoName: String, filePath: String): Try[Long] = {
    val path = AbsolutePath(Seq(baseFolder, repoName, filePath))
    fs.fileSize(path) match {
      case Success(fileSize) => Success(fileSize)
      case Failure(ex: InvalidPathException) => Failure(CommonInvalidCharacter(ex.getReason, "file"))
      case Failure(ex: Throwable) => Failure(CommonUnexpectedError(ex.getMessage))
    }
  }
}

object ClearFileSystemProvider {
  val providerId = "ClearFileSystem"

  case class GetFileInformation(filePathForDownload: Seq[String], data: Try[Array[Byte]])

  case class ListFileInformation(fileName: String, filePathForDownload: Seq[String])

  case class FindFileInformation(filePath: String, filePathForDownload: Seq[String])

}