package io.simplifier.plugin.contentrepo.definitions

object Constants {

  final val ACTION_SLOT_ADD: String = "add"
  final val ACTION_SLOT_LIST: String = "list"
  final val ACTION_SLOT_GET: String = "get"
  final val ACTION_SLOT_GET_METADATA: String = "get metadata"
  final val ACTION_SLOT_DELETE: String = "delete"
  final val ACTION_SLOT_EDIT: String = "edit"
  final val ACTION_SLOT_FIND: String = "find"

  final val ACTION_READ: String = "read"
  final val ACTION_LISTED: String = "listed"
  final val ACTION_DELETE: String = "deleted"
  final val ACTION_INSERT: String = "inserted"
  final val ACTION_EDIT: String = "edited"

  final val ACTION_STREAM_READ: String = "read"

  final val ASPECT_REPOSITORY_BASE: String = ""
  final val ASPECT_REPOSITORY: String = "repository"
  final val ASPECT_REPOSITORY_CONFIG: String = "repository config"
  final val ASPECT_FOLDER: String = "folder"
  final val ASPECT_FILE: String = "file"

  final val ACTION_FILE_LOADED_IO: String = "loaded (file IO)"
  final val ACTION_FILE_READ_STREAM: String = "read (stream)"
  final val ACTION_FILE_READ: String = "read"
  final val ACTION_FILE_CREATED: String = "created"
  final val ACTION_FILE_DELETED: String = "deleted"
  final val ACTION_FILE_OVERWRITTEN: String = "overwritten"

  final val ACTION_FILE_LOADING_OF_FILE_IO = "loading of the file IO object"
  final val ACTION_FILE_READING_OF_STREAM = "reading of the stream source"
  final val ACTION_FILE_READING_OF_FILE = "reading g of a file"
  final val ACTION_FILE_DELETION_OF_FILE = "deletion of a file"
  final val ACTION_FILE_CREATION_OF_FILE = "creation of a file"
  final val ACTION_FILE_OVERWRITING_OF_ENTRY = "overwriting of a file"

  final val ACTION_DATABASE_DELETION_OF_ENTRY = "deletion of an entry"
  final val ACTION_DATABASE_INSERTION_OF_ENTRY = "insertion of an entry"
  final val ACTION_DATABASE_UPDATE_OF_EXTERNAL_STORAGE = "update of the external storage reference"
}
