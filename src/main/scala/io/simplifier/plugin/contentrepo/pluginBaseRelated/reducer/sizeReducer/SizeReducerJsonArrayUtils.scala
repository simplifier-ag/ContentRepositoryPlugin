package io.simplifier.plugin.contentrepo.pluginBaseRelated.reducer.sizeReducer

import io.simplifier.plugin.contentrepo.pluginBaseRelated.reducer.determineSerializedLength
import org.json4s.{JValue, _}


/**
  * This trait provides utility functions for Json-Arrays, that does not require functions for other Json-Values.
  */
protected[sizeReducer] trait SizeReducerJsonArrayUtils extends SizeReducerUtils {

  /**
    * Replaces an the highest level Json-Array into JNothing, when all fields are JNothing.
    *
    * @param value    the provided Json-Value.
    *
    * @return         <b>JNothing</b> when the provided Json-Array contains only fields with JNothing or the provided value.
    */
  protected[sizeReducer] def replaceEmptyArray(value: JValue): JValue = {
    value match {
      case JArray(values) if values.forall(_ == JNothing) => JNothing
      case v => v
    }
  }


  /**
    * Shortens the provided Json-Array by removing the indices from a certain point onward.
    *
    * @note                   this is used for byte-array equivalents.
    * @param maximumLength    the maximum length of the Json-Array.
    * @param arrayValues      the provided Json-Array values.
    *
    * @return                 the shortened Json-Array.
    */
  protected[sizeReducer] def shortenCompleteJArrayLength(maximumLength: Int,
                                                         arrayValues: Seq[JValue]): JArray = {
    JArray(arrayValues.take(maximumLength).toList)
  }


  /**
    * Shortens a Json-Array by deleting the respective keys for a calculated delta-length.
    *
    * @note                         deletion refers to setting the value at the respective index to JNothing.
    * @param maximumLength          the maximum possible length.
    * @param shortenedArrayLength   the length of the shortened array.
    * @param array                  the provided shortened Json-Array.
    *
    * @return                       the shortened Json-Array without the keys that would exceed the calculated delta
    */
  protected[sizeReducer] def shortenJArrayByDeltaLengthIndicesDeletion(maximumLength: Int,
                                                                       shortenedArrayLength: Int,
                                                                       array: JArray): JArray = {
    val deltaLength: Int = shortenedArrayLength - maximumLength

    if(deltaLength>0) {
      array.transform {
        //The lowest level will be tried to shortened by deleting the indices.
        case a: JArray if !isDeeper(a) => shortenDeepestJArrayByIndicesDeletion(deltaLength, a)

        //The highest level will be tried to shortened by deleting the indices (this occurs, when the deletion of the lowest level does not suffice).
        case a: JArray if isDeeper(a) && determineSerializedLength(a) > maximumLength => shortenDeepestJArrayByIndicesDeletion(deltaLength, a)

        //Every other case will be returned.
        case v => v
      } match {
        case a: JArray => a
        case _ => JArray(List.empty[JValue])
      }
    } else array

  }


  /**
    * Shortens a Json-Array by deleting the respective indices.
    *
    * @note                   deletion refers to setting the value at the respective index to JNothing.
    * @param maximumLength    the maximum length.
    * @param array            the provided Json-Array.
    *
    * @return                 the shortened Json-Array without the indices that would exceed the provided maximum length.
    */
  protected[sizeReducer] def shortenJArrayByIndicesDeletion(maximumLength: Int,
                                                            array: JArray): JArray = {
    val originalLength: Int = determineSerializedLength(array)
    val indicesSortedByLength: Seq[(Int, Int)] = sortArrayValuesByLength(array)
    val indicesToDelete: Seq[(Int, Int)] = determineIdentifiersToDelete[Int](indicesSortedByLength, Seq.empty[(Int, Int)], originalLength, maximumLength)
    deleteJsonArrayIndices(array, indicesToDelete)
  }




  private[this] def deleteJsonArrayIndices(array: JArray,
                                           indicesToDelete: Seq[(Int, Int)]): JArray = {
    JArray(
      array.arr.zipWithIndex.collect {
        case (_, index) if indicesToDelete.exists { case (_, i) => i == index } => JNothing
        case (value, _) => value
      }
    )
  }


  private[this] def sortArrayValuesByLength(array: JArray): Seq[(Int, Int)] = {
    array.arr.zipWithIndex.map {
      case (value, index) => (determineSerializedLength(value), index)
    }.sortBy { case (length, _) => length }(Ordering.Int.reverse)
  }


  private[this] def shortenDeepestJArrayByIndicesDeletion(deltaLength: Int,
                                                          array: JArray): JArray = {
    val indicesSortedByLength: Seq[(Int, Int)] = sortArrayValuesByLength(array)
    val indicesToDelete: Seq[(Int, Int)] = determineIdentifiersToDelete[Int](indicesSortedByLength, Seq.empty[(Int, Int)], deltaLength)
    deleteJsonArrayIndices(array, indicesToDelete)
  }
}