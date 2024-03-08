package io.simplifier.plugin.contentrepo.permission

import io.simplifier.pluginbase.permission.PluginPermissionObject
import io.simplifier.pluginbase.permission.PluginPermissionObjectCharacteristics.{CheckboxCharacteristic, TextFieldCharacteristic}

/**
 * Permission Object for Content Repository.
  *
  * @author Christian Simon
 */
object ContentRepoPermission extends PluginPermissionObject {

  override val technicalName = PluginPermissionObject.getTechnicalName("ContentRepo")

  override val name = "Plugin: Content Repository"

  override val description = "Plugin: Content Repository"

  val characteristicCreateRepository = "CreateRepository"
  val characteristicAccessForeignFile = "AccessForeignFile"
  val characteristicManageMimeMappings = "MimeMappings"
  val characteristicPermissionObjectType = "PermissionObjectType"
  val characteristicPermissionObjectID = "PermissionObjectID"
  val characteristicView = "view"
  val characteristicAdministrate = "administrate"

  val characteristics = Seq(
    CheckboxCharacteristic(characteristicCreateRepository, "Create Repository", "Create new Content Repository"),
    CheckboxCharacteristic(characteristicManageMimeMappings, "Mime Mapping", "Manage Mappings of File Extensions to MIME types"),
    TextFieldCharacteristic(characteristicPermissionObjectType, "Permission Object Type", "Object Type for the Content Permission", "App"),
    TextFieldCharacteristic(characteristicPermissionObjectID, "Permission Object ID", "Object ID for the Content Permission", "xyz"),
    CheckboxCharacteristic(characteristicAccessForeignFile,
      "Read and edit foreign files",
      "Permission to create, read, update and delete foreign files"),
    CheckboxCharacteristic(characteristicView, "View", "View plugin content"),
    CheckboxCharacteristic(characteristicAdministrate, "Administrate", "Administrate the plugin")
  )
  
}