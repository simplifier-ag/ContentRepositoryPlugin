package io.simplifier.plugin.contentrepo.model.provider

import io.simplifier.plugin.contentrepo.contentRepoIo.StreamSource
import io.simplifier.plugin.contentrepo.model.ContentFile
import io.simplifier.pluginbase.util.io.StreamUtils.ByteSource

import scala.concurrent.Future
import scala.util.Try

/**
 * Content Provider Trait.
 * @author Christian Simon
 */
trait ContentProvider {

  val id: String
}

/**
 * Abstraction for IO operations on Content Files.
 */
trait ContentFileIO {

  def externalStorageFileId: Option[String]

  def create(dataSource: ByteSource, contentFile: ContentFile): Future[ContentFile]

  def overwrite(dataSource: ByteSource, contentFile: ContentFile): Future[ContentFile]

  def delete(contentFile: ContentFile): Try[ContentFile]

  def read(contentFile: ContentFile): Try[StreamSource]

  def readStreamSource(streamSource: StreamSource, contentFile: ContentFile): Future[Array[Byte]]

}