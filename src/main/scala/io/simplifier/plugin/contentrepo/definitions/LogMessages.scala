package io.simplifier.plugin.contentrepo.definitions

import io.simplifier.plugin.contentrepo.model.{ContentFile, ContentFolder, ContentRepository}
import org.squeryl.KeyedEntity

import scala.reflect.ClassTag

object LogMessages {

  /* Meta Functions */
  def logAndReturn[T](value: T, logger: String => Unit, message: String)(implicit valueTag: ClassTag[T]): T = {
    logger(message)
    value
  }

  def contentOldTrace[T <: KeyedEntity[Int]](metaDataFunction: T => String, model: T, aspect: String)(implicit valueTag: ClassTag[T]): String =
    s"Content Repository $aspect with the following details: {${metaDataFunction(model)}} will be edited!"

  def contentActionTraceBegin[T <: KeyedEntity[Int]](metaDataFunction: T => String, model: T, aspect: String, action: String)(implicit valueTag: ClassTag[T]): String =
    s"Content Repository $aspect with the following details: {${metaDataFunction(model)}} performs the following action: {$action} !"

  def contentActionTraceEnd[T <: KeyedEntity[Int]](metaDataFunction: T => String, model: T, aspect: String, action: String)(implicit valueTag: ClassTag[T]): String =
    s"Content Repository $aspect with the following details: {${metaDataFunction(model)}} was $action successfully!"

  /* Functions */

  def createContentRepositoryMetaContent(contentRepository: ContentRepository): String =
    s"Id: {${contentRepository.id}}, Name: {${contentRepository.name}}, Description: {${contentRepository.description}}, " +
    s"Permission Object Type: {${contentRepository.permissionObjectType}} Permission Object Id: {${contentRepository.permissionObjectID}}, Provider: {${contentRepository.provider}}"

  def createContentFolderMetaContent(contentFolder: ContentFolder): String =
    s"Id: {${contentFolder.id}}, Name: {${contentFolder.folderName}}, Description: {${contentFolder.folderDescription}}, Status Schema Id: {${contentFolder.statusSchemeID}}, " +
    s"Status Id: {${contentFolder.statusID}}, Security Schema Id: {${contentFolder.securitySchemeID}}, Current Status: {${contentFolder.currentStatus}}, " +
    s"Permission Object Type: {${contentFolder.permissionObjectType}}, Permission Object Id: {${contentFolder.permissionObjectID}}, " +
    s"Parent Folder: {${contentFolder.parentFolderId.getOrElse("")}}, Content Id: {${contentFolder.contentId}}"

  def createContentFileMetaContent(contentFile: ContentFile): String =
    s"Id: {${contentFile.id}}, Name: {${contentFile.fileName}}, Description: {${contentFile.fileDescription}}, Status Schema Id: {${contentFile.statusSchemeID}}, " +
    s"Status Id: {${contentFile.statusID}}, Security Schema Id: {${contentFile.securitySchemeID}}, Permission Object Type: {${contentFile.permissionObjectType}}, " +
    s"Permission Object Id: {${contentFile.permissionObjectID}}, External Storage Field: {${contentFile.externalStorageFileId.getOrElse("")}}, Folder Id: {${contentFile.folderID}}"

  def contentFileAction(contentFile: ContentFile, action: String): String =
    s"Content Repository File: {${contentFile.fileName}} with id for file system: {${contentFile.id}} in folder: {${contentFile.folderID}} was $action successfully!"

  def fileIsNotInDatabase(fileId:Int): String = s"The file with the id: {$fileId} was not found in the database!"
  def fileIsNotInDatabase(folderId:Int, fileName:String): String = s"The file: {$fileName} in the folder with the id: {$folderId} was not found in the database!"

  def dataBaseOperationStart(id:Int, idType:String, action: String, aspect: String): String =
    s"Database operation $action for $aspect: {$idType: $id} starts!"

  def dataBaseOperationStart(id1:Int, idType1:String, id2:String, idType2:String, action: String, aspect: String): String =
    s"Database operation $action for $aspect: {$idType1: $id1 and $idType2: $id2} starts!"

  /* Failed Database Operations */
  def dataBaseOperationError(error: Throwable, action: String, aspect: String, name: String): String =
    s"Database operation $action for $aspect: {$name} could not be performed due to an error the type: {${error.getClass.getName}. " +
    s"The error message was: {${error.getMessage.replace("\n", ";")}}!"

  /* Slot Messages */
  def slotMessageBegin(action: String, aspect: String, provider: String, arg: Any): String =
    s"Performing $action of $aspect for provider $provider with argument: {$arg}!"

  def slotMessageEnd(action: String, aspect: String, provider: String, retVal: Any): String =
    s"Performing $action of $aspect for provider $provider with return value: {$retVal} was finished successfully!"

  def slotMessageEnd(action: String, aspect: String, provider: String): String =
    s"Performing $action of $aspect for provider $provider was finished successfully!"

  def slotError(action: String, aspect: String, error: Throwable): String =
    s"Error during operation $action of $aspect: $error"
}