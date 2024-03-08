package io.simplifier.plugin.contentrepo.pluginBaseRelated.reducer

import akka.util.ByteString
import io.simplifier.plugin.contentrepo.pluginBaseRelated.reducer.ReducibleJSONDataTypes.{JsonArrayReducibleType, JsonStringReducibleType}
import io.simplifier.plugin.contentrepo.pluginBaseRelated.reducer.ReductionChanges.{FIELD_NAME_CHANGED_AMOUNT, FIELD_NAME_CHANGED_ELEMENTS}
import io.simplifier.plugin.contentrepo.pluginBaseRelated.reducer.ReductionResult.{FIELD_NAME_CHANGES, FIELD_NAME_ORIGINAL_SIZE, FIELD_NAME_ORIGINAL_VALUE, FIELD_NAME_REDUCED_SIZE, FIELD_NAME_REDUCED_VALUE, FIELD_NAME_TYPE, FIELD_NAME_WAS_REDUCED}
import io.simplifier.plugin.contentrepo.pluginBaseRelated.reducer.reductionChanges.ReductionChange
import io.simplifier.plugin.contentrepo.pluginBaseRelated.reducer.reductionChanges.ReductionChange.{FIELD_NAME_INDEX, FIELD_NAME_KEY, FIELD_NAME_REMOVED_ELEMENT}
import io.simplifier.plugin.contentrepo.pluginBaseRelated.reducer.reductionResults.OriginalResult.OriginalResultChangesSerializer
import io.simplifier.plugin.contentrepo.pluginBaseRelated.reducer.reductionResults.ReductionResultJArray.FIELD_NAME_ARRAY_CHANGES
import io.simplifier.plugin.contentrepo.pluginBaseRelated.reducer.reductionResults.ReductionResultJObject.FIELD_NAME_OBJECT_CHANGES
import io.simplifier.plugin.contentrepo.pluginBaseRelated.reducer.reductionResults.{OriginalResult, OriginalResultJValue, ReductionResultByteString, ReductionResultJArray, ReductionResultJObject, ReductionResultJString, ReductionResultString}
import io.simplifier.pluginbase.util.json.SimplifierFormats.ByteStringSerializer
import org.json4s.{JValue, _}
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec

import scala.reflect.runtime.universe._

class ReductionResultTest extends AnyWordSpec with Matchers {


