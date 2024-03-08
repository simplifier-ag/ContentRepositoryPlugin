package io.simplifier.plugin.contentrepo.pluginBaseRelated.ads

import akka.http.scaladsl.marshalling.{Marshaller, ToEntityMarshaller}
import akka.http.scaladsl.model.MediaTypes.{`application/octet-stream`, `application/zip`}
import akka.http.scaladsl.model.{ContentType, MediaTypes}
import akka.http.scaladsl.unmarshalling.{FromEntityUnmarshaller, Unmarshaller}
import io.simplifier.plugin.contentrepo.pluginBaseRelated.data.Checksum

import java.io.ByteArrayOutputStream
import java.util.zip.{ZipEntry, ZipOutputStream}

object ZipStructure {

  protected def init(content: Map[String, Array[Byte]]):Array[Byte] = {
    val zipContent = new ByteArrayOutputStream()
    val zip = new ZipOutputStream(zipContent)
    zip.setLevel(3)
    for ((k,v) <- content) {
      val entry = new ZipEntry(k)
      zip.putNextEntry(entry)
      zip.write(v, 0, v.length)
      zip.closeEntry()
    }
    zip.flush()
    zip.finish()
    zip.close()
    zipContent.close()
    zipContent.toByteArray
  }

  def apply(content: Map[String,Array[Byte]]): ZipStructure =
    new ZipStructure(init(content))

  def apply(zip: Array[Byte]): ZipStructure =
    new ZipStructure(zip)

  def empty: ZipStructure = apply(Map.empty[String, Array[Byte]])

  val zipMarshallerAsApplicationZip: ToEntityMarshaller[ZipStructure] =
    Marshaller.byteArrayMarshaller(ContentType(MediaTypes.`application/zip`)).compose(_.getZip)

  val zipMarshallerAsOctetStream: ToEntityMarshaller[ZipStructure] =
    Marshaller.byteArrayMarshaller(ContentType(MediaTypes.`application/octet-stream`)).compose(_.getZip)

  implicit val zipMarshaller: ToEntityMarshaller[ZipStructure] = Marshaller.oneOf(zipMarshallerAsApplicationZip, zipMarshallerAsOctetStream)

  implicit val zipUnmarshaller: FromEntityUnmarshaller[ZipStructure] =
    Unmarshaller.byteArrayUnmarshaller
      .forContentTypes(`application/zip`, `application/octet-stream`)
      .map(ZipStructure.apply)
}

/**
  * Wrapper for ZIP data.
  *
  * @param zipStructure byte array containing the initial ZIP contents
  */
class ZipStructure(zipStructure : Array[Byte]) extends ZipHandler {

  private[this] lazy val zipData = all

  /**
    * Load all files including byte data from ZIP
    * @return map in form: path -> bytes
    */
  private[this] def all: Map[String,Array[Byte]] = {
    unzip(zipStructure)
  }

  /**
    * Get all files in ZIP.
    * @return map in form: path -> bytes
    */
  def files: Map[String, Array[Byte]] = zipData

  /**
    * Get single file.
    * @param path path to look for
    * @return [[Option]] of byte data
    */
  def getFile(path:String) : Option[Array[Byte]] = zipData get path

  def getZip = zipStructure

  def getChecksum: String = Checksum.checksum(zipStructure)

}
