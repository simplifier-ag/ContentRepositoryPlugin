package io.simplifier.plugin.contentrepo.pluginBaseRelated.fileIo

case class TempPath(segments: Seq[String]) extends LogicPath {

  override def resolve(path: String, paths: String*): TempPath = resolve(path +: paths)

  override def resolve(paths: Seq[String]): TempPath = copy(segments ++ paths)

  override def pathDescription: String = "temp path " + toString

}

object TempPath {

  def apply(segment: String, segments: String*): TempPath = TempPath(segment +: segments)


}