  "A Reduction Result Case class" when {
    "returning the reduced JSON representation" should {
      "yield the correct JSON representation without omitting the reduced value" in new JsonReducedRepresentation {
        originalResult.toReducedJson(omitValues = false, None) mustBe originalResultJsonReduced
        byteStringResult.toReducedJson(omitValues = false, None) mustBe byteStringResultJson
        originalResultJValue.toReducedJson(omitValues = false, None) mustBe originalResultJValueJsonReduced
        stringResult.toReducedJson(omitValues = false, None) mustBe stringResultJson
        jStringResult.toReducedJson(omitValues = false, None) mustBe jStringResultJson
        jArrayResult.toReducedJson(omitReducedValue = false, None) mustBe jArrayResultJson
        jObjectResult.toReducedJson(omitReducedValue = false, None) mustBe jObjectResultJsonReduced
      }

      "yield the correct JSON representation with omitting the reduced value" in new JsonReducedOmittedRepresentation {
        originalResult.toReducedJson(omitValues = true, None) mustBe originalResultJsonOmitted
        byteStringResult.toReducedJson(omitValues = true, None) mustBe byteStringResultJsonOmitted
        originalResultJValue.toReducedJson(omitValues = true, None) mustBe originalResultJValueJsonOmitted
        stringResult.toReducedJson(omitValues = true, None) mustBe stringResultJsonOmitted
        jStringResult.toReducedJson(omitValues = true, None) mustBe jStringResultJsonOmitted
        jArrayResult.toReducedJson(omitReducedValue = true, None) mustBe jArrayResultJsonOmitted
        jObjectResult.toReducedJson(omitReducedValue = true, None) mustBe jObjectResultJsonOmitted
      }
    }

    "returning the JSON representation" should {
      "yield the correct JSON representation" in new JsonRepresentation {
        originalResult.toJson(None) mustBe originalResultJson
        byteStringResult.toJson(None) mustBe byteStringResultJson
        originalResultJValue.toJson(None) mustBe originalResultJValueJson
        stringResult.toJson(None) mustBe stringResultJson
        jStringResult.toJson(None) mustBe jStringResultJson
        jArrayResult.toJson(None) mustBe jArrayResultJson
        jObjectResult.toJson(None) mustBe jObjectResultJson
      }
    }

    "reducing the String representation" should {
      "yield the correct String representation" in new StringRepresentation {
        originalResult.toString mustBe originalResultString
        originalResult2.toString mustBe originalResultString2
        byteStringResult.toString mustBe byteStringResultString
        originalResultJValue.toString mustBe originalResultJValueString
        originalResultJValue2.toString mustBe originalResultJValueString2
        stringResult.toString mustBe stringResultString
        jStringResult.toString mustBe jStringResultString
        jArrayResult.toString mustBe jArrayResultString
        jArrayResult2.toString mustBe jArrayResultString2
        jObjectResult.toString mustBe jObjectResultString
      }
    }

    "getting the original value as a JSON" should {
      "yield the correct JSON" in new JsonValue with JsonReducedOmittedRepresentation {
        originalResult.getOriginalValueAsJValue mustBe originalResultValue
        byteStringResult.getOriginalValueAsJValue mustBe byteStringResultValueOriginal
        originalResultJValue.getOriginalValueAsJValue mustBe originalResultJValueValue
        stringResult.getOriginalValueAsJValue mustBe stringResultValueOriginal
        jStringResult.getOriginalValueAsJValue mustBe jStringResultValueOriginal
        jArrayResult.getOriginalValueAsJValue mustBe jArrayResultValueOriginal
        jObjectResult.getOriginalValueAsJValue mustBe jObjectResultValueOriginal

        originalResultJValueOmittedReconstructed.getOriginalValueAsJValue mustBe defaultValue
        byteStringResultOmittedReconstructed.getOriginalValueAsJValue mustBe defaultValue
        originalResultJValueOmittedReconstructed.getOriginalValueAsJValue mustBe defaultValue
        stringResultOmittedReconstructed.getOriginalValueAsJValue mustBe defaultValue
        jStringResultOmittedReconstructed.getOriginalValueAsJValue mustBe defaultValue
        jArrayResultOmittedReconstructed.getOriginalValueAsJValue mustBe defaultValue
        jObjectResultOmittedReconstructed.getOriginalValueAsJValue mustBe defaultValue
      }
    }

    "getting the reduced value as a JSON" should {
      "yield the correct JSON" in new JsonValue with JsonReducedOmittedRepresentation {
        originalResult.getReducedValueAsJValue mustBe originalResultValue
        byteStringResult.getReducedValueAsJValue mustBe byteStringResultValue
        originalResultJValue.getReducedValueAsJValue mustBe originalResultJValueValue
        stringResult.getReducedValueAsJValue mustBe stringResultValue
        jStringResult.getReducedValueAsJValue mustBe jStringResultValue
        jArrayResult.getReducedValueAsJValue mustBe jArrayResultValue
        jObjectResult.getReducedValueAsJValue mustBe jObjectResultValue

        originalResultJValueOmittedReconstructed.getReducedValueAsJValue mustBe defaultValue
        byteStringResultOmittedReconstructed.getReducedValueAsJValue mustBe defaultValue
        originalResultJValueOmittedReconstructed.getReducedValueAsJValue mustBe defaultValue
        stringResultOmittedReconstructed.getReducedValueAsJValue mustBe defaultValue
        jStringResultOmittedReconstructed.getReducedValueAsJValue mustBe defaultValue
        jArrayResultOmittedReconstructed.getReducedValueAsJValue mustBe defaultValue
        jObjectResultOmittedReconstructed.getReducedValueAsJValue mustBe defaultValue
      }
    }


    "serializing a Case Class with the reduced serializer" should {
      "yield the correct JSON" in new Serializer with JsonReducedRepresentation {
        Extraction.decompose(originalResult)(formatsWithSmallFloatSerializer) mustBe originalResultJsonReduced
        Extraction.decompose(byteStringResult)(formatsWithSmallFloatSerializer) mustBe byteStringResultJson
        Extraction.decompose(originalResultJValue)(formatsWithSmallFloatSerializer) mustBe originalResultJValueJsonReduced
        Extraction.decompose(stringResult)(formatsWithSmallFloatSerializer) mustBe stringResultJson
        Extraction.decompose(jStringResult)(formatsWithSmallFloatSerializer) mustBe jStringResultJson
        Extraction.decompose(jArrayResult)(formatsWithSmallFloatSerializer) mustBe jArrayResultJson
        Extraction.decompose(jObjectResult)(formatsWithSmallFloatSerializer) mustBe jObjectResultJsonReduced
      }
    }

    "serializing a Case Class with the reduced omitting serializer" should {
      "yield the correct JSON" in new Serializer with JsonReducedOmittedRepresentation {
        Extraction.decompose(originalResult)(formatsWithSmallOmittingSerializer) mustBe originalResultJsonOmitted
        Extraction.decompose(byteStringResult)(formatsWithSmallOmittingSerializer) mustBe byteStringResultJsonOmitted
        Extraction.decompose(originalResultJValue)(formatsWithSmallOmittingSerializer) mustBe originalResultJValueJsonOmitted
        Extraction.decompose(stringResult)(formatsWithSmallOmittingSerializer) mustBe stringResultJsonOmitted
        Extraction.decompose(jStringResult)(formatsWithSmallOmittingSerializer) mustBe jStringResultJsonOmitted
        Extraction.decompose(jArrayResult)(formatsWithSmallOmittingSerializer) mustBe jArrayResultJsonOmitted
        Extraction.decompose(jObjectResult)(formatsWithSmallOmittingSerializer) mustBe jObjectResultJsonOmitted
      }
    }


    "serializing a Case Class with the full serializer" should {
      "yield the correct JSON" in new Serializer with JsonRepresentation {
        Extraction.decompose(originalResult)(formatsWithFullSerializer) mustBe originalResultJson
        Extraction.decompose(byteStringResult)(formatsWithFullSerializer) mustBe byteStringResultJson
        Extraction.decompose(originalResultJValue)(formatsWithFullSerializer) mustBe originalResultJValueJson
        Extraction.decompose(stringResult)(formatsWithFullSerializer) mustBe stringResultJson
        Extraction.decompose(jStringResult)(formatsWithFullSerializer) mustBe jStringResultJson
        Extraction.decompose(jArrayResult)(formatsWithFullSerializer) mustBe jArrayResultJson
        Extraction.decompose(jObjectResult)(formatsWithFullSerializer) mustBe jObjectResultJson
      }
    }

    "deserializing a JSON with the small serializer" should {
      "yield the correct Case Class" in new Serializer with JsonReducedRepresentation {
        override implicit val formats: Formats = formatsWithSmallFloatSerializer

        Extraction.extract[ReductionResult[Float]](originalResultJson) mustBe originalResult
        Extraction.extract[ReductionResult[ByteString]](byteStringResultJson)(formatsWithSmallByteStringSerializer, manifest[ReductionResult[ByteString]]) mustBe byteStringResultReconstructed
        Extraction.extract[ReductionResult[JDouble]](originalResultJValueJson) mustBe originalResultJValue
        Extraction.extract[ReductionResult[String]](stringResultJson) mustBe stringResultReconstructed
        Extraction.extract[ReductionResult[JString]](jStringResultJson) mustBe jStringResultReconstructed
        Extraction.extract[ReductionResult[JArray]](jArrayResultJson) mustBe jArrayResultReconstructed
        Extraction.extract[ReductionResult[JObject]](jObjectResultJson) mustBe jObjectResultReconstructed


        Extraction.extract[ReductionResult[Float]](Extraction.decompose(originalResult)(formatsWithoutSerializer)) mustBe originalResult
        Extraction.extract[ReductionResult[ByteString]](Extraction.decompose(byteStringResultReconstructed)(formatsWithoutSerializer)) mustBe byteStringResultReconstructed

        Extraction.extract[ReductionResult[JDouble]](Extraction.decompose(originalResultJValue)(formatsWithoutSerializer)) mustBe originalResultJValue
        Extraction.extract[ReductionResult[String]](Extraction.decompose(stringResultReconstructed)(formatsWithoutSerializer)) mustBe stringResultReconstructed
        Extraction.extract[ReductionResult[JString]](Extraction.decompose(jStringResultReconstructed)(formatsWithoutSerializer)) mustBe jStringResultReconstructed
        Extraction.extract[ReductionResult[JArray]](Extraction.decompose(jArrayResultReconstructed)(formatsWithoutSerializer)) mustBe jArrayResultReconstructed
        Extraction.extract[ReductionResult[JObject]](Extraction.decompose(jObjectResultReconstructed)(formatsWithoutSerializer)) mustBe jObjectResultReconstructed
      }
    }


    "deserializing a JSON with the small omitting serializer" should {
      "yield the correct Case Class" in new Serializer with JsonReducedOmittedRepresentation with JsonReducedRepresentation {
        override implicit val formats: Formats = formatsWithSmallOmittingSerializer

        Extraction.extract[ReductionResult[Float]](originalResultJson) mustBe originalResult
        Extraction.extract[ReductionResult[ByteString]](byteStringResultJson) mustBe byteStringResultReconstructed
        Extraction.extract[ReductionResult[JDouble]](originalResultJValueJson) mustBe originalResultJValue
        Extraction.extract[ReductionResult[String]](stringResultJson) mustBe stringResultReconstructed
        Extraction.extract[ReductionResult[JString]](jStringResultJson) mustBe jStringResultReconstructed
        Extraction.extract[ReductionResult[JArray]](jArrayResultJson) mustBe jArrayResultReconstructed
        Extraction.extract[ReductionResult[JObject]](jObjectResultJson) mustBe jObjectResultReconstructed
        Extraction.extract[ReductionResult[Float]](Extraction.decompose(originalResult)(formatsWithoutSerializer)) mustBe originalResult

        Extraction.extract[ReductionResult[ByteString]](Extraction.decompose(byteStringResultReconstructed)(formatsWithoutSerializer)) mustBe byteStringResultReconstructed
        Extraction.extract[ReductionResult[JDouble]](Extraction.decompose(originalResultJValue)(formatsWithoutSerializer)) mustBe originalResultJValue
        Extraction.extract[ReductionResult[String]](Extraction.decompose(stringResultReconstructed)(formatsWithoutSerializer)) mustBe stringResultReconstructed
        Extraction.extract[ReductionResult[JString]](Extraction.decompose(jStringResultReconstructed)(formatsWithoutSerializer)) mustBe jStringResultReconstructed
        Extraction.extract[ReductionResult[JArray]](Extraction.decompose(jArrayResultReconstructed)(formatsWithoutSerializer)) mustBe jArrayResultReconstructed
        Extraction.extract[ReductionResult[JObject]](Extraction.decompose(jObjectResultReconstructed)(formatsWithoutSerializer)) mustBe jObjectResultReconstructed

        //The type changed to Option[_], because neither Null nor Nothing can be used to extract without any errors.
        Extraction.extract[ReductionResult[Option[_]]](Extraction.decompose(originalResultJsonOmitted)(formatsWithoutSerializer))(formatsWithSmallOmittingSerializerNull, manifest[ReductionResult[Option[_]]]) mustBe originalResultOmittedReconstructed
        Extraction.extract[ReductionResult[ByteString]](Extraction.decompose(byteStringResultJsonOmitted)(formatsWithoutSerializer)) mustBe byteStringResultOmittedReconstructed
        Extraction.extract[ReductionResult[JDouble]](Extraction.decompose(originalResultJValueJsonOmitted)(formatsWithoutSerializer)) mustBe originalResultJValueOmittedReconstructed
        Extraction.extract[ReductionResult[String]](Extraction.decompose(stringResultJsonOmitted)(formatsWithoutSerializer)) mustBe stringResultOmittedReconstructed
        Extraction.extract[ReductionResult[JString]](Extraction.decompose(jStringResultJsonOmitted)(formatsWithoutSerializer)) mustBe jStringResultOmittedReconstructed
        Extraction.extract[ReductionResult[JArray]](Extraction.decompose(jArrayResultJsonOmitted)(formatsWithoutSerializer)) mustBe jArrayResultOmittedReconstructed
        Extraction.extract[ReductionResult[JObject]](Extraction.decompose(jObjectResultJsonOmitted)(formatsWithoutSerializer)) mustBe jObjectResultOmittedReconstructed
      }
    }

    "deserializing a JSON with the full serializer" should {
      "yield the correct Case Class" in new Serializer with JsonRepresentation {
        override implicit val formats: Formats = formatsWithFullSerializer

        Extraction.extract[ReductionResult[Float]](originalResultJson) mustBe originalResult
        Extraction.extract[ReductionResult[ByteString]](byteStringResultJson) mustBe byteStringResult
        Extraction.extract[ReductionResult[JDouble]](originalResultJValueJson) mustBe originalResultJValue
        Extraction.extract[ReductionResult[String]](stringResultJson) mustBe stringResult
        Extraction.extract[ReductionResult[JString]](jStringResultJson) mustBe jStringResult
        Extraction.extract[ReductionResult[JArray]](jArrayResultJson) mustBe jArrayResult
        Extraction.extract[ReductionResult[JObject]](jObjectResultJson) mustBe jObjectResult

        Extraction.extract[ReductionResult[Float]](Extraction.decompose(originalResult)(formatsWithoutSerializer)) mustBe originalResult
        Extraction.extract[ReductionResult[ByteString]](Extraction.decompose(byteStringResult)(formatsWithoutSerializer)) mustBe byteStringResult
        Extraction.extract[ReductionResult[JDouble]](Extraction.decompose(originalResultJValue)(formatsWithoutSerializer)) mustBe originalResultJValue
        Extraction.extract[ReductionResult[String]](Extraction.decompose(stringResult)(formatsWithoutSerializer)) mustBe stringResult
        Extraction.extract[ReductionResult[JString]](Extraction.decompose(jStringResult)(formatsWithoutSerializer)) mustBe jStringResult
        Extraction.extract[ReductionResult[JArray]](Extraction.decompose(jArrayResult)(formatsWithoutSerializer)) mustBe jArrayResult
        Extraction.extract[ReductionResult[JObject]](Extraction.decompose(jObjectResult)(formatsWithoutSerializer)) mustBe jObjectResult
      }
    }
  }

