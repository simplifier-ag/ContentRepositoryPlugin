package db.migration.oracle

import io.simplifier.plugin.contentrepo.ContentRepoPlugin.BASIC_STATE
import io.simplifier.plugin.contentrepo.pluginBaseRelated.db.migration.ResultSetHelper
import io.simplifier.plugin.contentrepo.pluginBaseRelated.db.migration.ResultSetHelper.ResultList
import org.flywaydb.core.api.migration.{BaseJavaMigration, Context}

import java.nio.file.{Files, Paths}
import java.sql.Connection

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
         |SELECT ${prefix}CONTENTREPOSITORY.CONTENT_ID, NAME, VALUE FROM ${prefix}CONTENTREPOSITORY
         |INNER JOIN ${prefix}CONTENTREPOSITORYCONFIG
         |ON ${prefix}CONTENTREPOSITORY.CONTENT_ID = ${prefix}CONTENTREPOSITORYCONFIG.CONTENT_ID
         |WHERE CONFIGKEY = 'basedir' AND NAME <> VALUE
       """.stripMargin)

    try {
      val res = stmnt.executeQuery()
      val conRes = ResultSetHelper.apply(res, asMap = true)

      conRes.foreach {
        a =>
          println(s"Migrating Repo ${a("NAME")} with base dir ${a("VALUE")}")
          val folders = getFoldersForRepo(a("CONTENT_ID").toString.toInt)
          val folderPath = Paths.get(baseDir, a("NAME").toString)
          if(Files.notExists(folderPath)) Files.createDirectory(folderPath)
          folders.foreach { folder =>
            println(s"Migrating folder ${folder("FOLDER_ID")}")
            val sourcePath = Paths.get(baseDir, a("VALUE").toString, folder("FOLDER_ID").toString)
            val destPath = Paths.get(baseDir, a("NAME").toString, folder("FOLDER_ID").toString)
            if(Files.exists(sourcePath)) {
              Files.move(sourcePath, destPath)
              println(s"Moved Folder ${folder("FOLDER_ID")}" )
            } else {
              println(s"Folder ${folder("FOLDER_ID")} not present in filesystem")
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
         |SELECT FOLDER_ID FROM ${prefix}CONTENTFOLDER
         |WHERE CONTENT_ID = $contentId
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
        |DROP TABLE ${prefix}CONTENTREPOSITORYCONFIG
      """.stripMargin)

    try {
      stmnt.execute()
    } finally {
      stmnt.close()
    }
  }
}
