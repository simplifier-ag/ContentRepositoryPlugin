package io.simplifier.plugin.contentrepo.pluginBaseRelated.types

import io.simplifier.pluginbase.util.logging.ExceptionFormatting

import scala.reflect.runtime.universe._


/**
 * Trait that provided method for comparision of type tags.
 */
trait TypeTagMatcher extends TypeTagCreator {

  import TypeTagMatcher.{SimpleClassNames, getClassNameSafely}

  def isEqual[T: TypeTag, U: TypeTag]: Boolean = typeOf[T] =:= typeOf[U]


  def isEqual[T: TypeTag, U: TypeTag](`object`: U): Boolean = {
    getClassFromTag[T].getSimpleName == getClassNameSafely(`object`, SimpleClassNames)
  }

  def isAssignable[T: TypeTag, U: TypeTag]: Boolean = typeOf[T] <:< typeOf[U]

  def isAssignable[T: TypeTag, U: TypeTag](`object`: U): Boolean = {
    `object` match {
      case null => false
      case other => getClassFromTag[T].isAssignableFrom(other.getClass)
    }
  }
}

object TypeTagMatcher extends ExceptionFormatting {

}