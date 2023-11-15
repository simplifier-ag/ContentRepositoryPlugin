package db.migration.mysql

import db.migration.V1_5__Add_Extension_To_File_Base

//TODO Comment
/**
  * Migrate the repositories
  * Rename all repository folders to the repository name
  * Delete the ContentRepositoryConfig table
  */
class V1_5__Add_Extension_To_File extends V1_5__Add_Extension_To_File_Base {

  val nameColumn: String = "file_name"
  val extensionColumn: String = "extension"
  val fileIdColumn: String = "file_id"
  val tableName: String = "ContentFile"
  val addStatement: String =
    s"""
        | ALTER TABLE $prefix$tableName
        | ADD COLUMN $extensionColumn VARCHAR(16)
        """.stripMargin
}