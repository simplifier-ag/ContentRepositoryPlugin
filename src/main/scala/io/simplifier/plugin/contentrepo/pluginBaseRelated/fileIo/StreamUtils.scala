package io.simplifier.plugin.contentrepo.pluginBaseRelated.fileIo

import akka.stream.Materializer
import akka.stream.scaladsl.Source
import akka.util.ByteString

import scala.collection.immutable
import scala.concurrent.{ExecutionContext, Future}

/**
  * Utilities for constructing streams.
  */
object StreamUtils {

  type ByteSource = Source[ByteString, Any]

  /**
    * Create source of ByteString from compact byte array.
    *
    * @param bytes     bytes to build source from
    * @param chunkSize maximum number of bytes per stream element (default: 8192)
    * @return source of ByteString for use in e.g. FileIO
    */
  def byteArraySource(bytes: Array[Byte], chunkSize: Int = 8192): ByteSource = {
    Source(bytes.grouped(chunkSize).to[immutable.Seq].map(ByteString.fromArray))
  }

}
