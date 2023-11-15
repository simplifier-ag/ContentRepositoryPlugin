package io.simplifier.plugin.contentrepo.pluginBaseRelated.types

import scala.reflect.runtime.universe._
import scala.util.Try

/**
 * This trait provides util functions for creating strings from type tags.
 */
trait TypeTagStringCreator {

  import TypeTagStringCreator._


  /**
   * Creates the shortest possible string representation from a TypeTag[[T]]'s TPE.
   *
   * @param typeTag    the TypeTag[[T]]
   * @tparam T         the parameter of the type tag.
   *
   * @return           the string representation from the <b>TPE</b> name.
   */
  def createString[T](typeTag: TypeTag[T]): String = {
    val fromTypeSymbol: String = createStringFromTypeSymbol(typeTag)
    if (fromTypeSymbol == TYPE_TAG_TYPE_SYMBOL_EMPTY) createStringFromTPE(typeTag) else fromTypeSymbol
  }



  /**
   * Creates the string representation from a TypeTag[[T]]'s TPE.
   *
   * @param typeTag    the TypeTag[[T]]
   * @tparam T         the parameter of the type tag.
   *
   * @return           the string representation from the <b>TPE</b> name.
   */
  def createStringFromTPE[T](typeTag: TypeTag[T]): String = {
    Try(Option(typeTag.tpe.toString)
      .filter(_.nonEmpty))
      .toOption
      .flatten
      .getOrElse(TYPE_TAG_EMPTY)
  }


  /**
   * Creates the string representation from a TypeTag[[T]]'s type symbol.
   *
   * @param typeTag    the TypeTag[[T]]
   * @tparam T         the parameter of the type tag.
   *
   * @return           the string representation from the <b>typeSymbol</b> name.
   */
  def createStringFromTypeSymbol[T](typeTag: TypeTag[T]): String = {
    Try(Option(typeTag.tpe.typeSymbol.name.toString)
      .filter(_.nonEmpty))
      .toOption
      .flatten
      .getOrElse(TYPE_TAG_TYPE_SYMBOL_EMPTY)
  }
}


/** The type tag utils Companion Object. */
object TypeTagStringCreator extends TypeTagStringCreator {

  private[TypeTagStringCreator] val TYPE_TAG_TYPE_SYMBOL_EMPTY: String = "-- Empty Type Tag Type Symbol Name --"
  private[TypeTagStringCreator] val TYPE_TAG_EMPTY: String = "-- Empty Type Tag TPE Name --"
}