  trait BaseFixture {
    val key: String = "key"

    val float: Float = 1337.42f
    val jValue: JDouble = JDouble(42.21)
    val byteString: ByteString = ByteString(1, 2, 3, 4, 5, 6, 7, 8, 9, 10)
    val byteStringReduced: ByteString = ByteString(1, 2, 3)
    val string: String = "I am a string"
    val stringReduced: String = "I am"
    val jString: JString = JString("I am a string")
    val jStringReduced: JString = JString("I am")
    val jArray: JArray = JArray(List(JInt(1), JInt(2), JInt(3)))
    val jArray2: JArray = JArray(List(JNull))
    val jArrayReduced: JArray = JArray(List(JInt(1)))
    val jArrayReduced2: JArray = JArray(List(JNull))
    val jObject: JObject = JObject(JField(key, jString))
    val jObjectReduced: JObject = JObject(JField(key, jStringReduced))

    val jArrayAsHex: JArray = JArray(List(JString("01"), JString("02"), JString("03"), JString("04"), JString("05"), JString("06"), JString("07"), JString("08"), JString("09"), JString("0A")))
    val jArrayAsHexReduced: JArray = JArray(List(JString("01"), JString("02"), JString("03")))
    val arrayChanges: ReductionChanges = ReductionChanges(
      Seq(
        ReductionChange(
          index = None,
          key = None,
          `type` = JsonArrayReducibleType(),
          originalSize = jArray.arr.size,
          reducedSize = jArrayReduced.arr.size,
          removedElement = None,
          changes = None
        )
      )
    )

