package io.simplifier.plugin.contentrepo.model

import io.simplifier.plugin.contentrepo.pluginBaseRelated.db.NamedEntity
import org.squeryl.KeyedEntity
import org.squeryl.annotations.Column

/**
 * Model: Content Repository.
 * @author Christian Simon
 */
class ContentRepository(
    @Column("content_id") var id: Int,
    @Column("name") var name: String,
    @Column("description") var description: Option[String],
    @Column("permission_object_type") var permissionObjectType: String,
    @Column("permission_object_id") var permissionObjectID: String,
    @Column("provider") var provider: String)
  extends KeyedEntity[Int] with NamedEntity