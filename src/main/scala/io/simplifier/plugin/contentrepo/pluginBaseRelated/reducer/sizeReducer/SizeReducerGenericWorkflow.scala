package io.simplifier.plugin.contentrepo.pluginBaseRelated.reducer.sizeReducer

import io.simplifier.plugin.contentrepo.pluginBaseRelated.reducer.ReductionResult
import io.simplifier.plugin.contentrepo.pluginBaseRelated.reducer.reductionResults.{OriginalResult, OriginalResultJValue}
import io.simplifier.plugin.contentrepo.pluginBaseRelated.reducer.sizeReducer.SizeReducerUtils.SizeReducerException
import io.simplifier.plugin.contentrepo.pluginBaseRelated.types.TypeTagStringCreator
import io.simplifier.pluginbase.util.json.SimplifierFormats
import io.simplifier.pluginbase.util.logging.{ExceptionFormatting, Logging}
import org.json4s.{JValue, _}

import scala.reflect.runtime.universe._
import scala.util.{Failure, Try}


/**
  * This trait provides the generic Reduction Workflow.
  */
protected[reducer] trait SizeReducerGenericWorkflow extends Logging {

  import SizeReducerGenericWorkflow._


  /** The JSON Formats */
  protected val Formats: Formats = SimplifierFormats.formats


  /**
    * The generic reduction workflow logic.
    *
    * @param maximumLengthSubPart    the length of each part.
    * @param maximumLength           the maximum length of the whole object/array.
    * @param reduce                  the flag, whether the value should be reduced or not.
    * @param value                   the value to be reduced.
    * @param valueLength             the value length determination functor.
    * @param reductionWorkflow       the workflow functor.
    * @param typeTag                 the type tag for the value.
    * @tparam T                      the type parameter for the value..
    *
    * @return                        the respective [[ReductionResult]] as a Try.
    */
  protected[reducer] def genericWorkflowLogic[T](maximumLengthSubPart: Int,
                                                 maximumLength: Option[Int],
                                                 reduce: Boolean,
                                                 value: T,
                                                 valueLength: T => Int,
                                                 reductionWorkflow: (Int, Option[Int], T) => ReductionResult[T])
                                                (implicit typeTag: TypeTag[T]): Try[ReductionResult[T]] = {
    Try {
      if (reduce) {
        logger.debug(StartingReduction[T](maximumLengthSubPart, maximumLength, value, valueLength(value)))
        logger.trace(StartingReductionTrace[T](value))

        val result: ReductionResult[T] = reductionWorkflow(maximumLengthSubPart, maximumLength, value)

        logger.trace(FinishedReductionTrace[T](result.reducedValue))
        logger.debug(FinishedReduction[T](maximumLengthSubPart, maximumLength, value, valueLength(value)))

        result
      } else {
        logger.debug(IgnoringReduction[T](value, valueLength(value)))
        logger.trace(IgnoringReductionTrace[T](value))

        value match {
          case json: JValue => OriginalResultJValue(
            reducedJValue = json,
            originalSize = valueLength(value)
          ).asInstanceOf[ReductionResult[T]]
          case _ => OriginalResult(
            reducedValue = value,
            originalSize = valueLength(value)
          )(Formats, typeTag)
        }

      }
    }.recoverWith {
      case e => val length: Int = Try(valueLength(value)).getOrElse(-1)
        logger.error(ErrorDuringReduction[T](maximumLengthSubPart, maximumLength, value, length, e))
        Failure(UnexpectedErrorDuringReduction(maximumLengthSubPart, maximumLength, value, length, e))
    }
  }
}




/** The Size Reducer Generic Workflow Companion Object */
protected[reducer] object SizeReducerGenericWorkflow extends ExceptionFormatting with TypeTagStringCreator {

  protected[reducer] case class UnexpectedErrorDuringReduction[T: TypeTag](maximumLengthSubPart: Int,
                                                                               maximumLength: Option[Int],
                                                                               value: T,
                                                                               valueLength: Int,
                                                                               error: Throwable)
    extends SizeReducerException(
      message = ErrorDuringReduction[T](maximumLengthSubPart, maximumLength, value, valueLength, error),
      details = JNothing,
      error = error
    )


  private[SizeReducerGenericWorkflow] def StartingReduction[T: TypeTag](maximumLengthSubPart: Int,
                                                                        maximumLength: Option[Int],
                                                                        value: T,
                                                                        valueLength: Int)
                                                                       (implicit typeTag: TypeTag[T]): String = {
    s"Starting the reduction of the value of the type: [${createString(typeTag)}] (or its sub-parts) with the length: [$valueLength] " +
      s"to the maximum length of: [$maximumLengthSubPart]${maximumLength.fold("")(len => s" for each sub-part and a maximum length of: [$len]")}."
  }

  private[SizeReducerGenericWorkflow] def StartingReductionTrace[T: TypeTag](value: T): String = {
    s"The value to reduce is: [$value]."
  }


  private[SizeReducerGenericWorkflow] def FinishedReduction[T: TypeTag](maximumLengthSubPart: Int,
                                                                        maximumLength: Option[Int],
                                                                        value: T,
                                                                        valueLength: Int)
                                                                       (implicit typeTag: TypeTag[T]): String = {
    s"Finished the reduction of the value of the type: [${createString(typeTag)}] (or its sub-parts) with the length: [$valueLength] " +
      s"to the maximum length of: [$maximumLengthSubPart]${maximumLength.fold("")(len => s" for each sub-part and a maximum length of: [$len]")} successfully."
  }

  private[SizeReducerGenericWorkflow] def FinishedReductionTrace[T: TypeTag](reducedValue: T): String = {
    s"The successfully reduced value is: [$reducedValue]."
  }


  private[SizeReducerGenericWorkflow] def IgnoringReduction[T: TypeTag](value: T,
                                                                        valueLength: Int)
                                                                       (implicit typeTag: TypeTag[T]): String = {
    s"The reduction of the value of the type: [${createString(typeTag)}] (or its sub-parts) with the length: [$valueLength] is not deemed as necessary, therefore the value will be returned unreduced."
  }

  private[SizeReducerGenericWorkflow] def IgnoringReductionTrace[T: TypeTag](reducedValue: T): String = {
    s"The value that will be returned unreduced is: [$reducedValue]."
  }


  private[SizeReducerGenericWorkflow] def ErrorDuringReduction[T: TypeTag](maximumLengthSubPart: Int,
                                                                           maximumLength: Option[Int],
                                                                           value: T,
                                                                           valueLength: Int,
                                                                           error: Throwable)
                                                                          (implicit typeTag: TypeTag[T]): String = {
    val rootCauseType: String = getCauseTypeSafelyWithFallback(error, classNameType = CanonicalClassNames)
    val rootCauseMessage: String = getCauseMessageSafelyWithFallback(error)

    s"An error of the type: [$rootCauseType] with the message: [$rootCauseMessage] occurred during " +
      s"the reduction of the value of the type: [${createString(typeTag)}] (or its sub-parts) with the length: [$valueLength] " +
      s"to the maximum length of: [$maximumLengthSubPart]${maximumLength.fold("")(len => s" for each sub-part and a maximum length of: [$len]")}."
  }
}