    val arrayChanges2: ReductionChanges = ReductionChanges(
      Seq(
        ReductionChange(
          index = None,
          key = None,
          `type` = JsonArrayReducibleType(),
          originalSize = jArray2.arr.size,
          reducedSize = jArrayReduced2.arr.size,
          removedElement = None,
          changes = None
        )
      )
    )

    val objectChanges: ReductionChanges = ReductionChanges(
      Seq(
        ReductionChange(
          index = None,
          key = Some(key),
          `type` = JsonStringReducibleType(),
          originalSize = string.length,
          reducedSize = stringReduced.length,
          removedElement = None,
          changes = None
        )
      )
    )

    val originalResult: OriginalResult[Float] = OriginalResult[Float](float, -1)(DefaultFormats.lossless, TypeTag.Float)
    val originalResult2: OriginalResult[String] = OriginalResult[String](string, string.length)(DefaultFormats.lossless, typeTag[String])
    val byteStringResult: ReductionResultByteString = ReductionResultByteString(byteString, byteStringReduced)
    val byteStringResultReconstructed: ReductionResultByteString = ReductionResultByteString(null, byteStringReduced)
    val originalResultJValue: OriginalResultJValue[JDouble] = OriginalResultJValue[JDouble](jValue, -1)
    val originalResultJValue2: OriginalResultJValue[JString] = OriginalResultJValue[JString](jString, jString.s.length)
    val stringResult: ReductionResultString = ReductionResultString(string, stringReduced)
    val stringResultReconstructed: ReductionResultString = ReductionResultString(null, stringReduced)
    val jStringResult: ReductionResultJString = ReductionResultJString(jString, jStringReduced)
    val jStringResultReconstructed: ReductionResultJString = ReductionResultJString(null, jStringReduced)
    val jArrayResult: ReductionResultJArray = ReductionResultJArray(jArray, jArrayReduced, arrayChanges)
    val jArrayResultReconstructed: ReductionResultJArray = ReductionResultJArray(null, jArrayReduced, arrayChanges)
    val jArrayResult2: ReductionResultJArray = ReductionResultJArray(jArray2, jArrayReduced2, arrayChanges)
    val jObjectResult: ReductionResultJObject = ReductionResultJObject(jObject, jObjectReduced, objectChanges)
    val jObjectResultReconstructed: ReductionResultJObject = ReductionResultJObject(null, jObjectReduced, objectChanges)
  }


  trait JsonReducedRepresentation extends BaseFixture {
    val originalResultJson: JValue = JObject(
      JField(FIELD_NAME_TYPE, JString(originalResult.`type`)),
      JField(FIELD_NAME_WAS_REDUCED, JBool(false)),
      JField(FIELD_NAME_ORIGINAL_VALUE, JNothing),
      JField(FIELD_NAME_REDUCED_VALUE, JDouble(float)),
      JField(FIELD_NAME_ORIGINAL_SIZE, JNothing),
      JField(FIELD_NAME_REDUCED_SIZE, JNothing),
      JField(FIELD_NAME_CHANGES, JNothing)
    )

    val originalResultJsonReduced: JValue = JObject(
      JField(FIELD_NAME_TYPE, JString(originalResult.`type`)),
      JField(FIELD_NAME_WAS_REDUCED, JBool(false)),
      JField(FIELD_NAME_ORIGINAL_VALUE, JDouble(float)),
      JField(FIELD_NAME_REDUCED_VALUE, JNothing),
      JField(FIELD_NAME_ORIGINAL_SIZE, JNothing),
      JField(FIELD_NAME_REDUCED_SIZE, JNothing),
      JField(FIELD_NAME_CHANGES, JNothing)
    )

    val byteStringResultJson: JValue = JObject(
      JField(FIELD_NAME_TYPE, JString(byteStringResult.`type`)),
      JField(FIELD_NAME_WAS_REDUCED, JBool(true)),
      JField(FIELD_NAME_ORIGINAL_VALUE, JNothing),
      JField(FIELD_NAME_REDUCED_VALUE, jArrayAsHexReduced),
      JField(FIELD_NAME_ORIGINAL_SIZE, JString(s"${byteString.size} [Byte]")),
      JField(FIELD_NAME_REDUCED_SIZE, JString(s"${byteStringReduced.size} [Byte]")),
      JField(FIELD_NAME_CHANGES, JNothing)
    )


