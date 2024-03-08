package io.simplifier.plugin.contentrepo.pluginBaseRelated.reducer.sizeReducer

import io.simplifier.pluginbase.util.json.NamedTupleAccess._
import io.simplifier.plugin.contentrepo.pluginBaseRelated.reducer.reductionResults.{OriginalResultJValue, ReductionResultJArray, ReductionResultJObject, ReductionResultJString}
import io.simplifier.plugin.contentrepo.pluginBaseRelated.reducer.{ReductionResult, determineSerializedLength, isByteArrayEquivalent}
import org.json4s._


/**
  * This trait provides all functions for any Json-Alike Reduction Workflows.
  */
protected[reducer] trait SizeReducerJsonLikeWorkflow
  extends SizeReducerGenericWorkflow
    with SizeReducerJsonArrayUtils
    with SizeReducerJsonObjectUtils
    with SizeReducerReductionChangeDeterminer {


  /**
    * The Json-Value-Reduction Workflow, that determines the [[ReductionResult]] for the provided Json-Value.
    *
    * @param maximumLengthSubPart    the length of each part.
    * @param maximumLength           the maximum length of the whole object/array.
    * @param value                   the provided Json-Value.
    *
    * @return                        the respective Json-Value-[[ReductionResult]].
    */
  protected[reducer] def shortenJSONValueWorkflow(maximumLengthSubPart: Int,
                                                  maximumLength: Option[Int],
                                                  value: JValue): ReductionResult[_ <: JValue] = {
    value match {
      case string: JString => shortenJSONStringWorkflow(maximumLengthSubPart, maximumLength, string)
      case array: JArray => shortenJSONArrayWorkflow(maximumLengthSubPart, maximumLength, array)
      case obj: JObject => shortenJSONObjectWorkflow(maximumLengthSubPart, maximumLength, obj)
      case other => returnOriginalValue(other)
    }
  }


  /**
    * The Json-String Reduction Workflow, that determines the [[ReductionResult]] for the provided Json-String.
    *
    * @param maximumLengthSubPart    the length of each part (Here the Json-String itself).
    * @param maximumLength           the maximum length of the whole object/array (Here it is ignored).
    * @param string                  the provided Json-String.
    *
    * @return                        the respective Json-String-[[ReductionResult]].
    */
  protected[reducer] def shortenJSONStringWorkflow(maximumLengthSubPart: Int,
                                                   maximumLength: Option[Int],
                                                   string: JString): ReductionResult[JString] = {
    if (string.s.length <= maximumLengthSubPart) {
      OriginalResultJValue(
        reducedJValue = string,
        originalSize = determineJSONLength(string)
      )
    } else {
      ReductionResultJString(
        originalJString = string,
        reducedJString = JString(s"${ShortenedString(string.s.take(maximumLengthSubPart), string.s.length)}")
      )
    }
  }


  /**
    * The Json-Array Reduction Workflow, that determines the [[ReductionResult]] for the provided Json-Array.
    *
    * @param maximumLengthSubPart    the length of each part.
    * @param maximumLength           the maximum length of the whole object/array (Here the Json-Array).
    * @param array                   the provided Json-Array.
    *
    * @return                        the respective Json-Array-[[ReductionResult]].
    */
  protected[reducer] def shortenJSONArrayWorkflow(maximumLengthSubPart: Int,
                                                  maximumLength: Option[Int],
                                                  array: JArray): ReductionResult[JArray] = {

    if (requireArrayShortening(maximumLengthSubPart, maximumLength, array.arr)) {
      val shortenedArray: JArray = shortenJArray(maximumLengthSubPart, maximumLength, array)
      ReductionResultJArray(
        originalArray = array,
        reducedArray = shortenedArray,
        changes = determineArrayChanges(shortenedArray.arr, array.arr)
      )
    } else {
      OriginalResultJValue(
        reducedJValue = array,
        originalSize = determineJSONLength(array)
      )
    }
  }


  /**
    * The Json-Object Reduction Workflow, that determines the [[ReductionResult]] for the provided Json-Object.
    *
    * @param maximumLengthSubPart    the length of each part.
    * @param maximumLength           the maximum length of the whole object/array (Here the Json-Object).
    * @param `object`                the provided Json-Object.
    *
    * @return                        the respective Json-Object-[[ReductionResult]].
    */
  protected[reducer] def shortenJSONObjectWorkflow(maximumLengthSubPart: Int,
                                                   maximumLength: Option[Int],
                                                   `object`: JObject): ReductionResult[JObject] = {

    if (requireObjectShortening(maximumLengthSubPart, maximumLength, `object`.obj)) {
      val shortenedObject: JObject = shortenJObject(maximumLengthSubPart, maximumLength, `object`)
      ReductionResultJObject(
        originalObject = `object`,
        reducedObject = shortenedObject,
        changes = determineObjectChanges(shortenedObject.obj, `object`.obj)
      )
    } else {
      OriginalResultJValue(
        reducedJValue = `object`,
        originalSize = determineJSONLength(`object`)
      )
    }
  }


  private[this] def hasDeeperLevelsWithExceedingLength(value: JValue,
                                                       maximumLength: Int): Boolean = {
    if (isDeeper(value)) {
      value match {
        case JObject(fields) => hasValuesThatExceedsLength(fields.map(_.value), maximumLength)
        case JArray(values) => hasValuesThatExceedsLength(values, maximumLength)
        case _ => false
      }
    } else false
  }


  private[this] def hasPrimitiveFieldsThatExceedsLength(fields: Seq[JField],
                                                        maximumSubPartLength: Int): Boolean = {
    fields.exists {
      case JField(_, string: JString) => determineJSONLength(string) > maximumSubPartLength
      case JField(_, array: JArray) if isByteArrayEquivalent(array.arr) => determineJSONLength(array) > maximumSubPartLength
      case JField(_, array: JArray) => hasPrimitiveValuesThatExceedsLength(array.arr, maximumSubPartLength)
      case JField(_, obj: JObject) => hasPrimitiveFieldsThatExceedsLength(obj.obj, maximumSubPartLength)
      case _ => false
    }
  }


  private[this] def hasPrimitiveValuesThatExceedsLength(arrayValues: Seq[JValue],
                                                        maximumSubPartLength: Int): Boolean = {
    arrayValues.exists {
      case string: JString => determineJSONLength(string) > maximumSubPartLength
      case array: JArray if isByteArrayEquivalent(array.arr) => determineJSONLength(array) > maximumSubPartLength
      case array: JArray => hasPrimitiveValuesThatExceedsLength(array.arr, maximumSubPartLength)
      case obj: JObject => hasPrimitiveFieldsThatExceedsLength(obj.obj, maximumSubPartLength)
      case _ => false
    }
  }


  private[this] def hasValuesThatExceedsLength(values: Seq[JValue],
                                               maximumLength: Int): Boolean = {
    values.exists {
      case array: JArray => determineSerializedLength(array) > maximumLength
      case obj: JObject => determineSerializedLength(obj) > maximumLength
      case _ => false
    }
  }


  private[this] def returnOriginalValue(value: JValue): ReductionResult[JValue] = {
    OriginalResultJValue(
      reducedJValue = value,
      originalSize = determineJSONLength(value)
    )
  }


  private[this] def requireArrayShortening(maximumLengthSubPart: Int,
                                           maximumLength: Option[Int],
                                           arrayValues: Seq[JValue]): Boolean = {
    arrayValues match {
      case _ if isByteArrayEquivalent(arrayValues) => arrayValues.size > maximumLengthSubPart
      case _ if hasPrimitiveValuesThatExceedsLength(arrayValues, maximumLengthSubPart) => true
      case _ => maximumLength match {
        case Some(maximumWholeLength) =>
          val array: JArray = JArray(arrayValues.toList)
          val hasMoreLevels: Boolean = isDeeper(array)
          val hasExceedingPrimitiveValues = hasPrimitiveValuesThatExceedsLength(arrayValues, maximumLengthSubPart)
          val serializedLength: Int = determineSerializedLength(array)

          (hasMoreLevels, serializedLength > maximumWholeLength) match {
            case (false, false) if hasExceedingPrimitiveValues => true
            case (false, false) => false
            case (true, false) => requireArrayValuesShortenings(maximumLengthSubPart, maximumLength, arrayValues)
            case (_, true) => true
          }
        case None => requireArrayValuesShortenings(maximumLengthSubPart, maximumLength, arrayValues)
      }
    }
  }


  private[this] def requireArrayValuesShortenings(maximumLengthSubPart: Int,
                                                  maximumLength: Option[Int],
                                                  arrayValues: Seq[JValue]): Boolean = {
    arrayValues.exists {
      case JString(string) => requireStringShortening(maximumLengthSubPart, string)
      case JArray(values) => requireArrayShortening(maximumLengthSubPart, maximumLength, values)
      case JObject(fields) => requireObjectShortening(maximumLengthSubPart, maximumLength, fields)
      case _ => false
    }
  }


  private[this] def requireObjectFieldsShortenings(maximumLengthSubPart: Int,
                                                   maximumLength: Option[Int],
                                                   objectFields: Seq[JField]): Boolean = {
    objectFields.exists {
      case JField(_, JString(string)) => requireStringShortening(maximumLengthSubPart, string)
      case JField(_, JArray(values)) => requireArrayShortening(maximumLengthSubPart, maximumLength, values)
      case JField(_, JObject(f)) => requireObjectShortening(maximumLengthSubPart, maximumLength, f)
      case _ => false
    }
  }


  private[this] def requireObjectShortening(maximumLengthSubPart: Int,
                                            maximumLength: Option[Int],
                                            fields: Seq[JField]): Boolean = {
    maximumLength match {
      case Some(maximumWholeLength) =>
        val `object`: JObject = JObject(fields.toList)
        val hasMoreLevels: Boolean = isDeeper(`object`)
        val hasExceedingPrimitiveValues = hasPrimitiveFieldsThatExceedsLength(fields, maximumLengthSubPart)
        val serializedLength: Int = determineSerializedLength(`object`)

        (hasMoreLevels, serializedLength > maximumWholeLength) match {
          case (false, false) if hasExceedingPrimitiveValues => true
          case (false, false) => false
          case (true, false) => requireObjectFieldsShortenings(maximumLengthSubPart, maximumLength, fields)
          case (_, true) => true
        }
      case None => requireObjectFieldsShortenings(maximumLengthSubPart, maximumLength, fields)
    }
  }


  private[this] def requireStringShortening(maximumLengthSubPart: Int,
                                            string: String): Boolean = {
    string.length > maximumLengthSubPart
  }


  private[this] def shortenDeeperJArray(maximumLength: Int,
                                        maximumLengthSubPart: Int,
                                        array: JArray): JArray = {
    //Best effort reduction by reducing the primitive elements.
    JArray(
      array.arr.map {
        //The value has more levels => dive into deeper level an recursive reduction attempt.
        case value if isDeeper(value) => replaceEmptyArray(shortenJSONValueWorkflow(maximumLengthSubPart, Some(maximumLength), value).getReducedValueAsJValue(Formats))

        //The value is at the deepest level => reduction attempt of the value:
        case value =>
          //The value will be shortened (without a frame length) => reduction of primitives.
          val shortenedValue: JValue = replaceEmptyArray(shortenJSONValueWorkflow(maximumLengthSubPart, None, value).getReducedValueAsJValue(Formats))

          //If the shortening suffices, then the shortened value will be returned else the shortening will be reapplied with a frame length..
          if (determineSerializedLength(shortenedValue) > maximumLength) {
            replaceEmptyArray(shortenJSONValueWorkflow(maximumLengthSubPart, Some(maximumLength), value).getReducedValueAsJValue(Formats))
          } else shortenedValue
      }
    )
  }


  private[this] def shortenDeeperJObject(maximumLength: Int,
                                         maximumLengthSubPart: Int,
                                         `object`: JObject): JObject = {
    //Best effort reduction by reducing the primitive elements.
    JObject(
      `object`.obj.map {
        //The value has more levels => dive into deeper level an recursive reduction attempt.
        case JField(name, value) if isDeeper(value) => JField(name, replaceEmptyObject(shortenJSONValueWorkflow(maximumLengthSubPart, Some(maximumLength), value).getReducedValueAsJValue(Formats)))

        //The value is at the deepest level => reduction attempt of the value:
        case JField(name, value) =>
          //The value will be shortened (without a frame length) => reduction of primitives.
          val shortenedValue: JValue = replaceEmptyArray(shortenJSONValueWorkflow(maximumLengthSubPart, None, value).getReducedValueAsJValue(Formats))

          //If the shortening suffices, then the shortened value will be returned else the shortening will be reapplied with a frame length..
          if (determineSerializedLength(shortenedValue) > maximumLength) {
            JField(name, replaceEmptyObject(shortenJSONValueWorkflow(maximumLengthSubPart, Some(maximumLength), value).getReducedValueAsJValue(Formats)))
          } else JField(name, shortenedValue)
      }
    )
  }


  private[this] def shortenJArray(maximumLengthSubPart: Int,
                                  maximumLength: Option[Int],
                                  array: JArray): JArray = {
    if (isByteArrayEquivalent(array.arr) && array.arr.size > maximumLengthSubPart) {
      shortenCompleteJArrayLength(maximumLengthSubPart, array.arr)
    } else {
      JArray(
        maximumLength match {
          case None => array.arr.map(shortenJSONValueWorkflow(maximumLengthSubPart, maximumLength, _).getReducedValueAsJValue(Formats))
          case Some(maxLength) =>
            val exceedsItself: Boolean = determineSerializedLength(array) > maxLength
            val hasDeeperExceedingValues: Boolean = hasDeeperLevelsWithExceedingLength(array, maxLength)

            val shortenedArray: List[JValue] = (exceedsItself, hasDeeperExceedingValues) match {
              case (false, false) => array.arr.map(shortenJSONValueWorkflow(maximumLengthSubPart, None, _).getReducedValueAsJValue(Formats))
              case (true, false) => shortenJArrayByIndicesDeletion(maxLength, maximumLengthSubPart, array).arr
              case (_, true) => shortenDeeperJArray(maxLength, maximumLengthSubPart, array).arr
            }

            //2nd pass to check if the array still exceeds the limit.
            val shortenedArrayLength: Int = determineSerializedLength(JArray(shortenedArray))

            //If the limit is exceeded, then a delta length indices deletion will be performed.
            if (shortenedArrayLength > maxLength) {
              shortenJArrayByDeltaLengthIndicesDeletion(maxLength, shortenedArrayLength, JArray(shortenedArray)).arr
            } else shortenedArray

        }
      )
    }
  }


  private[this] def shortenJArrayByIndicesDeletion(maximumLength: Int,
                                                   maximumLengthSubPart: Int,
                                                   array: JArray): JArray = {

    //If the array contains deeper levels, then a best effort reduction will be performed:
    if (isDeeper(array)) {
      JArray(
        array.arr.map {
          //The value is an array and has more levels.
          case a: JArray if isDeeper(a) =>

            //The serialized length exceeds the limit => dive into deeper level an recursive reduction attempt.
            if (determineSerializedLength(a) > maximumLength) {
              shortenJArrayRecursively(maximumLengthSubPart, Some(maximumLength), a)
            } else { //The serialized length keeps the limit => value will be used as is (but primitives will be shortened as well).
              shortenJArrayRecursively(maximumLengthSubPart, None, a)
            }

          //The array is at the deepest level but exceeds the limit => reduction attempt of the array:
          case a: JArray if determineSerializedLength(a) > maximumLength =>

            //The array will be shortened.
            val shortenedValue: JValue = shortenJArrayRecursively(maximumLengthSubPart, None, a)

            //If the shortening suffices, then the shortened version will be returned else JNothing.
            if (determineSerializedLength(shortenedValue) > maximumLength) JNothing else replaceEmptyArray(shortenedValue)

          //The array is at the deepest level and keeps the limit => value will be used as is (but primitives will be shortened as well).
          case a: JArray => shortenJArrayRecursively(maximumLengthSubPart, None, a)

          //All other values will relive the reduction process.
          case value => replaceEmptyArray(shortenJSONValueWorkflow(maximumLengthSubPart, Some(maximumLength), value).reducedValue)
        }
      )
    } else { //When the array is at the deepest level then a best effort reduction will be performed:
      //The array will be shortened (without a frame length) => reduction of primitives.
      val shortenedArray: JArray = JArray(array.arr.map(shortenJSONValueWorkflow(maximumLengthSubPart, None, _).getReducedValueAsJValue(Formats)))

      //If the shortening suffices, then the shortened version will be returned else the indices will be removed.
      if (determineSerializedLength(shortenedArray) > maximumLength) {
        shortenJArrayByIndicesDeletion(maximumLength, array)
      } else shortenedArray
    }
  }


  private[this] def shortenJArrayRecursively(maximumLengthSubPart: Int,
                                             maximumLength: Option[Int],
                                             array: JArray): JArray = {
    JArray(
      array.arr.map {
        case string: JString => shortenJSONStringWorkflow(maximumLengthSubPart, None, string).reducedValue
        case array: JArray => replaceEmptyArray(shortenJArray(maximumLengthSubPart, maximumLength, array))
        case obj: JObject => replaceEmptyObject(shortenJObject(maximumLengthSubPart, maximumLength, obj))
        case value => value
      }
    )
  }


  private[this] def shortenJObject(maximumLengthSubPart: Int,
                                   maximumLength: Option[Int],
                                   `object`: JObject): JObject = {
    JObject(
      maximumLength match {
        case None => shortenJObjectRecursively(maximumLengthSubPart, None, `object`).obj
        case Some(maxLength) =>
          val exceedsItself: Boolean = determineSerializedLength(`object`) > maxLength
          val hasDeeperExceedingValues: Boolean = hasDeeperLevelsWithExceedingLength(`object`, maxLength)

          val shortenedObject: List[JField] = (exceedsItself, hasDeeperExceedingValues) match {
            case (false, false) => shortenJObjectRecursively(maximumLengthSubPart, None, `object`).obj
            case (true, false) => shortenJObjectByKeysDeletion(maxLength, maximumLengthSubPart, `object`).obj
            case (_, true) => shortenDeeperJObject(maxLength, maximumLengthSubPart, `object`).obj
          }

          //2nd pass to check if the object still exceeds the limit.
          val shortenedObjectLength: Int = determineSerializedLength(JObject(shortenedObject))

          //If the limit is exceeded, then a delta length keys deletion will be performed.
          if (shortenedObjectLength > maxLength) {
            shortenJObjectByDeltaLengthKeysDeletion(maxLength, shortenedObjectLength, JObject(shortenedObject)).obj
          } else shortenedObject
      }
    )
  }


  private[this] def shortenJObjectAsJValue(maximumLengthSubPart: Int,
                                           maximumLength: Option[Int],
                                           `object`: JObject): JValue = {
    //The object will be shortened and if the result is a JSON-Object where all fields are JNothing, then the empty object will be deleted as well.
    replaceEmptyObject(shortenJObjectRecursively(maximumLengthSubPart, maximumLength, `object`))
  }


  private[this] def shortenJObjectByDeltaLengthKeysDeletion(maximumLength: Int,
                                                            shortenedArrayLength: Int,
                                                            `object`: JObject): JObject = {
    val deltaLength: Int = shortenedArrayLength - maximumLength

    `object`.transform {
      //The highest level will be tried to shortened by deleting the keys (this occurs, when the deletion of the lowest level does not suffice.
      case o: JObject if isDeeper(o) && determineSerializedLength(o) > maximumLength => shortenJObjectByDeltaLengthKeysDeletion(deltaLength, o)

      //The lowest level will be tried to shortened by deleting the keys.
      case o: JObject => shortenJObjectByDeltaLengthKeysDeletion(deltaLength, o)


      //Every other case will be returned.
      case v => v
    } match {
      case o: JObject => JObject(
        o.obj.map {
          case JField(n, obj: JObject) => JField(n, replaceEmptyObject(obj))
          case JField(n, v) => JField(n, v)
        }
      )
      case _ => JObject(List.empty[JField])
    }
  }


  private[this] def shortenJObjectByKeysDeletion(maximumLength: Int,
                                                 maximumLengthSubPart: Int,
                                                 `object`: JObject): JObject = {

    //If the object contains deeper levels, then a best effort reduction will be performed:
    if (isDeeper(`object`)) {
      JObject(
        `object`.obj.map {
          //The value is an object and has more levels.
          case JField(name, o: JObject) if isDeeper(o) =>

            //The serialized length exceeds the limit => dive into deeper level an recursive reduction attempt.
            if (determineSerializedLength(o) > maximumLength) {
              JField(name, shortenJObjectAsJValue(maximumLengthSubPart, Some(maximumLength), o))
            } else { //The serialized length keeps the limit => value will be used as is (but primitives will be shortened as well).
              JField(name, shortenJObjectAsJValue(maximumLengthSubPart, None, o))
            }

          //The object is at the deepest level but exceeds the limit => reduction attempt of the object:
          case JField(name, o: JObject) if determineSerializedLength(o) > maximumLength =>

            //The object will be shortened and if the result is a JSON-Object where all fields are JNothing, then the empty object will be deleted as well.
            val shortenedValue: JValue = shortenJObjectAsJValue(maximumLengthSubPart, None, o)

            //If the shortening suffices, then the shortened version will be returned else JNothing.
            if (determineSerializedLength(shortenedValue) > maximumLength) JField(name, JNothing) else JField(name, shortenedValue)

          //The object is at the deepest level and keeps the limit => value will be used as is (but primitives will be shortened as well).
          case JField(name, o: JObject) => JField(name, shortenJObjectAsJValue(maximumLengthSubPart, None, o))

          //All other values will relive the reduction process.
          case JField(name, value) => JField(name, replaceEmptyObject(shortenJSONValueWorkflow(maximumLengthSubPart, Some(maximumLength), value).getReducedValueAsJValue(Formats)))
        }
      )
    } else { //When the object is at the deepest level then a best effort reduction will be performed:
      //The object will be shortened (without a frame length) => reduction of primitives.
      val shortenedObject: JObject =
      JObject(
        `object`.obj.map {
          case JField(name, v) => JField(name, replaceEmptyObject(shortenJSONValueWorkflow(maximumLengthSubPart, None, v).getReducedValueAsJValue(Formats)))
        }
      )

      //If the shortening suffices, then the shortened version will be returned else the keys will be removed.
      if (determineSerializedLength(shortenedObject) > maximumLength) {
        shortenJObjectByKeysDeletion(maximumLength, `object`)
      } else shortenedObject
    }
  }


  private[this] def shortenJObjectRecursively(maximumLengthSubPart: Int,
                                              maximumLength: Option[Int],
                                              `object`: JObject): JObject = {
    JObject(
      `object`.obj.map {
        case JField(name, string: JString) => JField(name, shortenJSONStringWorkflow(maximumLengthSubPart, None, string).reducedValue)
        case JField(name, array: JArray) => JField(name, replaceEmptyArray(shortenJArrayRecursively(maximumLengthSubPart, maximumLength, array)))
        case JField(name, obj: JObject) => JField(name, replaceEmptyObject(shortenJObject(maximumLengthSubPart, maximumLength, obj)))
        case JField(name, value) => JField(name, value)
      }
    )
  }
}