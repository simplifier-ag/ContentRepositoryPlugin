package io.simplifier.plugin.contentrepo.pluginBaseRelated.fileIo

/**
  * Common trait for storage settings, defining base directory for file system access.
  */
trait StorageSettingsLike {

  /** Base path for filesystem access */
  def storageDir: String

  /** Base path for temp files */
  def tempDir: String
}
