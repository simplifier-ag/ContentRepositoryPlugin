package io.simplifier.plugin.contentrepo.pluginBaseRelated.reducer

import akka.util.ByteString
import io.simplifier.plugin.contentrepo.pluginBaseRelated.reducer.sizeReducer.SizeReducerUtils.SizeReducerException
import io.simplifier.plugin.contentrepo.pluginBaseRelated.reducer.sizeReducer._
import io.simplifier.plugin.contentrepo.pluginBaseRelated.types.TypeTagCreator
import io.simplifier.pluginbase.util.xcoder.ByteArrayEncoding
import org.json4s.{JValue, _}

import scala.util.{Failure, Success, Try}


/**
  * This traits provides methods to reduce the size of certain data types with a reduction report.
  */
trait SizeReducer extends ByteArrayEncoding
  with TypeTagCreator
  with SizeReducerStringWorkflow
  with SizeReducerByteStringWorkflow
  with SizeReducerJsonLikeWorkflow {


  import SizeReducer._


  /**
    * Shortens a [[ByteString]] to the maximum provided length of Bytes.
    *
    * @param maximumLengthSubPart    the maximum amount of Bytes.
    * @param reduce                  the flag, whether the reduction is necessary or not.
    * @param `type`                  the type of the string.
    * @param byteString              the [[ByteString]] to shorten.
    *
    * @return                 the shortened [[ByteString]] result in a Try.
    */
  def shortenByteString(maximumLengthSubPart: Int,
                        reduce: Boolean,
                        `type`: Option[String],
                        byteString: ByteString): Try[ReductionResult[ByteString]] = {
    for {
      _ <- validateProvidedLength(maximumLengthSubPart, `type`)
      result <- genericWorkflowLogic(maximumLengthSubPart, None, reduce, byteString, determineByteStringLength, shortenByteStringWorkflow)
    } yield result
  }


  /**
    * Shortens a [[JValue]] to the maximum provided length.
    *
    * @note                                  # A [[JString]] will reduced in the amount of characters.
    *                                        # For a [[JObject]] all values will reduced accordingly.
    *                                        # For a [[JArray]] that is a ByteArray equivalent, the amount of Bytes will be reduced.
    *                                        # For any other [[JArray]] all values will be reduced accordingly.
    * @note                                  a different workflow is being used, because argument expression's type is not compatible with formal parameter type
    * @param maximumLengthSingleParameter    the maximum length of a single parameter.
    * @param maximumLengthAllParameters      the optional maximum length of the parameters altogether.
    * @param reduce                          the flag, whether the reduction is necessary or not.
    * @param singleParameterType             the type of the single parameter.
    * @param totalParametersType             the type of the total parameters.
    * @param value                           the [[JValue]] to shorten.
    *
    * @return                                the shortened [[JValue]] result in a Try.
    */
  def shortenJSONValue(maximumLengthSingleParameter: Int,
                       maximumLengthAllParameters: Option[Int],
                       reduce: Boolean,
                       singleParameterType: Option[String],
                       totalParametersType: Option[String],
                       value: JValue): Try[ReductionResult[_ <: JValue]] = {
    for {
      _ <- validateProvidedLength(maximumLengthSingleParameter, singleParameterType)
      _ <- maximumLengthAllParameters.fold[Try[Unit]](Success(()))(validateProvidedLength(_, totalParametersType))
      result <- genericWorkflowLogic(maximumLengthSingleParameter, maximumLengthAllParameters, reduce, value, determineJSONLength, shortenJSONValueWorkflow)(typeTagFromObject(value))
    } yield result
  }


  /**
    * Shortens a [[JString]] to the maximum provided length of characters.
    *
    * @param maximumLengthSubPart    the maximum length of characters.
    * @param reduce                  the flag, whether the reduction is necessary or not.
    * @param `type`                  the type of the string.
    * @param string                  the [[JString]] to shorten.
    *
    * @return                 the shortened [[JString]] result in a Try.
    */
  def shortenJSONString(maximumLengthSubPart: Int,
                        reduce: Boolean,
                        `type`: Option[String],
                        string: JString): Try[ReductionResult[JString]] = {
    for {
      _ <- validateProvidedLength(maximumLengthSubPart, `type`)
      result <- genericWorkflowLogic(maximumLengthSubPart, None, reduce, string, determineJSONLength, shortenJSONStringWorkflow)
    } yield result
  }


  /**
    * Shortens a [[JArray]] to the maximum provided length.
    *
    * @note                                  # If the array is a ByteArray equivalent, the amount of Bytes will be reduced.
    *                                        # For any other array all values will be reduced accordingly.
    * @param maximumLengthSingleParameter    the maximum length of a single parameter in the array.
    * @param maximumLengthCompleteArray      the optional maximum length of the whole array.
    * @param reduce                          the flag, whether the reduction is necessary or not.
    * @param singleParameterType             the type of the single parameter.
    * @param completeArrayType               the type of the complete array.
    * @param array                           the [[JArray]] to shorten.
    *
    * @return                                the shortened [[JArray]] result in a Try.
    */
  def shortenJSONArray(maximumLengthSingleParameter: Int,
                       maximumLengthCompleteArray: Option[Int],
                       reduce: Boolean,
                       singleParameterType: Option[String],
                       completeArrayType: Option[String],
                       array: JArray): Try[ReductionResult[JArray]] = {
    for {
      _ <- validateProvidedLength(maximumLengthSingleParameter, singleParameterType)
      _ <- maximumLengthCompleteArray.fold[Try[Unit]](Success(()))(validateProvidedLength(_, completeArrayType))
      result <- genericWorkflowLogic(maximumLengthSingleParameter, maximumLengthCompleteArray, reduce, array, determineJSONLength, shortenJSONArrayWorkflow)
    } yield result
  }


  /**
    * Shortens a [[JValue]] to the maximum provided length.
    *
    * @note                                  # All values will reduced accordingly i.e. String and ByteArrays will be reduced everything else not.
    * @param maximumLengthSingleParameter    the maximum length of a single parameter in the array.
    * @param maximumLengthCompleteObject     the optional maximum length of the whole array.
    * @param reduce                          the flag, whether the reduction is necessary or not.
    * @param singleParameterType             the type of the single parameter.
    * @param completeObjectType              the type of the complete object.
    * @param `object`                        the [[JObject]] to shorten.
    *
    * @return                                the shortened [[JObject]] result in a Try.
    */
  def shortenJSONObject(maximumLengthSingleParameter: Int,
                        maximumLengthCompleteObject: Option[Int],
                        reduce: Boolean,
                        singleParameterType: Option[String],
                        completeObjectType: Option[String],
                        `object`: JObject): Try[ReductionResult[JObject]] = {
    for {
      _ <- validateProvidedLength(maximumLengthSingleParameter, singleParameterType)
      _ <- maximumLengthCompleteObject.fold[Try[Unit]](Success(()))(validateProvidedLength(_, completeObjectType))
      result <- genericWorkflowLogic(maximumLengthSingleParameter, maximumLengthCompleteObject, reduce, `object`, determineJSONLength, shortenJSONObjectWorkflow)
    } yield result
  }


  /**
    * Shortens a [[String]] to the maximum provided length of characters.
    *
    * @param maximumLengthSubPart    the maximum amount of characters.
    * @param reduce                  the flag, whether the reduction is necessary or not.
    * @param `type`                  the type of the string.
    * @param string                  the [[String]] to shorten.
    *
    * @return                        the shortened [[String]] result in a Try.
    */
  def shortenString(maximumLengthSubPart: Int,
                    reduce: Boolean,
                    `type`: Option[String],
                    string: String): Try[ReductionResult[String]] = {
    for {
      _ <- validateProvidedLength(maximumLengthSubPart, `type`)
      result <- genericWorkflowLogic(maximumLengthSubPart, None, reduce, string, determineStringLength, shortenStringWorkflow)
    } yield result
  }


  private[this] def validateProvidedLength(maximumLengthSubPart: Int,
                                           `type`: Option[String]): Try[Unit] = {
    maximumLengthSubPart match {
      case len if len < 0 => Failure(MaximumLengthMayNotBeNegative(`type`))
      case len if len == 0 => Failure(MaximumLengthMayNotBeZero(`type`))
      case _ => Success(())
    }
  }
}


/** The Size Reducer Companion Object. */
object SizeReducer {

  protected[reducer] case class MaximumLengthMayNotBeNegative(`type`: Option[String])
    extends SizeReducerException(
      message = s"The maximum length${`type`.fold("")(t => s" for $t")} may not be negative and must be positive.",
      details = JNothing,
      error = null
    )

  protected[reducer] case class MaximumLengthMayNotBeZero(`type`: Option[String])
    extends SizeReducerException(
      message = s"The maximum length${`type`.fold("")(t => s" for $t")} may not be zero and must be positive.",
      details = JNothing,
      error = null
    )

}