    val originalResultJValueJson: JValue = JObject(
      JField(FIELD_NAME_TYPE, JString(originalResultJValue.`type`)),
      JField(FIELD_NAME_WAS_REDUCED, JBool(false)),
      JField(FIELD_NAME_ORIGINAL_VALUE, JNothing),
      JField(FIELD_NAME_REDUCED_VALUE, jValue),
      JField(FIELD_NAME_ORIGINAL_SIZE, JNothing),
      JField(FIELD_NAME_REDUCED_SIZE, JNothing),
      JField(FIELD_NAME_CHANGES, JNothing)
    )

    val originalResultJValueJsonReduced: JValue = JObject(
      JField(FIELD_NAME_TYPE, JString(originalResultJValue.`type`)),
      JField(FIELD_NAME_WAS_REDUCED, JBool(false)),
      JField(FIELD_NAME_ORIGINAL_VALUE, jValue),
      JField(FIELD_NAME_REDUCED_VALUE, JNothing),
      JField(FIELD_NAME_ORIGINAL_SIZE, JNothing),
      JField(FIELD_NAME_REDUCED_SIZE, JNothing),
      JField(FIELD_NAME_CHANGES, JNothing)
    )


    val stringResultJson: JValue = JObject(
      JField(FIELD_NAME_TYPE, JString(stringResult.`type`)),
      JField(FIELD_NAME_WAS_REDUCED, JBool(true)),
      JField(FIELD_NAME_ORIGINAL_VALUE, JNothing),
      JField(FIELD_NAME_REDUCED_VALUE, jStringReduced),
      JField(FIELD_NAME_ORIGINAL_SIZE, JString(s"${string.length} [Byte]")),
      JField(FIELD_NAME_REDUCED_SIZE, JString(s"${stringReduced.length} [Byte]")),
      JField(FIELD_NAME_CHANGES, JNothing)
    )


    val jStringResultJson: JValue = JObject(
      JField(FIELD_NAME_TYPE, JString(jStringResult.`type`)),
      JField(FIELD_NAME_WAS_REDUCED, JBool(true)),
      JField(FIELD_NAME_ORIGINAL_VALUE, JNothing),
      JField(FIELD_NAME_REDUCED_VALUE, jStringReduced),
      JField(FIELD_NAME_ORIGINAL_SIZE, JString(s"${string.length} [Byte]")),
      JField(FIELD_NAME_REDUCED_SIZE, JString(s"${stringReduced.length} [Byte]")),
      JField(FIELD_NAME_CHANGES, JNothing)
    )

    val jStringResultJsonReduced: JValue = JObject(
      JField(FIELD_NAME_TYPE, JString(jStringResult.`type`)),
      JField(FIELD_NAME_WAS_REDUCED, JBool(true)),
      JField(FIELD_NAME_ORIGINAL_VALUE, JNothing),
      JField(FIELD_NAME_REDUCED_VALUE, jStringReduced),
      JField(FIELD_NAME_ORIGINAL_SIZE, JNothing),
      JField(FIELD_NAME_REDUCED_SIZE, JString(s"${stringReduced.length} [Byte]")),
      JField(FIELD_NAME_CHANGES, JNothing)
    )

    val jArrayResultJson: JValue = JObject(
      JField(FIELD_NAME_TYPE, JString(jArrayResult.`type`)),
      JField(FIELD_NAME_WAS_REDUCED, JBool(true)),
      JField(FIELD_NAME_ORIGINAL_VALUE, JNothing),
      JField(FIELD_NAME_REDUCED_VALUE, jArrayReduced),
      JField(FIELD_NAME_ORIGINAL_SIZE, JString(s"${jArray.arr.size} [Byte]")),
      JField(FIELD_NAME_REDUCED_SIZE, JString(s"${jArrayReduced.arr.size} [Byte]")),
      JField(FIELD_NAME_ARRAY_CHANGES, JObject(
        JField(FIELD_NAME_CHANGED_AMOUNT, JInt(1)),
        JField(FIELD_NAME_CHANGED_ELEMENTS, JArray(
          List(
            JObject(
              JField(FIELD_NAME_INDEX, JNothing),
              JField(FIELD_NAME_KEY, JNothing),
              JField(FIELD_NAME_TYPE, JsonArrayReducibleType().toJson),
              JField(FIELD_NAME_ORIGINAL_SIZE, JString(s"${jArray.arr.size} [Byte] (Compactly Rendered JSON-Array Size)")),
              JField(FIELD_NAME_REDUCED_SIZE, JString(s"${jArrayReduced.arr.size} [Byte] (Compactly Rendered JSON-Array Size)")),
              JField(FIELD_NAME_REMOVED_ELEMENT, JNothing),
              JField(FIELD_NAME_CHANGES, JNothing))
          )
        ))
      ))
    )


    val jObjectResultJson: JValue = JObject(
      JField(FIELD_NAME_TYPE, JString(jObjectResult.`type`)),
      JField(FIELD_NAME_WAS_REDUCED, JBool(true)),
      JField(FIELD_NAME_ORIGINAL_VALUE, JNothing),
      JField(FIELD_NAME_REDUCED_VALUE, jObjectReduced),
      JField(FIELD_NAME_ORIGINAL_SIZE, JNothing),
      JField(FIELD_NAME_REDUCED_SIZE, JNothing),
      JField(FIELD_NAME_OBJECT_CHANGES, JObject(
        JField(FIELD_NAME_CHANGED_AMOUNT, JInt(1)),
        JField(FIELD_NAME_CHANGED_ELEMENTS, JArray(
          List(
            JObject(
              JField(FIELD_NAME_INDEX, JNothing),
              JField(FIELD_NAME_KEY, JString(key)),
              JField(FIELD_NAME_TYPE, JsonStringReducibleType().toJson),
              JField(FIELD_NAME_ORIGINAL_SIZE, JInt(string.length)),
              JField(FIELD_NAME_REDUCED_SIZE, JInt(stringReduced.length)),
              JField(FIELD_NAME_REMOVED_ELEMENT, JNothing),
              JField(FIELD_NAME_CHANGES, JNothing))
          )
        ))
      ))
    )

