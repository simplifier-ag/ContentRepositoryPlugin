package io.simplifier.plugin.contentrepo.pluginBaseRelated.reducer.sizeReducer

import io.simplifier.pluginbase.util.json.NamedTupleAccess._
import io.simplifier.plugin.contentrepo.pluginBaseRelated.reducer.ReducibleJSONDataTypes._
import io.simplifier.plugin.contentrepo.pluginBaseRelated.reducer._
import io.simplifier.plugin.contentrepo.pluginBaseRelated.reducer.reductionChanges.{ReductionChange, RemovedElement}
import org.json4s._


/**
  * This trait provides all functions to calculate [[ReductionChanges]].
  */
protected[sizeReducer] trait SizeReducerReductionChangeDeterminer extends SizeReducerUtils {


  /**
    * Determines the [[ReductionChanges]] for the provided Json-Array
    *
    * @param reducedArrayValues    the values of the already reduced Json-Array.
    * @param arrayValues           the values of the original Json-Array.
    *
    * @return                      the respective calculated [[ReductionChanges]].
    */
  protected[sizeReducer] def determineArrayChanges(reducedArrayValues: Seq[JValue],
                                                   arrayValues: Seq[JValue]): ReductionChanges = {
    val changes: Seq[ReductionChange] = if (isByteArrayEquivalent(arrayValues)) {
      Seq(
        ReductionChange(
          `type` = JsonByteArrayEquivalentReducibleType(),
          originalSize = arrayValues.size,
          reducedSize = reducedArrayValues.size
        )
      )
    } else {
      arrayValues
        .zipWithIndex
        .collect {
          case (value, index) if determineIfArrayChangeOccurred(index, reducedArrayValues, value) =>
            determineChange(Some(index), None, value, reducedArrayValues(index))
        }
    }


    ReductionChanges(
      changes = changes
    )
  }


  /**
    * Determines the [[ReductionChanges]] for the provided Json-Object
    *
    * @param reducedFields    the fields of the already reduced Json-Object.
    * @param fields           the fields of the original Json-Object.
    *
    * @return                 the respective calculated [[ReductionChanges]].
    */
  protected[sizeReducer] def determineObjectChanges(reducedFields: Seq[JField],
                                                    fields: Seq[JField]): ReductionChanges = {
    ReductionChanges(
      changes = fields
        .collect {
          case JField(name, value) if determineIfObjectChangeOccurred(name, reducedFields, value) =>
            val reducedValue: JValue = getJsonFieldValueByNameAndOriginalValue(name, reducedFields, value)

            determineChange(None, Some(name), value, reducedValue)
        }
    )
  }


  private[this] def determineChange(index: Option[Int],
                                    key: Option[String],
                                    value: JValue,
                                    reducedValue: JValue): ReductionChange = {
    val reducedSize: Int = determineReducedJSONLength(reducedValue)

    ReductionChange(
      index = value match {
        case JArray(v) if !isByteArrayEquivalent(v) && reducedSize == DEFAULT_SIZE => index
        case _ if reducedValue == JNothing => index
        case _ => if (reducedSize == DEFAULT_SIZE) None else index
      },
      key = key,
      `type` = determineJsonType(value),
      originalSize = determineJSONLength(value),
      reducedSize = reducedSize,
      removedElement = determineRemovedElements(value, reducedValue),
      changes = value match {
        case JArray(v) if v.isEmpty => None
        case JArray(v) => reducedValue match {
          case JArray(rv) => Some(determineArrayChanges(rv, v))
          case _ => None
        }
        case JObject(f) if f.isEmpty => None
        case JObject(f) => reducedValue match {
          case JObject(rf) => Some(determineObjectChanges(rf, f))
          case _ => None
        }
        case _ => None
      }
    )
  }


  private[this] def determineIfArrayChangeOccurred(index: Int,
                                                   reducedArrayValues: Seq[JValue],
                                                   value: JValue): Boolean = {
    (value, reducedArrayValues(index)) match {
      case (JString(_) | JArray(_) | JObject(_), JNothing) => true
      case (JString(v), JString(rv)) => determineSerializedLength(JString(v)) != determineSerializedLength(JString(rv))
      case (JArray(v), JArray(rv)) => determineSerializedLength(JArray(v)) != determineSerializedLength(JArray(rv))
      case (JObject(f), JObject(rf)) => determineSerializedLength(JObject(f)) != determineSerializedLength(JObject(rf))
      case _ => false
    }
  }


  private[this] def determineIfObjectChangeOccurred(name: String,
                                                    reducedFields: Seq[JField],
                                                    value: JValue): Boolean = {
    getJsonFieldValueByNameAndOriginalValue(name, reducedFields, value) match {
      case JNothing if value != JNothing => true
      case JNothing | JNull | JBool(_) | JInt(_) | JDouble(_) => false
      case v => determineSerializedLength(v) != determineSerializedLength(value)
    }
  }


  private[this] def determineReducedJSONLength(value: JValue): Int = {
    value match {
      case JArray(values) if isByteArrayEquivalent(values) => values.length
      case JArray(_) | JObject(_) => determineSerializedLength(value)
      case JString(_) => determineSerializedLength(value) - 2 //Removing the " symbols from the count.
      case JNothing => 0 //Removed Elements will be treated as 0.
      case _ => DEFAULT_SIZE
    }
  }


  private[this] def determineRemovedElements(value: JValue,
                                             reducedValue: JValue): Option[RemovedElement] = {
    (value, reducedValue) match {
      case (JString(_), JNothing) => Some(RemovedElement())
      case (JArray(v), JNothing) if isByteArrayEquivalent(v) => Some(RemovedElement())
      case (JArray(v), JNothing) => Some(RemovedElement(v.indices.toList))
      case (JArray(v), JArray(_)) if isByteArrayEquivalent(v) => None
      case (JArray(v), JArray(rv)) => v
        .zipWithIndex
        .collect {
          case (av, index) if av != JNothing && rv(index) == JNothing => index
        } match {
        case indices if indices.isEmpty => None
        case indices => Some(RemovedElement(indices))
      }
      case (JObject(f), JNothing) => Some(RemovedElement(f.map(_.name)))
      case (JObject(f), JObject(rf)) => f
        .zipWithIndex
        .collect {
          case (JField(name, fv), index) if fv != JNothing && rf(index).value == JNothing => name
        } match {
        case keys if keys.isEmpty => None
        case keys => Some(RemovedElement(keys))
      }
      case _ => None
    }
  }

  private[this] def getJsonFieldValueByNameAndOriginalValue(name: String,
                                                            fields: Seq[JField],
                                                            value: JValue): JValue = {
    fields.collectFirst {
      case JField(n, v) if determineJsonType(v) == determineJsonType(value) && n == name => v
    }.getOrElse(JNothing)
  }
}