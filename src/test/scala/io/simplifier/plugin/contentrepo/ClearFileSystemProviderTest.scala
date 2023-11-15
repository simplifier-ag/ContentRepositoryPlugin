package io.simplifier.plugin.contentrepo

import akka.actor.ActorSystem
import akka.stream.Materializer
import io.simplifier.plugin.contentrepo.definitions.exceptions.CommonExceptions._
import io.simplifier.plugin.contentrepo.definitions.exceptions.ContentFileExceptions._
import io.simplifier.plugin.contentrepo.definitions.exceptions.ContentFolderExceptions._
import io.simplifier.plugin.contentrepo.model.provider.ClearFileSystemProvider
import io.simplifier.plugin.contentrepo.model.provider.ClearFileSystemProvider.{FindFileInformation, GetFileInformation, ListFileInformation}
import io.simplifier.plugin.contentrepo.pluginBaseRelated.fileIo.{AbsolutePath, BasicFileSystemAccess}
import com.typesafe.config.Config
import org.mockito.ArgumentMatchers.any
import org.mockito.MockitoSugar
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

import java.nio.file.{InvalidPathException, Path, Paths}
import scala.util.{Failure, Success, Try}

class ClearFileSystemProviderTest extends AnyWordSpec with Matchers {

  "A ClearFileSystemProvider" when {
    "getting a file" should {
      "return the file information" in new GetFileFixture {
        provider.getFile(repoName, filePath) shouldBe GetFileInformation(filePathSegments, Success(dataArray))
      }
      "throw an Exception if the file is outside the repository" in new FileOutsideRepoFixture {
        Try(provider.getFile(repoName, filePath)) shouldBe Failure(CommonInvalidPath)
      }
      "throw an Exception if the file doesn't exist" in new FileNotExistingFixture {
        Try(provider.getFile(repoName, filePath)) shouldBe Failure(ContentFileNotFound)
      }
    }
    "getting the file size" should {
      "return the correct file size" in new FileSizeFixture {
        provider.getFileSize(repoName, filePath) shouldBe Success(fileSize)
      }
      "return a CommonInvalidCharacter exception when a invalid file path is passed" in new InvalidPathFixture {
        provider.getFileSize(repoName, filePath) shouldBe Failure(CommonInvalidCharacter(reason, "file"))
      }
      "return an UnexpectedErrorException when some other exception is thrown" in new UnexpectedExceptionFixture {
       provider.getFileSize(repoName, filePath) shouldBe Failure(CommonUnexpectedError(msg))
      }
    }
    "listing all files in a folder" should {
      "return all files" in new ListFilesFixture {
        provider.listFiles(repoName, folderPath) shouldBe Vector(retItem1, retItem2)
      }
      "throw a ContentFolderNotFound Exception, if the folder does not exist" in new FolderNotExistingFixture {
        Try(provider.listFiles(repoName, Some(folderPath))) shouldBe Failure(ClearContentFolderNotFound)
      }
      "throw an exception if the folderPath is outside of the repository" in new FolderOutsideRepoFixture {
        Try(provider.listFiles(repoName, folderPath)) shouldBe Failure(CommonInvalidPath)
      }
    }
    "finding files" should {
      "return all files" in new FindFilesFixture {
        provider.findFile(repoName, Some(folderPath) ,fileName) shouldBe Vector(retItem1, retItem2)
      }
      "throw an exception if the folder does not exist" in new FolderNotExistingFixture {
        Try(provider.findFile(repoName, Some(folderPath), fileName)) shouldBe Failure(ClearContentFolderNotFound)
      }
      "throw an exception if the folder is outside of the repo" in new FolderOutsideRepoFixture {
        Try(provider.findFile(repoName, folderPath, fileName)) shouldBe Failure(CommonInvalidPath)
      }
    }
  }

  trait BaseFixture extends MockitoSugar {
    implicit val system: ActorSystem = mock[ActorSystem]
    implicit val materializer: Materializer = mock[Materializer]
    val config: Config = mock[Config]
    val fs: BasicFileSystemAccess = mock[BasicFileSystemAccess]
    val provider = new ClearFileSystemProvider(config, fs)

    val baseDirKey = "fileSystemRepository.baseDirectory"
    val baseDir = "baseDir"

    val timeoutKey = "plugin.timeoutSeconds"
    val timeout = 60

    val repoName = "repoName"

