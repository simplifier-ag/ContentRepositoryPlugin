package io.simplifier.plugin.contentrepo.pluginBaseRelated.reducer.sizeReducer

import akka.util.ByteString
import io.simplifier.plugin.contentrepo.pluginBaseRelated.reducer.ReductionResult
import io.simplifier.plugin.contentrepo.pluginBaseRelated.reducer.reductionResults.{OriginalResult, ReductionResultByteString}

import scala.reflect.runtime.universe._

/**
  * This trait provides all functions for the ByteString Reduction Workflow.
  */
protected[reducer] trait SizeReducerByteStringWorkflow extends SizeReducerGenericWorkflow {


  /**
    * Determines the length of the provided ByteString.
    *
    * @param value    the provided ByteString.
    *
    * @return         the respective length.
    */
  protected[reducer] def determineByteStringLength(value: ByteString): Int = {
    value.size
  }


  /**
    * The ByteString-Reduction Workflow, that determines the [[ReductionResult]] for the provided ByteString.
    *
    * @param maximumLengthSubPart    the length of each part (Here the ByteString itself).
    * @param maximumLength           the maximum length of the whole object/array (Here it is ignored).
    * @param byteString              the provided ByteString.
    *
    * @return                        the respective ByteString-[[ReductionResult]].
    */
  protected[reducer] def shortenByteStringWorkflow(maximumLengthSubPart: Int,
                                                   maximumLength: Option[Int],
                                                   byteString: ByteString): ReductionResult[ByteString] = {
    if (byteString.size <= maximumLengthSubPart) {
      OriginalResult(
        reducedValue = byteString,
        originalSize = byteString.size
      )(Formats, typeTag[ByteString])
    } else {
      ReductionResultByteString(
        originalByteString = byteString,
        reducedByteString = byteString.take(maximumLengthSubPart)
      )
    }
  }
}