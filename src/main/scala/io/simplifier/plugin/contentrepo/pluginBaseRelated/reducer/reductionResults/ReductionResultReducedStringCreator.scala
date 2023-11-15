package io.simplifier.plugin.contentrepo.pluginBaseRelated.reducer.reductionResults

import io.simplifier.plugin.contentrepo.pluginBaseRelated.reducer.ReducibleJSONDataTypes._
import io.simplifier.plugin.contentrepo.pluginBaseRelated.reducer.{DEFAULT_SIZE, LENGTH_FORMATTER, ReducibleJSONDataTypes, ReductionChanges}
import io.simplifier.plugin.contentrepo.pluginBaseRelated.types.TypeTagStringCreator

import scala.reflect.runtime.universe._

/**
 * This trait provides the String creation functions for reduction results.
 */
 trait ReductionResultReducedStringCreator extends TypeTagStringCreator {


  import ReductionResultReducedStringCreator._


  /**
   * Creates the respective String for a typed value.
   *
   * @param wasReduced          the flag, whether the value was reduced or not.
   * @param originalSize        the original size.
   * @param reducedSize         the reduced size.
   * @param reductionResultType the type String of the reduction result.
   * @param typeFilterFunctor   the type filter functor to filter the type tag String.
   * @param `type`              the optional [[ReducibleJSONDataTypes]].
   * @param typeTag             the type tag for the provided value.
   * @tparam T the type parameter for the type.
   * @return the respective String for the provided typed reduction result.
   */
  def createStringForTypedValue[T](wasReduced: Boolean,
                                   originalSize: Int,
                                   reducedSize: Int,
                                   reductionResultType: String,
                                   typeFilterFunctor: String => String,
                                   `type`: Option[ReducibleJSONDataTypes])
                                  (implicit typeTag: TypeTag[T]): String = {
    val typeString: String = typeFilterFunctor(createString(typeTag))

    if (wasReduced) {
      s"The ${determineReductionResultTypeString(wasReduced, reductionResultType)} of the type: [$typeString] " +
        s"${createSizeString(reducedSize, `type`)} ${createOriginalSizeString(originalSize, `type`)}."
    } else {
      s"The ${determineReductionResultTypeString(wasReduced, reductionResultType)} of the type: [$typeString] ${createSizeString(originalSize, `type`)}."
    }
  }


  /**
   * Creates the respective String for a reduction result.
   *
   * @param wasReduced          the flag, whether the value was reduced or not.
   * @param originalSize        the original size.
   * @param reducedSize         the reduced size.
   * @param reductionResultType the type String of the reduction result.
   * @param changes             the optional [[ReductionChanges]].
   * @param `type`              the optional [[ReducibleJSONDataTypes]].
   * @return the respective String for the provided reduction result.
   */
  def createString(wasReduced: Boolean,
                   originalSize: Int,
                   reducedSize: Int,
                   reductionResultType: String,
                   changes: Option[ReductionChanges],
                   `type`: Option[ReducibleJSONDataTypes]): String = {
    if (wasReduced) {
      s"The ${determineReductionResultTypeString(wasReduced, reductionResultType)} ${createSizeString(reducedSize, `type`)} " +
        s"${createOriginalSizeString(originalSize, `type`)}${createChangesString(changes)}."
    } else {
      s"The ${determineReductionResultTypeString(wasReduced, reductionResultType)} ${createSizeString(originalSize, `type`)}."
    }
  }

  private[this] def createChangesString(changes: Option[ReductionChanges]): String = {
    changes match {
      case null | None | Some(null) => ""
      case Some(change) => s" with ${change.toString.stripSuffix(".")}"
    }
  }


  private[this] def createSizeString(reducedSize: Int,
                                     `type`: Option[ReducibleJSONDataTypes]): String = {
    reducedSize match {
      case DEFAULT_SIZE => "without a clear length"
      case other => s"with the length of ${LENGTH_FORMATTER.format(other)}${determineUnit(`type`)}"
    }
  }

  private[this] def createOriginalSizeString(originalSize: Int,
                                             `type`: Option[ReducibleJSONDataTypes]): String = {
    originalSize match {
      case DEFAULT_SIZE => "and without a clear original length"
      case other => s"and with the original length of ${LENGTH_FORMATTER.format(other)}${determineUnit(`type`)}"
    }
  }


  private[this] def determineReductionResultTypeString(wasReduced: Boolean,
                                                       reductionResultType: String): String = {
    reductionResultType.trim.toLowerCase match {
      case resultType if wasReduced && resultType.startsWith(REDUCED_PREFIX) => reductionResultType
      case _ if wasReduced => s"$REDUCED_PREFIX $reductionResultType"
      case resultType if !wasReduced && resultType.startsWith(ORIGINAL_PREFIX) => reductionResultType
      case _ if !wasReduced => s"$ORIGINAL_PREFIX $reductionResultType"
    }
  }

  private[this] def determineUnit(`type`: Option[ReducibleJSONDataTypes]): String = {
    `type` match {
      case None => " [Byte]"
      case Some(NonReducibleJson()) => ""
      case Some(JsonStringReducibleType() | JsonByteArrayEquivalentReducibleType()) => " [Byte]"
      case Some(JsonObjectReducibleType()) => " [Byte] (Compactly Rendered JSON-Object Size)"
      case Some(JsonArrayReducibleType()) => " [Byte] (Compactly Rendered JSON-Array Size)"
    }
  }
}


/** The Reduction Result String Creator Companion Object */
 object ReductionResultReducedStringCreator {

  private[ReductionResultReducedStringCreator] val ORIGINAL_PREFIX: String = "original"
  private[ReductionResultReducedStringCreator] val REDUCED_PREFIX: String = "reduced"


  /**
   * Default Functor for the type filter function.
   *
   * @param typeString the type string.
   * @return the filtered type string.
   */
  def DefaultTypeFilterFunctor(typeString: String): String = typeString
}