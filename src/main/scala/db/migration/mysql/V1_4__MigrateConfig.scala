package db.migration.mysql

import java.nio.file.{Files, Paths}
import java.sql.Connection
import io.simplifier.plugin.contentrepo.ContentRepoPlugin.BASIC_STATE
import io.simplifier.plugin.contentrepo.pluginBaseRelated.db.migration.ResultSetHelper
import io.simplifier.plugin.contentrepo.pluginBaseRelated.db.migration.ResultSetHelper.ResultList
import org.flywaydb.core.api.migration.{BaseJavaMigration, Context}

/**
  * Migrate the repositories
  * Rename all repository folders to the repository name
  * Delete the ContentRepositoryConfig table
  */
class V1_4__MigrateConfig extends BaseJavaMigration {

  val CFG_BASEDIR_KEY = "fileSystemRepository.baseDirectory"
  val CFG_PREFIX_KEY = "database.table_prefix"

  val prefix: String = if (BASIC_STATE.config.hasPath(CFG_PREFIX_KEY)) {
    BASIC_STATE.config.getString(CFG_PREFIX_KEY)
  } else {
    ""
  }

  val baseDir: String = if (BASIC_STATE.config.hasPath(CFG_BASEDIR_KEY)) {
    BASIC_STATE.config.getString(CFG_BASEDIR_KEY)
  } else {
    ""
  }

  /**
    * Gets all repositories with their config, compares the name to the parameter basedir.
    * If the name is different to the basedir a new folder is created with the name of the repository
    * and all files and subfolders are moved into the new folders
    * @param context the database connection context
    */
  override def migrate(context: Context): Unit = {

    println("############################################################################################")
    println("############################ Remove Config and Migrate Folders #############################")
    println("############################################################################################")

    implicit val con: Connection = context.getConnection

    val stmnt = con.prepareStatement(
      s"""
         |SELECT cr.content_id, name, value FROM ${prefix}ContentRepository AS cr
         |INNER JOIN ${prefix}ContentRepositoryConfig AS cc
         |ON cr.content_id = cc.content_id
         |WHERE cc.configkey = 'basedir' AND name <> value
       """.stripMargin)

    try {
      val res = stmnt.executeQuery()
      val conRes = ResultSetHelper.apply(res, asMap = true)

      conRes.foreach {
        a =>
          println(s"Migrating Repo ${a("name")} with base dir ${a("value")}")
          val folders = getFoldersForRepo(a("content_id").toString.toInt)
          val folderPath = Paths.get(baseDir, a("name").toString)
          if(Files.notExists(folderPath)) Files.createDirectory(folderPath)
          folders.foreach { folder =>
            println(s"Migrating folder ${folder("folder_id")}")
            val sourcePath = Paths.get(baseDir, a("value").toString, folder("folder_id").toString)
            val destPath = Paths.get(baseDir, a("name").toString, folder("folder_id").toString)
            if(Files.exists(sourcePath)) {
              Files.move(sourcePath, destPath)
              println(s"Moved Folder ${folder("folder_id")}" )
            } else {
              println(s"Folder ${folder("folder_id")} not present in filesystem")
            }
          }
      }

    } finally {
      stmnt.close()
    }
    deleteConfig
    println("############################################################################################")
    println("########################### / Remove Config and Migrate Folders ############################")
    println("############################################################################################")
  }

  def getFoldersForRepo(contentId: Int)(implicit con: Connection): ResultList = {

    val stmnt = con.prepareStatement(
      s"""
         |SELECT folder_id FROM ${prefix}ContentFolder
         |WHERE content_id = $contentId
      """.stripMargin
    )
    try {
      val res = stmnt.executeQuery()
      ResultSetHelper.apply(res, asMap = true)
    } finally {
      stmnt.close()
    }
  }

  def deleteConfig()(implicit con: Connection): Unit = {
    val stmnt = con.prepareStatement(
      s"""
        |DROP TABLE ${prefix}ContentRepositoryConfig
      """.stripMargin)

    try {
      stmnt.execute()
    } finally {
      stmnt.close()
    }
  }
}
