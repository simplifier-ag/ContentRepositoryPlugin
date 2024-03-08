package io.simplifier.plugin.contentrepo.dao

import io.simplifier.plugin.contentrepo.model.{MimeMapping, PluginSchema}
import io.simplifier.plugin.contentrepo.pluginBaseRelated.db.GenericDao
import org.squeryl.Table
 import org.squeryl.PrimitiveTypeMode._

 import scala.util.Try

/**
  * DAO (Data Access Object) for Mime Mappings.
  */
class MimeMappingDao extends GenericDao[MimeMapping, Int] {

  override def table: Table[MimeMapping] = PluginSchema.mimeMappingT

  /**
    * Find by extension.
    * @param ext normalized extension
    * @return found mime mapping
    */
  def findByExt(ext: String): Try[Option[MimeMapping]] = Try(getBy(_.ext === ext))

}
