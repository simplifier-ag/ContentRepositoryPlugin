package io.simplifier.plugin.contentrepo.model

import io.simplifier.plugin.contentrepo.pluginBaseRelated.db.SquerylInit
import org.squeryl.PrimitiveTypeMode._
import org.squeryl.Schema

/**
 * DB Schema for Plugin Data Model.
 * @author Christian Simon
 */
object PluginSchema extends PluginSchema

/**
 * DB Schema for Plugin Data Model.
 * @author Christian Simon
 */
class PluginSchema extends Schema {

  val prefix = SquerylInit.tablePrefix.getOrElse("")

  /*
   * TABLES
   */

  val contentRepositoryT = table[ContentRepository](prefix + "ContentRepository")
  on(contentRepositoryT)(t => declare(
    t.id is autoIncremented(prefix + "S_REPO")
  ))

  val contentFolderT = table[ContentFolder](prefix + "ContentFolder")
  on(contentFolderT)(t => declare(
    t.id is autoIncremented(prefix + "S_CFOLDER")
  ))
  
  val contentFileT = table[ContentFile](prefix + "ContentFile")
  on(contentFileT)(t => declare(
    t.id is autoIncremented(prefix + "S_CFILE")
  ))

  val mimeMappingT = table[MimeMapping](prefix + "MimeMapping")
  on(mimeMappingT)(t => declare(
    t.id is autoIncremented(prefix + "S_MIME"),
    t.ext is unique
  ))

  /*
   * RELATIONS
   */

  val repoToFolder =
    oneToManyRelation(contentRepositoryT, contentFolderT).
    via(_.id === _.contentId)

  val folderToFile =
    oneToManyRelation(contentFolderT, contentFileT).
    via(_.id === _.folderID)

  val parentFolderToFolder =
    oneToManyRelation(contentFolderT, contentFolderT).
    via(_.id === _.parentFolderId)
}
