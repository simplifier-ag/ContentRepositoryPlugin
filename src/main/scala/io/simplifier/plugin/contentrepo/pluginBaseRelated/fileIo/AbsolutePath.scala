package io.simplifier.plugin.contentrepo.pluginBaseRelated.fileIo

case class AbsolutePath(segments: Seq[String]) extends LogicPath {

  override def resolve(path: String, paths: String*): AbsolutePath = resolve(path +: paths)

  override def resolve(paths: Seq[String]): AbsolutePath = copy(segments ++ paths)

  override def pathDescription: String = "absolute path " + toString

}

object AbsolutePath {

  def apply(segment: String, segments: String*): AbsolutePath = AbsolutePath(segment +: segments)
}