    val jObjectResultJsonReduced: JValue = JObject(
      JField(FIELD_NAME_TYPE, JString(jObjectResult.`type`)),
      JField(FIELD_NAME_WAS_REDUCED, JBool(true)),
      JField(FIELD_NAME_ORIGINAL_VALUE, JNothing),
      JField(FIELD_NAME_REDUCED_VALUE, jObjectReduced),
      JField(FIELD_NAME_ORIGINAL_SIZE, JString("23 [Byte] (Compactly Rendered JSON-Object Size)")),
      JField(FIELD_NAME_REDUCED_SIZE, JString("14 [Byte] (Compactly Rendered JSON-Object Size)")),
      JField(FIELD_NAME_OBJECT_CHANGES, JObject(
        JField(FIELD_NAME_CHANGED_AMOUNT, JInt(1)),
        JField(FIELD_NAME_CHANGED_ELEMENTS, JArray(
          List(
            JObject(
              JField(FIELD_NAME_INDEX, JNothing),
              JField(FIELD_NAME_KEY, JString(key)),
              JField(FIELD_NAME_TYPE, JsonStringReducibleType().toJson),
              JField(FIELD_NAME_ORIGINAL_SIZE, JString(s"${string.length} [Byte]")),
              JField(FIELD_NAME_REDUCED_SIZE, JString(s"${stringReduced.length} [Byte]")),
              JField(FIELD_NAME_REMOVED_ELEMENT, JNothing),
              JField(FIELD_NAME_CHANGES, JNothing))
          )
        ))
      ))
    )
  }


  trait JsonReducedOmittedRepresentation extends BaseFixture {
    val originalResultJsonOmitted: JValue = JObject(
      JField(FIELD_NAME_TYPE, JString(originalResult.`type`)),
      JField(FIELD_NAME_WAS_REDUCED, JBool(false)),
      JField(FIELD_NAME_ORIGINAL_VALUE, JNothing),
      JField(FIELD_NAME_REDUCED_VALUE, JNothing),
      JField(FIELD_NAME_ORIGINAL_SIZE, JNothing),
      JField(FIELD_NAME_REDUCED_SIZE, JNothing),
      JField(FIELD_NAME_CHANGES, JNothing)
    )


    val jStringResultJsonOmitted: JValue = JObject(
      JField(FIELD_NAME_TYPE, JString(jStringResult.`type`)),
      JField(FIELD_NAME_WAS_REDUCED, JBool(true)),
      JField(FIELD_NAME_ORIGINAL_VALUE, JNothing),
      JField(FIELD_NAME_REDUCED_VALUE, JNothing),
      JField(FIELD_NAME_ORIGINAL_SIZE, JString(s"${string.length} [Byte]")),
      JField(FIELD_NAME_REDUCED_SIZE, JString(s"${stringReduced.length} [Byte]")),
      JField(FIELD_NAME_CHANGES, JNothing)
    )

    val stringResultJsonOmitted: JValue = JObject(
      JField(FIELD_NAME_TYPE, JString(stringResult.`type`)),
      JField(FIELD_NAME_WAS_REDUCED, JBool(true)),
      JField(FIELD_NAME_ORIGINAL_VALUE, JNothing),
      JField(FIELD_NAME_REDUCED_VALUE, JNothing),
      JField(FIELD_NAME_ORIGINAL_SIZE, JString(s"${string.length} [Byte]")),
      JField(FIELD_NAME_REDUCED_SIZE, JString(s"${stringReduced.length} [Byte]")),
      JField(FIELD_NAME_CHANGES, JNothing)
    )

    val byteStringResultJsonOmitted: JValue = JObject(
      JField(FIELD_NAME_TYPE, JString(byteStringResult.`type`)),
      JField(FIELD_NAME_WAS_REDUCED, JBool(true)),
      JField(FIELD_NAME_ORIGINAL_VALUE, JNothing),
      JField(FIELD_NAME_REDUCED_VALUE, JNothing),
      JField(FIELD_NAME_ORIGINAL_SIZE, JString(s"${byteString.size} [Byte]")),
      JField(FIELD_NAME_REDUCED_SIZE, JString(s"${byteStringReduced.size} [Byte]")),
      JField(FIELD_NAME_CHANGES, JNothing)
    )


    val originalResultJValueJsonOmitted: JValue = JObject(
      JField(FIELD_NAME_TYPE, JString(originalResultJValue.`type`)),
      JField(FIELD_NAME_WAS_REDUCED, JBool(false)),
      JField(FIELD_NAME_ORIGINAL_VALUE, JNothing),
      JField(FIELD_NAME_REDUCED_VALUE, JNothing),
      JField(FIELD_NAME_ORIGINAL_SIZE, JNothing),
      JField(FIELD_NAME_REDUCED_SIZE, JNothing),
      JField(FIELD_NAME_CHANGES, JNothing)
    )


    val jArrayResultJsonOmitted: JValue = JObject(
      JField(FIELD_NAME_TYPE, JString(jArrayResult.`type`)),
      JField(FIELD_NAME_WAS_REDUCED, JBool(true)),
      JField(FIELD_NAME_ORIGINAL_VALUE, JNothing),
      JField(FIELD_NAME_REDUCED_VALUE, JNothing),
      JField(FIELD_NAME_ORIGINAL_SIZE, JString(s"${jArray.arr.size} [Byte]")),
      JField(FIELD_NAME_REDUCED_SIZE, JString(s"${jArrayReduced.arr.size} [Byte]")),
      JField(FIELD_NAME_ARRAY_CHANGES, JObject(
        JField(FIELD_NAME_CHANGED_AMOUNT, JInt(1)),
        JField(FIELD_NAME_CHANGED_ELEMENTS, JArray(
          List(
            JObject(
              JField(FIELD_NAME_INDEX, JNothing),
              JField(FIELD_NAME_KEY, JNothing),
              JField(FIELD_NAME_TYPE, JsonArrayReducibleType().toJson),
              JField(FIELD_NAME_ORIGINAL_SIZE, JString(s"${jArray.arr.size} [Byte] (Compactly Rendered JSON-Array Size)")),
              JField(FIELD_NAME_REDUCED_SIZE, JString(s"${jArrayReduced.arr.size} [Byte] (Compactly Rendered JSON-Array Size)")),
              JField(FIELD_NAME_REMOVED_ELEMENT, JNothing),
              JField(FIELD_NAME_CHANGES, JNothing))
          )
        ))
      ))
    )

