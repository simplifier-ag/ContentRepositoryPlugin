package io.simplifier.plugin.contentrepo.pluginBaseRelated.fileIo

import java.io.IOException
import java.nio.file.{FileSystems, Path, Paths}

/**
  * Default implementation of NIO-Based FileSystemAccess. Support for all paths are given.
  * @param storageSettings settings containing storage base dir and temp base dir.
  */
class DefaultFileSystemAccess(storageSettings: StorageSettingsLike) extends NioFileSystemAccess {

  override protected[fileIo] def resolveRealPath(logicPath: LogicPath): Path = {
    logicPath match {
      case AbsolutePath(segments) =>
        enforceAbsolutePath(FileSystems.getDefault)(segments).normalize()
      case TempPath(segments) =>
        guardTempContainment {
          Paths.get(storageSettings.tempDir, segments: _*).normalize()
        }
      case StoragePath(segments) =>
        guardStorageContainment {
          Paths.get(storageSettings.storageDir, segments: _*).normalize()
        }
    }
  }

  /**
    * Guarantee, that a generated path is contained in the root folder denoted by the Storage base path.
    * @param path path to check
    * @return checked path
    * @throws IOException if the containment was violated
    */
  @throws[IOException]
  private def guardStorageContainment(path: Path): Path = {
    if (!path.startsWith(Paths.get(storageSettings.storageDir).normalize())) {
      throw new IOException(s"Storage path $path not contained in storage base dir")
    }
    path
  }

  /**
    * Guarantee, that a generated path is contained in the root folder denoted by the Temp base path.
    * @param path path to check
    * @return checked path
    * @throws IOException if the containment was violated
    */
  @throws[IOException]
  private def guardTempContainment(path: Path): Path = {
    if (!path.startsWith(Paths.get(storageSettings.tempDir).normalize())) {
      throw new IOException(s"Temp path $path not contained in temp base dir")
    }
    path
  }
}

object DefaultFileSystemAccess {

  /**
    * Create default file system access.
    * @param storageSettings settings containing storage base dir and temp base dir.
    * @return file system object
    */
  def apply(storageSettings: StorageSettingsLike): DefaultFileSystemAccess = new DefaultFileSystemAccess(storageSettings)

}