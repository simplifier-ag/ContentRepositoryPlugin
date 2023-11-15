package io.simplifier.plugin.contentrepo.model

import org.squeryl.KeyedEntity
import org.squeryl.annotations.Column

import java.sql.Timestamp

/**
  * Model: Content File.
  *
  * @author Christian Simon
  */
class ContentFile(@Column("file_id") var id: Int,
                  @Column("file_name") var fileName: String,
                  @Column("file_description") var fileDescription: String,
                  @Column("status_schema_id") var statusSchemeID: String,
                  @Column("status_id") var statusID: String,
                  @Column("security_schema_id") var securitySchemeID: String,
                  @Column("permission_object_type") var permissionObjectType: String,
                  @Column("permission_object_id") var permissionObjectID: String,
                  @Column("ext_file_id") var externalStorageFileId: Option[String],
                  @Column("folder_id") var folderID: Int,
                  @Column("user_id") var userId: Option[Long],
                  @Column("rec_date") var recDate: Option[Timestamp],
                  @Column("rec_user") var recUser: Option[String],
                  @Column("chg_date") var chgDate: Option[Timestamp],
                  @Column("chg_user") var chgUser: Option[String],
                  var extension: Option[String])
  extends KeyedEntity[Int]