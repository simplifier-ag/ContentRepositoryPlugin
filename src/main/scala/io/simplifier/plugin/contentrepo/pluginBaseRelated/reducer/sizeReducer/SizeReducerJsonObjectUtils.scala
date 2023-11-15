package io.simplifier.plugin.contentrepo.pluginBaseRelated.reducer.sizeReducer

import io.simplifier.pluginbase.util.json.NamedTupleAccess._
import io.simplifier.plugin.contentrepo.pluginBaseRelated.reducer.determineSerializedLength
import org.json4s._


/**
  * This trait provides utility functions for Json-Objects, that does not require functions for other Json-Values.
  */
protected[sizeReducer] trait SizeReducerJsonObjectUtils extends SizeReducerUtils {


  /**
    * Replaces an the highest level Json-Object into JNothing, when all fields are JNothing.
    *
    * @param value    the provided Json-Value.
    *
    * @return         <b>JNothing</b> when the provided Json-Object contains only fields with JNothing or the provided value.
    */
  protected[sizeReducer] def replaceEmptyObject(value: JValue): JValue = {
    value match {
      case JObject(fields) if fields.forall(_.value == JNothing) => JNothing
      case v => v
    }
  }


  /**
    * Shortens a Json-Object by deleting the respective keys for the provided delta-length.
    *
    * @note                 deletion refers to setting the value for the respective key to JNothing.
    * @param deltaLength    the delta-length.
    * @param `object`       the provided Json-Object.
    *
    * @return               the shortened Json-Object without the keys that would exceed the provided delta length.
    */
  protected[sizeReducer] def shortenJObjectByDeltaLengthKeysDeletion(deltaLength: Int,
                                                                     `object`: JObject): JObject = {
    val keysSortedByLength: Seq[(Int, String)] = sortObjectValuesByLength(`object`)
    val keysToDelete: Seq[(Int, String)] = determineIdentifiersToDelete[String](keysSortedByLength, Seq.empty[(Int, String)], deltaLength)
    deleteJsonObjectKeys(`object`, keysToDelete)
  }


  /**
    * Shortens a Json-Object by deleting the respective keys.
    *
    * @note                   deletion refers to setting the value for the respective key to JNothing.
    * @param maximumLength    the maximum length.
    * @param `object`         the provided Json-Object.
    *
    * @return                 the shortened Json-Object without the keys that would exceed the provided maximum length.
    */
  protected[sizeReducer] def shortenJObjectByKeysDeletion(maximumLength: Int,
                                                          `object`: JObject): JObject = {
    val originalLength: Int = determineSerializedLength(`object`)
    val keysSortedByLength: Seq[(Int, String)] = sortObjectValuesByLength(`object`)
    val keysToDelete: Seq[(Int, String)] = determineIdentifiersToDelete[String](keysSortedByLength, Seq.empty[(Int, String)], originalLength, maximumLength)
    deleteJsonObjectKeys(`object`, keysToDelete)
  }


  private[this] def deleteJsonObjectKeys(`object`: JObject,
                                         keysToDelete: Seq[(Int, String)]): JObject = {
    JObject(
      `object`.obj.collect {
        case JField(name, _) if keysToDelete.exists { case (_, key) => key == name } => JField(name, JNothing)
        case field => field
      }
    )
  }


  private[this] def sortObjectValuesByLength(`object`: JObject): Seq[(Int, String)] = {
    `object`.obj.map {
      case JField(name, value) => (determineSerializedLength(value), name)
    }.sortBy { case (length, _) => length }(Ordering.Int.reverse)
  }
}