package io.simplifier.plugin.contentrepo.pluginBaseRelated.fileIo

import java.io.IOException
import java.nio.file.{FileSystems, Path}

/**
  * NIO-based FileSystemAccess implementation which supports only basic access through absolute paths.
  * Storage paths and temp paths are not supported, therefore no settings are required.
  */
class BasicFileSystemAccess extends NioFileSystemAccess {

  override protected[fileIo] def resolveRealPath(logicPath: LogicPath): Path = {
    logicPath match {
      case AbsolutePath(segments) => enforceAbsolutePath(FileSystems.getDefault)(segments).normalize()
      case TempPath(_) => throw new IOException(s"Only absolute paths are supported in this FileSystemAccess configuration: ${logicPath.pathDescription}")
      case StoragePath(_) => throw new IOException(s"Only absolute paths are supported in this FileSystemAccess configuration: ${logicPath.pathDescription}")
    }
  }

}

object BasicFileSystemAccess {

  def apply(): BasicFileSystemAccess = new BasicFileSystemAccess

}
