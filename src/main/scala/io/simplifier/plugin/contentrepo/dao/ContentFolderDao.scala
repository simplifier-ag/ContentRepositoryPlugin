package io.simplifier.plugin.contentrepo.dao

import io.simplifier.plugin.contentrepo.model.{ContentFolder, PluginSchema}
import io.simplifier.plugin.contentrepo.pluginBaseRelated.db.QueryHeadOptionFix.fixHeadOption
import io.simplifier.plugin.contentrepo.pluginBaseRelated.db.GenericDao
import org.squeryl.PrimitiveTypeMode._
import org.squeryl.Table

import scala.util.Try
import org.squeryl.Table

/**
  * DAO (Data Access Object) for Content Folders.
  */
class ContentFolderDao extends GenericDao[ContentFolder, Int] {

  override def table: Table[ContentFolder] = PluginSchema.contentFolderT

  def getAllByRepository(contentId: Int): Try[Seq[ContentFolder]] = Try(getAllBy(_.contentId === contentId))

  def getAllByParent(parentFolderId: Int): Try[Seq[ContentFolder]] = Try(getAllBy(_.parentFolderId === Some(parentFolderId)))

  def findByNameAndParent(contentId: Int, name: String, parentFolderId: Option[Int]): Try[Option[ContentFolder]] = Try(inTransaction {
    table.where { f => f.contentId === contentId and f.folderName === name and f.parentFolderId === parentFolderId }.headOptionFixed
  })

  def findAllByRepoAndParent(contentId: Int, parentFolderIdOpt: Option[Int]): Try[Seq[ContentFolder]] = Try(inTransaction {
    table.where { f => f.contentId === contentId and f.parentFolderId === parentFolderIdOpt }.toVector
  })

  def truncateSafely: Try[Unit] = Try(truncate())

  override def truncate(): Unit = inTransaction {
    Try(__thisDsl.update(table) { folder => setAll(folder.parentFolderId := None) }).fold(e => throw e, _ =>  super.truncate() )
  }
}