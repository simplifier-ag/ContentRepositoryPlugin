package io.simplifier.plugin.contentrepo.pluginBaseRelated.reducer.sizeReducer

import io.simplifier.pluginbase.util.json.NamedTupleAccess._
import io.simplifier.plugin.contentrepo.pluginBaseRelated.reducer.{DEFAULT_SIZE, LENGTH_FORMATTER, determineSerializedLength, isByteArrayEquivalent}
import org.json4s._


/**
  * The generic Size Reducer Utils Trait, that provides util functions like length determination.
  */
protected[sizeReducer] trait SizeReducerUtils {


  /**
    * Determines generic identifiers, that should be deleted when they exceed a certain maximum length.
    *
    * @param lengthWithAspects    the descending sorted sequence with all identifiers and their length.
    * @param aspectsToDelete      the current sequence with the identifiers and length to delete.
    * @param originalLength       the original length of the array/object the identifiers belong to.
    * @param maximumLength        the maximum possible length.
    * @tparam T                   the identifier type e.g. String or Int.
    *
    * @return                     the sequence with the identifiers and length to delete.
    */
  protected[sizeReducer] def determineIdentifiersToDelete[T](lengthWithAspects: Seq[(Int, T)],
                                                             aspectsToDelete: Seq[(Int, T)],
                                                             originalLength: Int,
                                                             maximumLength: Int): Seq[(Int, T)] = {
    val currentLengthToDelete: Long = aspectsToDelete.map { case (len, _) => len }.sum
    val lengthAfterDeletion: Long = originalLength - currentLengthToDelete

    if (lengthAfterDeletion < maximumLength || lengthWithAspects.isEmpty) {
      aspectsToDelete.filterNot { case (len, _) => len == 0 }
    } else {
      determineIdentifiersToDelete[T](lengthWithAspects.drop(1), aspectsToDelete :+ lengthWithAspects.head, originalLength, maximumLength)
    }
  }


  /**
    * Determines generic identifiers, that should be deleted when they exceed a certain delta length.
    *
    * @note                       the difference between this and the other functions lies, that here the original length of the object/array is not considered
    *                             but only the determined length of the identifiers to be deleted against the delta-length.
    * @param lengthWithAspects    the descending sorted sequence with all identifiers and their length.
    * @param aspectsToDelete      the current sequence with the identifiers to delete.
    * @param deltaLength          the delta length, that does not need to be exceeded.
    * @tparam T                   the identifier type e.g. String or Int.
    *
    * @return                     the sequence with the identifiers and length to delete.
    */
  protected[sizeReducer] def determineIdentifiersToDelete[T](lengthWithAspects: Seq[(Int, T)],
                                                             aspectsToDelete: Seq[(Int, T)],
                                                             deltaLength: Int): Seq[(Int, T)] = {
    val currentLengthToDelete: Long = aspectsToDelete.map { case (len, _) => len }.foldLeft(0L)(_ + _)

    if (currentLengthToDelete >= deltaLength || lengthWithAspects.isEmpty) {
      aspectsToDelete.filterNot { case (len, _) => len == 0 }
    } else {
      determineIdentifiersToDelete[T](lengthWithAspects.drop(1), aspectsToDelete :+ lengthWithAspects.head, deltaLength)
    }
  }


  /**
    * Determines the length of the provided Json-Value.
    *
    * @param value    the provided Json-Value.
    *
    * @return         the respective length.
    */
  protected[sizeReducer] def determineJSONLength(value: JValue): Int = {
    value match {
      case JString(string) => string.length
      case JArray(values) if isByteArrayEquivalent(values) => values.length
      case JArray(_) | JObject(_) => determineSerializedLength(value)
      case _ => DEFAULT_SIZE
    }
  }


  /**
    * Checkes whether the provided Json-Value has more levels.
    *
    * @param value    the provided Json-Value.
    *
    * @return         <b>true</b> if any field/value exists that contains more objects/arrays <b>false</b> otherwise.
    */
  protected[sizeReducer] def isDeeper(value: JValue): Boolean = {
    value match {
      case JObject(fields) => fields.map(_.value).exists {
        case JArray(_) => true
        case JObject(_) => true
        case _ => false
      }
      case JArray(values) => values.exists {
        case JArray(_) => true
        case JObject(_) => true
        case _ => false
      }
      case _ => false
    }
  }


  /**
    * Creates a shortened String representation.
    *
    * @note                  the String will have the form <b>[REDUCED_STRING] (... shortened [SHORTENED_LENGTH] of [ORIGINAL_LENGTH] [Byte])</b>
    * @param string          the reduced String.
    * @param originalSize    the original size of the String.
    *
    * @return                the reduced String with the reduction information.
    */
  protected[sizeReducer] def ShortenedString(string: String,
                                             originalSize: Int): String = {
    s"$string (... shortened ${LENGTH_FORMATTER.format(string.length)} of ${LENGTH_FORMATTER.format(originalSize)} [Byte])"
  }
}


/** The Size Reducer Utils Companion object */
protected[reducer] object SizeReducerUtils {


  protected[reducer] class SizeReducerException(message: String,
                                                details: JValue,
                                                error: Throwable) extends Exception

}