    when(config.getString(baseDirKey)).thenReturn(baseDir)
    when(config.getInt(timeoutKey)).thenReturn(timeout)
  }

  trait GetFileFixture extends BaseFixture {
    val filePath = "path/to/file"
    val filePathSegments: Seq[String] = Seq(repoName, "path", "to", "file")
    val dataArray: Array[Byte] = Array(1.toByte, 3.toByte , 3.toByte, 7.toByte)
    when(fs.isContained(any(), any())).thenReturn(true)
    when(fs.exists(any())).thenReturn(true)
    when(fs.readBytes(any())).thenReturn(Success(dataArray))
  }

  trait FileOutsideRepoFixture extends BaseFixture {
    val filePath = "../../ImOutside.foo"
    when(fs.isContained(any(), any())).thenReturn(false)
  }

  trait FileNotExistingFixture extends BaseFixture {
    val filePath = "I/Dont/exist.bar"
    when(fs.isContained(any(), any())).thenReturn(true)
    when(fs.exists(any())).thenReturn(false)
  }

  trait FileSizeFixture extends BaseFixture {
    val filePath = "get/size/file.foo"
    val fileSize = 42L

    when(fs.fileSize(any())).thenReturn(Success(fileSize))
  }

  trait InvalidPathFixture extends BaseFixture {
    val filePath = "this/is*/an#/invalidPath.foo"
    val input = "input"
    val reason = "reason"

    when(fs.fileSize(any())).thenReturn(Failure(new InvalidPathException(input, reason)))
  }

  trait UnexpectedExceptionFixture extends BaseFixture {
    val filePath = "gnarfl"
    val msg = "NONO"
    when(fs.fileSize(any())).thenReturn(Failure(new Exception(msg)))
  }

  trait ListFilesFixture extends BaseFixture {
    val folderPath: Option[String] = Some("this/is/teh/folderpath")
    val segments: Seq[String] = Seq(repoName, "this", "is", "teh", "folderpath")
    val folderName = "folderName"
    val file1Name = "file1.foo"
    val file2Name = "file2.bar"
    val folder: Path = Paths.get(s"this/is/teh/folderpath/$folderName")
    val file1: Path = Paths.get(s"this/is/teh/folderpath/$file1Name")
    val file2: Path = Paths.get(s"this/is/teh/folderpath/$file2Name")

    when(fs.exists(any())).thenReturn(true)
    when(fs.isContained(any(), any())).thenReturn(true)
    when(fs.listFilesAndFolders(any())).thenReturn(Vector(folder, file1, file2))
    when(fs.isFile(AbsolutePath(Seq(folder.toString)))).thenReturn(false)
    when(fs.isFile(AbsolutePath(Seq(file1.toString)))).thenReturn(true)
    when(fs.isFile(AbsolutePath(Seq(file2.toString)))).thenReturn(true)

    val retItem1: ListFileInformation = ListFileInformation(file1Name, segments ++ Seq(file1Name))
    val retItem2: ListFileInformation = ListFileInformation(file2Name, segments ++ Seq(file2Name))
  }

  trait FolderNotExistingFixture extends BaseFixture {
    val folderPath = "not/existing"
    val fileName = "fileName.foo"
    when(fs.exists(any())).thenReturn(false)
  }

  trait FolderOutsideRepoFixture extends BaseFixture {
    val folderPath: Option[String] = Some("../../../Im/Outside")
    val fileName = "fileName.foo"
    when(fs.exists(any())).thenReturn(true)
    when(fs.isContained(any(), any())).thenReturn(false)

  }

  trait FindFilesFixture extends BaseFixture {
    val folderPath = "this/is/the/folderPath"
    val segments: Seq[String] = Seq(repoName, "this", "is", "the", "folderPath")
    val fileName = "fileName.foo"
    val innerFileName = "innerFolder/fileName.foo"
    val emptyArray: Array[Byte] = Array()
    val fileMap: Map[String, Array[Byte]] = Map(fileName -> emptyArray, innerFileName -> emptyArray)
    when(fs.exists(any())).thenReturn(true)
    when(fs.isContained(any(), any())).thenReturn(true)
    when(fs.findFiles(any(), any())).thenReturn(Success(fileMap))

    val retItem1: FindFileInformation = FindFileInformation(s"$folderPath/$fileName", segments ++ Seq(fileName))
    val retItem2: FindFileInformation = FindFileInformation(s"$folderPath/$innerFileName", segments ++ innerFileName.split("/"))
  }

}