    val jObjectResultJsonOmitted: JValue = JObject(
      JField(FIELD_NAME_TYPE, JString(jObjectResult.`type`)),
      JField(FIELD_NAME_WAS_REDUCED, JBool(true)),
      JField(FIELD_NAME_ORIGINAL_VALUE, JNothing),
      JField(FIELD_NAME_REDUCED_VALUE, JNothing),
      JField(FIELD_NAME_ORIGINAL_SIZE, JString("23 [Byte] (Compactly Rendered JSON-Object Size)")),
      JField(FIELD_NAME_REDUCED_SIZE, JString("14 [Byte] (Compactly Rendered JSON-Object Size)")),
      JField(FIELD_NAME_OBJECT_CHANGES, JObject(
        JField(FIELD_NAME_CHANGED_AMOUNT, JInt(1)),
        JField(FIELD_NAME_CHANGED_ELEMENTS, JArray(
          List(
            JObject(
              JField(FIELD_NAME_INDEX, JNothing),
              JField(FIELD_NAME_KEY, JString(key)),
              JField(FIELD_NAME_TYPE, JsonStringReducibleType().toJson),
              JField(FIELD_NAME_ORIGINAL_SIZE, JString(s"${string.length} [Byte]")),
              JField(FIELD_NAME_REDUCED_SIZE, JString(s"${stringReduced.length} [Byte]")),
              JField(FIELD_NAME_REMOVED_ELEMENT, JNothing),
              JField(FIELD_NAME_CHANGES, JNothing))
          )
        ))
      ))
    )


    val originalResultOmittedReconstructed: OriginalResult[Option[_]] = OriginalResult[Option[_]](None, -1)(DefaultFormats.lossless, typeTag[Option[_]])
    val byteStringResultOmittedReconstructed: ReductionResultByteString = ReductionResultByteString(null, null)
    val originalResultJValueOmittedReconstructed: OriginalResultJValue[JValue] = OriginalResultJValue[JValue](JNothing, -1)
    val stringResultOmittedReconstructed: ReductionResultString = ReductionResultString(null, null)
    val jStringResultOmittedReconstructed: ReductionResultJString = ReductionResultJString(null, null)
    val jArrayResultOmittedReconstructed: ReductionResultJArray = ReductionResultJArray(null, null, arrayChanges)
    val jObjectResultOmittedReconstructed: ReductionResultJObject = ReductionResultJObject(null, null, objectChanges)


  }


  trait JsonRepresentation extends BaseFixture {
    val originalResultJson: JValue = JObject(
      JField(FIELD_NAME_TYPE, JString(originalResult.`type`)),
      JField(FIELD_NAME_WAS_REDUCED, JBool(false)),
      JField(FIELD_NAME_ORIGINAL_VALUE, JDouble(float)),
      JField(FIELD_NAME_REDUCED_VALUE, JDouble(float)),
      JField(FIELD_NAME_ORIGINAL_SIZE, JNothing),
      JField(FIELD_NAME_REDUCED_SIZE, JNothing),

      JField(FIELD_NAME_CHANGES, JNothing)
    )

    val byteStringResultJson: JValue = JObject(
      JField(FIELD_NAME_TYPE, JString(byteStringResult.`type`)),
      JField(FIELD_NAME_WAS_REDUCED, JBool(true)),
      JField(FIELD_NAME_ORIGINAL_VALUE, jArrayAsHex),
      JField(FIELD_NAME_REDUCED_VALUE, jArrayAsHexReduced),
      JField(FIELD_NAME_ORIGINAL_SIZE, JString(s"${byteString.size} [Byte]")),
      JField(FIELD_NAME_REDUCED_SIZE, JString(s"${byteStringReduced.size} [Byte]")),
      JField(FIELD_NAME_CHANGES, JNothing)
    )

    val originalResultJValueJson: JValue = JObject(
      JField(FIELD_NAME_TYPE, JString(originalResultJValue.`type`)),
      JField(FIELD_NAME_WAS_REDUCED, JBool(false)),
      JField(FIELD_NAME_ORIGINAL_VALUE, jValue),
      JField(FIELD_NAME_REDUCED_VALUE, jValue),
      JField(FIELD_NAME_ORIGINAL_SIZE, JNothing),
      JField(FIELD_NAME_REDUCED_SIZE, JNothing),
      JField(FIELD_NAME_CHANGES, JNothing)
    )

    val stringResultJson: JValue = JObject(
      JField(FIELD_NAME_TYPE, JString(stringResult.`type`)),
      JField(FIELD_NAME_WAS_REDUCED, JBool(true)),
      JField(FIELD_NAME_ORIGINAL_VALUE, jString),
      JField(FIELD_NAME_REDUCED_VALUE, jStringReduced),
      JField(FIELD_NAME_ORIGINAL_SIZE, JString(s"${string.length} [Byte]")),
      JField(FIELD_NAME_REDUCED_SIZE, JString(s"${stringReduced.length} [Byte]")),
      JField(FIELD_NAME_CHANGES, JNothing)
    )

    val jStringResultJson: JValue = JObject(
      JField(FIELD_NAME_TYPE, JString(jStringResult.`type`)),
      JField(FIELD_NAME_WAS_REDUCED, JBool(true)),
      JField(FIELD_NAME_ORIGINAL_VALUE, jString),
      JField(FIELD_NAME_REDUCED_VALUE, jStringReduced),
      JField(FIELD_NAME_ORIGINAL_SIZE, JString(s"${string.length} [Byte]")),
      JField(FIELD_NAME_REDUCED_SIZE, JString(s"${stringReduced.length} [Byte]")),
      JField(FIELD_NAME_CHANGES, JNothing)
    )

