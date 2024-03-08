package io.simplifier.plugin.contentrepo.pluginBaseRelated.ads

import org.apache.commons.io.IOUtils.toByteArray

import java.io.ByteArrayInputStream
import java.util.zip.ZipInputStream
import scala.collection.mutable

trait ZipHandler {

  def unzip(zipArray: Array[Byte]): Map[String, Array[Byte]] = {
    if (zipArray.nonEmpty) {
      val zip = new ZipInputStream(new ByteArrayInputStream(zipArray))
      unzipStream(zip)
    } else {
      Map.empty
    }
  }

  def unzipStream(zipInputStream: ZipInputStream): Map[String, Array[Byte]] = {
    try {
      val zipEntries = mutable.Map.empty[String, Array[Byte]]
      var zipEntry = zipInputStream.getNextEntry
      while (zipEntry != null) {
        if (!zipEntry.isDirectory) {
          zipEntries += zipEntry.getName -> toByteArray(zipInputStream)
        }
        zipEntry = zipInputStream.getNextEntry
      }
      zipEntries.toMap
    } finally {
      zipInputStream.close()
    }
  }
}
