package io.simplifier.plugin.contentrepo.pluginBaseRelated.reducer.reductionChanges

import io.simplifier.plugin.contentrepo.pluginBaseRelated.reducer.reductionChanges.RemovedElement.{FIELD_NAME_REMOVED_INDICES, FIELD_NAME_REMOVED_KEYS, RemovedElementSerializer}
import org.json4s.{JValue, _}
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec

class RemovedElementTest extends AnyWordSpec with Matchers {


  "A Removed Element Case Class" when {
    "returning the JSON representation" should {
      "yield the correct JSON representation" in new JsonRepresentation {
        removedElementMinimal.toJson mustBe removedElementMinimalJson
        removedElementKeysOnly.toJson mustBe removedElementKeysOnlyJson
        removedElementIndicesOnly.toJson mustBe removedElementIndicesJson
        removedElementMultipleKeys.toJson mustBe removedElementMultipleKeysJson
        removedElementMultipleIndices.toJson mustBe removedElementMultipleIndicesJson
        removedElementMixed.toJson mustBe removedElementMixedJson
      }
    }

    "reducing the String representation" should {
      "yield the correct String representation" in new StringRepresentation {
        removedElementMinimal.toString mustBe removedElementMinimalString
        removedElementKeysOnly.toString mustBe removedElementKeysOnlyString
        removedElementIndicesOnly.toString mustBe removedElementIndicesString
        removedElementMultipleKeys.toString mustBe removedElementMultipleKeysString
        removedElementMultipleIndices.toString mustBe removedElementMultipleIndicesString
        removedElementMixed.toString mustBe removedElementMixedString
      }
    }

    "serializing a Case Class" should {
      "yield the correct JSON" in new Serializer {
        Extraction.decompose(removedElementMinimal) mustBe removedElementMinimalJson
        Extraction.decompose(removedElementKeysOnly) mustBe removedElementKeysOnlyJson
        Extraction.decompose(removedElementIndicesOnly) mustBe removedElementIndicesJson
        Extraction.decompose(removedElementMultipleKeys) mustBe removedElementMultipleKeysJson
        Extraction.decompose(removedElementMultipleIndices) mustBe removedElementMultipleIndicesJson
        Extraction.decompose(removedElementMixed) mustBe removedElementMixedJson
      }
    }

    "deserializing a JSON" should {
      "yield the correct Case Class" in new Serializer {
        Extraction.extract[RemovedElement](JNothing) mustBe removedElementMinimal
        Extraction.extract[RemovedElement](JNull) mustBe removedElementMinimal
        Extraction.extract[RemovedElement](removedElementMinimalJson) mustBe removedElementMinimal
        Extraction.extract[RemovedElement](removedElementKeysOnlyJson) mustBe removedElementKeysOnly
        Extraction.extract[RemovedElement](removedElementIndicesJson) mustBe removedElementIndicesOnly
        Extraction.extract[RemovedElement](removedElementMultipleKeysJson) mustBe removedElementMultipleKeys
        Extraction.extract[RemovedElement](removedElementMultipleIndicesJson) mustBe removedElementMultipleIndices
        Extraction.extract[RemovedElement](removedElementMixedJson) mustBe removedElementMixed
        Extraction.extract[RemovedElement](Extraction.decompose(removedElementMixedJson)(formatsDefaultOnly)) mustBe removedElementMixed
      }
    }
  }


  trait BaseFixture {
    val key: String = "key"
    val key2: String = "key2"
    val index: Int = 1337
    val index2: Int = 42

    val removedKeys: Seq[String] = Seq(key)
    val removedIndices: Seq[Int] = Seq(index)
    val removedKeys2: Seq[String] = Seq(key, key2)
    val removedIndices2: Seq[Int] = Seq(index, index2)
    val removedMixed: Seq[Any] = Seq(key, index)


    val removedElementMinimal: RemovedElement = RemovedElement()

    val removedElementKeysOnly: RemovedElement = new RemovedElement(
      removedKeys = removedKeys,
      removedIndices = Seq.empty[Int]
    )

    val removedElementIndicesOnly: RemovedElement = new RemovedElement(
      removedKeys = Seq.empty[String],
      removedIndices = removedIndices
    )

    val removedElementMultipleKeys: RemovedElement = new RemovedElement(
      removedKeys = removedKeys2,
      removedIndices = Seq.empty[Int]
    )

    val removedElementMultipleIndices: RemovedElement = new RemovedElement(
      removedKeys = Seq.empty[String],
      removedIndices = removedIndices2
    )

    val removedElementMixed: RemovedElement = RemovedElement(
      removedKeysOrIndices = removedMixed
    )
  }


  trait JsonRepresentation extends BaseFixture {


    val removedElementMinimalJson: JValue = JObject(
      JField(FIELD_NAME_REMOVED_KEYS, JNothing),
      JField(FIELD_NAME_REMOVED_INDICES, JNothing)
    )

    val removedElementKeysOnlyJson: JValue = JObject(
      JField(FIELD_NAME_REMOVED_KEYS, JArray(
        List(
          JString(key)
        )
      )),
      JField(FIELD_NAME_REMOVED_INDICES, JNothing)
    )

    val removedElementIndicesJson: JValue = JObject(
      JField(FIELD_NAME_REMOVED_KEYS, JNothing),
      JField(FIELD_NAME_REMOVED_INDICES, JArray(
        List(
          JInt(index)
        )
      ))
    )

    val removedElementMultipleKeysJson: JValue = JObject(
      JField(FIELD_NAME_REMOVED_KEYS, JArray(
        List(
          JString(key),
          JString(key2)
        )
      )),
      JField(FIELD_NAME_REMOVED_INDICES, JNothing)
    )

    val removedElementMultipleIndicesJson: JValue = JObject(
      JField(FIELD_NAME_REMOVED_KEYS, JNothing),
      JField(FIELD_NAME_REMOVED_INDICES, JArray(
        List(
          JInt(index),
          JInt(index2)
        )
      ))
    )

    val removedElementMixedJson: JValue = JObject(
      JField(FIELD_NAME_REMOVED_KEYS, JArray(
        List(
          JString(key)
        )
      )),
      JField(FIELD_NAME_REMOVED_INDICES, JArray(
        List(
          JInt(index)
        )
      ))
    )
  }


  trait StringRepresentation extends BaseFixture {
    val removedElementMinimalString: String = "No removed element."
    val removedElementKeysOnlyString: String = "An object with 1 removed key: [key]."
    val removedElementIndicesString: String = "An array with 1 removed index: [1337]."
    val removedElementMultipleKeysString: String = "An object with 2 removed keys: [key, key2]."
    val removedElementMultipleIndicesString: String = "An array with 2 removed indices: [1337, 42]."
    val removedElementMixedString: String = "A removed mixed element with 1 removed key: [key] and with 1 removed index: [1337]."
  }


  trait Serializer extends JsonRepresentation {
    implicit val formats: Formats = DefaultFormats.lossless + RemovedElementSerializer
    val formatsDefaultOnly: Formats = DefaultFormats.lossless
  }
}