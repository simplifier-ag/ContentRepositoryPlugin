package io.simplifier.plugin.contentrepo.dao

import io.simplifier.plugin.contentrepo.model.{ContentRepository, PluginSchema}
import io.simplifier.plugin.contentrepo.pluginBaseRelated.db.GenericDao
import org.squeryl.PrimitiveTypeMode._
 import org.squeryl.Table

 import scala.util.Try

/**
  * DAO (Data Access Object) for Content Repositories.
  */
class ContentRepositoryDao extends GenericDao[ContentRepository, Int] {

  override def table: Table[ContentRepository] = PluginSchema.contentRepositoryT

  /**
    * Get repo by name
    *
    * @param name repo name
    * @return repository
    */
  def getByName(name: String): Try[Option[ContentRepository]] = Try(getBy(_.name === name))

  /**
    * Get all repos for one provider
    *
    * @param provider provider
    * @return list of repositories
    */
  def getAllByProvider(provider: String): Vector[ContentRepository] = getAllBy(_.provider === provider)

  /**
    * Get provider by repo id
    *
    * @param contentId repo id
    * @return provider name
    */
  def getProviderByContentId(contentId: Int): Option[String] = inTransaction{
    from(table)(cr =>
      where(cr.id === contentId)
      select cr.provider).headOptionFixed
  }
}