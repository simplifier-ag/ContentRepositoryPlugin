package db.migration

import io.simplifier.plugin.contentrepo.ContentRepoPlugin.BASIC_STATE
import io.simplifier.plugin.contentrepo.pluginBaseRelated.db.migration.ResultSetHelper
import org.apache.commons.io.FilenameUtils
import org.flywaydb.core.api.migration.{BaseJavaMigration, Context}

/**
  * Migrate the repositories
  * Rename all repository folders to the repository name
  * Delete the ContentRepositoryConfig table
  */
abstract class V1_5__Add_Extension_To_File_Base extends BaseJavaMigration {

  val nameColumn: String
  val extensionColumn: String
  val fileIdColumn: String
  val addStatement: String
  val tableName: String

  val CFG_PREFIX_KEY = "database.table_prefix"

  val prefix: String = if (BASIC_STATE.config.hasPath(CFG_PREFIX_KEY)) {
    BASIC_STATE.config.getString(CFG_PREFIX_KEY)
  } else {
    ""
  }

  override def migrate(context: Context): Unit = {

    println("##########################################################################################################")
    println("############################ Add extension to file and fill existing entries #############################")
    println("##########################################################################################################")

    val connection = context.getConnection

    val addColumn = connection.prepareStatement(addStatement)

    try {
      addColumn.executeUpdate()
    } finally {
      addColumn.close()
    }

    val getEntries = connection.prepareStatement(
      s"""
         | SELECT * FROM $prefix$tableName
       """.stripMargin
    )
    try {
      val res = getEntries.executeQuery()
      val conRes = ResultSetHelper.apply(res, asMap = true)
      conRes foreach { entry =>
        val name = entry(nameColumn).toString
        val fileId = entry(fileIdColumn)
        val extension = FilenameUtils.getExtension(name)
        val shortenedExtension =
          if(extension.length > 16) {
            println(s"Extension for file $fileId too large. Taking only the first 16 characters")
            extension.take(16)
          } else {
            extension
          }
        val updateStatement = connection.prepareStatement(
          s"""
             | UPDATE $prefix$tableName
             | SET $extensionColumn = '$shortenedExtension'
             | WHERE $fileIdColumn = $fileId
           """.stripMargin)
          try {
            updateStatement.executeUpdate()
          } finally {
            updateStatement.close()
          }
      }
    } finally {
      getEntries.close()
    }

    println("##########################################################################################################")
    println("########################### / Add extension to file and fill existing entries ############################")
    println("##########################################################################################################")
  }
}