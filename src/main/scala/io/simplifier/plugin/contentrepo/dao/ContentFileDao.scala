package io.simplifier.plugin.contentrepo.dao

import io.simplifier.plugin.contentrepo.model.{ContentFile, PluginSchema}
import io.simplifier.plugin.contentrepo.pluginBaseRelated.db.GenericDao
import org.squeryl.Table
 import org.squeryl.PrimitiveTypeMode._

 import scala.util.Try

/**
  * DAO (Data Access Object) for Content Files.
  */
class ContentFileDao extends GenericDao[ContentFile, Int] {

  override def table: Table[ContentFile] = PluginSchema.contentFileT
  def getAllByFolder(folderId: Int): Try[Seq[ContentFile]] = Try(getAllBy(_.folderID === folderId))
  def getByFolderAndName(folderId: Int, name: String): Try[Option[ContentFile]] = Try(getBy(f => f.folderID === folderId and f.fileName === name))


}
