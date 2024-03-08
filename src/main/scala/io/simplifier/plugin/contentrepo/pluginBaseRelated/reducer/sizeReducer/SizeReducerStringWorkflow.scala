package io.simplifier.plugin.contentrepo.pluginBaseRelated.reducer.sizeReducer

import io.simplifier.plugin.contentrepo.pluginBaseRelated.reducer.ReductionResult
import io.simplifier.plugin.contentrepo.pluginBaseRelated.reducer.reductionResults.{OriginalResult, ReductionResultString}

import scala.reflect.runtime.universe._


/**
  * This trait provides all functions for the String Reduction Workflow
  */
protected[reducer] trait SizeReducerStringWorkflow extends SizeReducerGenericWorkflow with SizeReducerUtils {


  /**
    * Determines the length of the provided String.
    *
    * @param value    the provided String.
    *
    * @return         the respective length.
    */
  protected[reducer] def determineStringLength(value: String): Int = {
    value.length
  }


  /**
    * The String-Reduction Workflow, that determines the [[ReductionResult]] for the provided String.
    *
    * @param maximumLengthSubPart    the length of each part (Here the String itself).
    * @param maximumLength           the maximum length of the whole object/array (Here it is ignored).
    * @param string                  the provided String.
    *
    * @return                        the respective [[ReductionResult]].
    */
  protected[reducer] def shortenStringWorkflow(maximumLengthSubPart: Int,
                                               maximumLength: Option[Int],
                                               string: String): ReductionResult[String] = {
    if (string.length <= maximumLengthSubPart) {
      OriginalResult(
        reducedValue = string,
        originalSize = string.length
      )(Formats, typeTag[String])
    } else {
      ReductionResultString(
        originalString = string,
        reducedString = s"${ShortenedString(string.take(maximumLengthSubPart), string.length)}"
      )
    }
  }
}