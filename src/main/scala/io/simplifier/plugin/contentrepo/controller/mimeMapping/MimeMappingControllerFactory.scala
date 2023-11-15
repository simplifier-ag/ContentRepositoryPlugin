package io.simplifier.plugin.contentrepo.controller.mimeMapping

import io.simplifier.plugin.contentrepo.permission.PermissionHandler

/**
  * Factory to discern which mime mapping controller is needed
  */
class MimeMappingControllerFactory(permissionHandler: PermissionHandler) {

  /**
    * Get mime mapping controller
    *
    * @return mime mapping controller
    */
  def getMimeMappingController(): MimeMappingController = {
    new MimeMappingController(permissionHandler)
  }
}
