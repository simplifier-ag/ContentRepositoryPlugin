package io.simplifier.plugin.contentrepo

import io.simplifier.plugin.contentrepo.interfaces.{ContentRepoConfigInterface, ContentRepoProxyInterface, ContentRepoSlotInterface}
import io.simplifier.plugin.contentrepo.model.PluginSchema
import io.simplifier.plugin.contentrepo.permission.ContentRepoPermission
import io.simplifier.plugin.contentrepo.pluginBaseRelated.db.{DatabaseMigration, SquerylInit}
import io.simplifier.pluginbase.SimplifierPlugin.BasicState
import io.simplifier.pluginbase.SimplifierPluginLogic
import io.simplifier.pluginbase.interfaces.{DocumentationInterfaceService, PluginBaseHttpService, SlotInterfaceService}

import scala.concurrent.Future


/**
 * Content Repository Plugin main logic.
 */
abstract class ContentRepoPluginLogic extends SimplifierPluginLogic(
  Defaults.PLUGIN_DESCRIPTION_DEFAULT, "contentRepoPlugin") {

  import scala.concurrent.ExecutionContext.Implicits.global

  val permission = ContentRepoPermission

  override def pluginPermissions = Seq(permission)

  override def startPluginServices(basicState: BasicState): Future[PluginBaseHttpService] = Future {
    logger.info("Starting up Content Repository database.")
    SquerylInit.initWith(SquerylInit.parseConfig(basicState.config))
    DatabaseMigration(basicState.config, PluginSchema).runMigration()

    val slotInterface = Some(ContentRepoSlotInterface(basicState.dispatcher, basicState.settings, basicState.config, basicState.pluginDescription,
      permission, basicState.appServerInformation))

    val proxyInterface = Some(ContentRepoProxyInterface(basicState.dispatcher, basicState.settings,
      basicState.pluginDescription, basicState.appServerInformation, basicState.config))

    val configInterface = Some(ContentRepoConfigInterface(basicState.dispatcher, basicState.settings,
      basicState.pluginDescription, basicState.appServerInformation, basicState.config))

    val documentationInterface = Some(new DocumentationInterfaceService {
      override val apiClasses: Set[Class[_]] = Set(
        classOf[ContentRepoSlotInterface.MimeMappingDocumentation],
        classOf[ContentRepoSlotInterface.ContentRepositoryDocumentation],
        classOf[ContentRepoSlotInterface.ContentFolderDocumentation],
        classOf[ContentRepoSlotInterface.ContentFileDocumentation],
        classOf[ContentRepoConfigInterface.DownloadDocumentation],
        classOf[SlotInterfaceService.Documentation]
      )
      override val title: String = "ContentRepo Plugin Client API"
      override val description: String = "Plugin to persist and read files from a file storage"
      override val externalDocsDescription: String = "Documentation for ContentRepo Plugin"
    })

    new PluginBaseHttpService(basicState.pluginDescription, basicState.settings, basicState.appServerInformation,
      proxyInterface, slotInterface, configInterface, documentationInterface)
  }
}