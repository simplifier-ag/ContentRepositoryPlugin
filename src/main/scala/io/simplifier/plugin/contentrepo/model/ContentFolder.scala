package io.simplifier.plugin.contentrepo.model

import org.squeryl.KeyedEntity
import org.squeryl.annotations.Column

/**
 * Model: Content Folder.
 * @author Christian Simon
 */
class ContentFolder(
    @Column("folder_id") var id: Int,
    @Column("folder_name") var folderName: String,
    @Column("folder_description") var folderDescription: String,
    @Column("status_schema_id") var statusSchemeID: String,
    @Column("status_id") var statusID: String,
    @Column("security_schema_id") var securitySchemeID: String,
    @Column("current_status") var currentStatus: String,
    @Column("permission_object_type") var permissionObjectType: String,
    @Column("permission_object_id") var permissionObjectID: String,
    @Column("parent_folder") var parentFolderId: Option[Int],
    @Column("content_id") var contentId: Int)
  extends KeyedEntity[Int]