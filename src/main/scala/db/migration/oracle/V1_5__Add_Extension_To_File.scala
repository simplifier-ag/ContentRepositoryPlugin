package db.migration.oracle

import db.migration.V1_5__Add_Extension_To_File_Base

/**
  * Migrate the repositories
  * Rename all repository folders to the repository name
  * Delete the ContentRepositoryConfig table
  */
class V1_5__Add_Extension_To_File extends V1_5__Add_Extension_To_File_Base {

  val nameColumn: String = "FILE_NAME"
  val extensionColumn: String = "EXTENSION"
  val fileIdColumn: String = "FILE_ID"
  val tableName: String = "CONTENTFILE"
  val addStatement: String =
    s"""
        | ALTER TABLE $prefix$tableName
        | ADD $extensionColumn VARCHAR2(16)
        """.stripMargin
}