package io.simplifier.plugin.contentrepo.contentRepoIo

import java.nio.file.{Files, Path}

import akka.stream.IOResult
import akka.stream.scaladsl.{FileIO, Source}
import akka.util.ByteString

import scala.concurrent.Future
import scala.util.Try

/**
  * Abstraction for a Streaming source to read from a content file.
  * This consists of a source, combined with the original file length
  */
trait StreamSource {

  /**
    * Get length of source in bytes.
    */
  def length: Try[Long]

  /**
    * Create streaming source.
    * @param chunkSize max number of bytes in a chunk in the stream
    * @return akka source
    */
  def stream(chunkSize: Int = 8192): Try[Source[ByteString, Future[IOResult]]]

  def chunk(startPosition: Int, chunkSize: Int): Try[Source[ByteString, Future[IOResult]]]
}

/**
  * Streaming implementation from file.
  * @param path nio path
  */
class FileStreamSource(path: Path) extends StreamSource {

  def length:Try[Long] = Try(Files.size(path))

  def stream(chunkSize: Int = 8192):Try[Source[ByteString, Future[IOResult]]] = Try(FileIO.fromPath(path, chunkSize))

  def chunk(startPosition: Int, chunkSize: Int): Try[Source[ByteString, Future[IOResult]]] = Try(FileIO.fromPath(path, chunkSize, startPosition))
}