package io.simplifier.plugin.contentrepo

import byDeployment.PluginRegistrationSecret
import io.simplifier.pluginbase.SimplifierPlugin


/**
 * Content Repository Plugin main app.
 */
object ContentRepoPlugin extends ContentRepoPluginLogic with SimplifierPlugin {
  val pluginSecret: String =  PluginRegistrationSecret()
}