    val jArrayResultJson: JValue = JObject(
      JField(FIELD_NAME_TYPE, JString(jArrayResult.`type`)),
      JField(FIELD_NAME_WAS_REDUCED, JBool(true)),
      JField(FIELD_NAME_ORIGINAL_VALUE, jArray),
      JField(FIELD_NAME_REDUCED_VALUE, jArrayReduced),
      JField(FIELD_NAME_ORIGINAL_SIZE, JString(s"${jArray.arr.size} [Byte]")),
      JField(FIELD_NAME_REDUCED_SIZE, JString(s"${jArrayReduced.arr.size} [Byte]")),
      JField(FIELD_NAME_ARRAY_CHANGES, JObject(
        JField(FIELD_NAME_CHANGED_AMOUNT, JInt(1)),
        JField(FIELD_NAME_CHANGED_ELEMENTS, JArray(
          List(
            JObject(
              JField(FIELD_NAME_INDEX, JNothing),
              JField(FIELD_NAME_KEY, JNothing),
              JField(FIELD_NAME_TYPE, JsonArrayReducibleType().toJson),
              JField(FIELD_NAME_ORIGINAL_SIZE, JString(s"${jArray.arr.size} [Byte] (Compactly Rendered JSON-Array Size)")),
              JField(FIELD_NAME_REDUCED_SIZE, JString(s"${jArrayReduced.arr.size} [Byte] (Compactly Rendered JSON-Array Size)")),
              JField(FIELD_NAME_REMOVED_ELEMENT, JNothing),
              JField(FIELD_NAME_CHANGES, JNothing))
          )
        ))
      ))
    )


    val jObjectResultJson: JValue = JObject(
      JField(FIELD_NAME_TYPE, JString(jObjectResult.`type`)),
      JField(FIELD_NAME_WAS_REDUCED, JBool(true)),
      JField(FIELD_NAME_ORIGINAL_VALUE, jObject),
      JField(FIELD_NAME_REDUCED_VALUE, jObjectReduced),
      JField(FIELD_NAME_ORIGINAL_SIZE, JString("23 [Byte] (Compactly Rendered JSON-Object Size)")),
      JField(FIELD_NAME_REDUCED_SIZE, JString("14 [Byte] (Compactly Rendered JSON-Object Size)")),
      JField(FIELD_NAME_OBJECT_CHANGES, JObject(
        JField(FIELD_NAME_CHANGED_AMOUNT, JInt(1)),
        JField(FIELD_NAME_CHANGED_ELEMENTS, JArray(
          List(
            JObject(
              JField(FIELD_NAME_INDEX, JNothing),
              JField(FIELD_NAME_KEY, JString(key)),
              JField(FIELD_NAME_TYPE, JsonStringReducibleType().toJson),
              JField(FIELD_NAME_ORIGINAL_SIZE, JString(s"${string.length} [Byte]")),
              JField(FIELD_NAME_REDUCED_SIZE, JString(s"${stringReduced.length} [Byte]")),
              JField(FIELD_NAME_REMOVED_ELEMENT, JNothing),
              JField(FIELD_NAME_CHANGES, JNothing))
          )
        ))
      ))
    )
  }


  trait StringRepresentation extends BaseFixture {
    val originalResultString: String = "The Original Unreduced Result of the type: [Float] without a clear length."
    val originalResultString2: String = "The Original Unreduced Result of the type: [String] with the length of 13 [Byte]."
    val byteStringResultString: String = "The reduced Byte String with the length of 3 [Byte] and with the original length of 10 [Byte]."
    val originalResultJValueString: String = "The Original Unreduced Json Result of the type: [JDouble] without a clear length."
    val originalResultJValueString2: String = "The Original Unreduced Json Result of the type: [JString] with the length of 13 [Byte]."
    val stringResultString: String = "The reduced String with the length of 4 [Byte] and with the original length of 13 [Byte]."
    val jStringResultString: String = "The reduced Json String with the length of 4 [Byte] and with the original length of 13 [Byte]."
    val jArrayResultString: String = "The reduced Json Array with the length of 1 [Byte] and with the original length of 3 [Byte] with 1 reduced element."
    val jArrayResultString2: String = "The original Json Array with the length of 6 [Byte] (Compactly Rendered JSON-Array Size)."
    val jObjectResultString: String = "The reduced Json Object with the length of 14 [Byte] (Compactly Rendered JSON-Object Size) and with the original length of 23 [Byte] (Compactly Rendered JSON-Object Size) with 1 reduced element."
  }


  trait JsonValue extends BaseFixture {
    implicit val formats: Formats = DefaultFormats.lossless

    val defaultValue: JValue = JNothing

    val originalResultValue: JValue = JDouble(float)
    val byteStringResultValue: JValue = jArrayAsHexReduced
    val originalResultJValueValue: JValue = jValue
    val stringResultValue: JValue = jStringReduced
    val jStringResultValue: JValue = jStringReduced
    val jArrayResultValue: JValue = jArrayReduced
    val jObjectResultValue: JValue = jObjectReduced

    val byteStringResultValueOriginal: JValue = JArray(List(JString("01"), JString("02"), JString("03"), JString("04"), JString("05"), JString("06"), JString("07"), JString("08"), JString("09"), JString("0A")))
    val stringResultValueOriginal: JValue = jString
    val jStringResultValueOriginal: JValue = jString
    val jArrayResultValueOriginal: JValue = jArray
    val jObjectResultValueOriginal: JValue = jObject
  }

  trait Serializer extends JsonValue {
    //Due to Json4s there are some serializers necessary
    val formatsWithoutSerializer: Formats = DefaultFormats.lossless +
      ReductionChanges.ReductionChangesSerializer +
      OriginalResultChangesSerializer[Float] +
      ByteStringSerializer

    val formatsWithSmallFloatSerializer: Formats = DefaultFormats.lossless +
      ReductionResult.ReductionResultSmallSerializer[Float](omitReducedValue = false, DefaultFormats.lossless)

    val formatsWithSmallByteStringSerializer: Formats = DefaultFormats.lossless +
      ReductionResult.ReductionResultSmallSerializer[ByteString](omitReducedValue = false, DefaultFormats.lossless + ByteStringSerializer)

    val formatsWithSmallOmittingSerializer: Formats = DefaultFormats.lossless +
      ReductionResult.ReductionResultSmallSerializer[Float](omitReducedValue = true, DefaultFormats.lossless)




    val formatsWithSmallOmittingSerializerNull: Formats = DefaultFormats.lossless + ReductionResult.ReductionResultSmallSerializer[Option[_]](omitReducedValue = true, DefaultFormats.lossless)
    val formatsWithFullSerializer: Formats = DefaultFormats.lossless +
      ReductionResult.ReductionResultFullSerializer[Float](DefaultFormats.lossless) +
      OriginalResultChangesSerializer[Float]()

  }

}