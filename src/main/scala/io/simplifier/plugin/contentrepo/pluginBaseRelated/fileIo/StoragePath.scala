package io.simplifier.plugin.contentrepo.pluginBaseRelated.fileIo

case class StoragePath(segments: Seq[String]) extends LogicPath {

  override def resolve(path: String, paths: String*): StoragePath = resolve(path +: paths)

  override def resolve(paths: Seq[String]): StoragePath = copy(segments ++ paths)

  override def pathDescription: String = "storage path " + toString

}

object StoragePath {

  def apply(segment: String, segments: String*): StoragePath = StoragePath(segment +: segments)


}
