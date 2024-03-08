package io.simplifier.plugin.contentrepo.pluginBaseRelated.fileIo

trait LogicPath {

  def segments: Seq[String]

  def resolve(path: String, paths: String*): LogicPath

  def resolve(paths: Seq[String]): LogicPath

  override def toString: String = segments.mkString("/")

  def pathDescription: String

  def pathDescriptionUpperCase: String = pathDescription.take(1).toUpperCase + pathDescription.drop(1)

}