package io.simplifier.plugin.contentrepo.pluginBaseRelated.db

import org.squeryl.PrimitiveTypeMode._
import org.squeryl.{KeyedEntity, Table}

/**
 * Abstract entity base class with default CRUD functionality.
 */
abstract class AbstractEntity[T <: KeyedEntity[K], K] extends KeyedEntity[K] {

  self: T =>

  def table: Table[T]

  def create(): T = inTransaction {
    table.insert(this)
    this
  }

  def bulkCreate(): T = {
    table.insert(this)
    this
  }

  def update(): T = inTransaction {
    table.update(this)
    this
  }

  def bulkUpdate(): T = {
    table.update(this)
    this
  }

  def delete(): T = inTransaction {
    table.delete(this.id)
    this
  }

  def bulkDelete(): T = {
    table.delete(this.id)
    this
  }

}
