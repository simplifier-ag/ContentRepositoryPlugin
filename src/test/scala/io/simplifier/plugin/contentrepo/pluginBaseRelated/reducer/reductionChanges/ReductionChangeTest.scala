package io.simplifier.plugin.contentrepo.pluginBaseRelated.reducer.reductionChanges

import io.simplifier.plugin.contentrepo.pluginBaseRelated.reducer.ReducibleJSONDataTypes.{JsonArrayReducibleType, NonReducibleJson, REDUCIBLE_JSON_DATA_TYPES_TYPE_HINTS}
import io.simplifier.plugin.contentrepo.pluginBaseRelated.reducer.ReductionChanges.{FIELD_NAME_CHANGED_AMOUNT, FIELD_NAME_CHANGED_ELEMENTS}
import io.simplifier.plugin.contentrepo.pluginBaseRelated.reducer.ReductionResult.{FIELD_NAME_CHANGES, FIELD_NAME_ORIGINAL_SIZE, FIELD_NAME_REDUCED_SIZE, FIELD_NAME_TYPE}
import io.simplifier.plugin.contentrepo.pluginBaseRelated.reducer.reductionChanges.ReductionChange.{FIELD_NAME_INDEX, FIELD_NAME_KEY, FIELD_NAME_REMOVED_ELEMENT, ReductionChangeSerializer}
import io.simplifier.plugin.contentrepo.pluginBaseRelated.reducer.reductionChanges.RemovedElement.{FIELD_NAME_REMOVED_INDICES, FIELD_NAME_REMOVED_KEYS}
import io.simplifier.plugin.contentrepo.pluginBaseRelated.reducer.{ReducibleJSONDataTypes, ReductionChanges}
import org.json4s.{JValue, _}
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec

class ReductionChangeTest extends AnyWordSpec with Matchers {


  "A Reduction Change Case Class" when {
    "returning the JSON representation" should {
      "yield the correct JSON representation" in new JsonRepresentation {
        reductionChangeMinimal.toJson mustBe reductionChangeMinimalJson
        reductionChangeNonOptional.toJson mustBe reductionChangeNonOptionalJson
        reductionChangeNonOptionalNoSizes.toJson mustBe reductionChangeNonOptionalNoSizesJson
        reductionChangeChanges.toJson mustBe reductionChangeChangesJson
      }
    }

    "reducing the String representation" should {
      "yield the correct String representation" in new StringRepresentation {
        reductionChangeMinimal.toString mustBe reductionChangeMinimalString
        reductionChangeNonOptional.toString mustBe reductionChangeNonOptionalString
        reductionChangeNonOptionalNoSizes.toString mustBe reductionChangeNonOptionalNoSizesString
        reductionChangeNonOptionalNoSizesEmptyList.toString mustBe reductionChangeNonOptionalNoSizesEmptyListString
        reductionChangeChanges.toString mustBe reductionChangeChangesString
        reductionChangeChangesAlt.toString mustBe reductionChangeChangesAltString
      }
    }

    "serializing a Case Class" should {
      "yield the correct JSON" in new Serializer {
        Extraction.decompose(reductionChangeMinimal) mustBe reductionChangeMinimalJson
        Extraction.decompose(reductionChangeNonOptional) mustBe reductionChangeNonOptionalJson
        Extraction.decompose(reductionChangeNonOptionalNoSizes) mustBe reductionChangeNonOptionalNoSizesJson
        Extraction.decompose(reductionChangeNonOptionalNoSizesEmptyList) mustBe reductionChangeNonOptionalNoSizesJson
        Extraction.decompose(reductionChangeChanges) mustBe reductionChangeChangesJson
      }
    }

    "deserializing a JSON" should {
      "yield the correct Case Class" in new Serializer {

        Extraction.extract[ReductionChange](JNothing) mustBe reductionChangeMinimal
        Extraction.extract[ReductionChange](JNull) mustBe reductionChangeMinimal
        Extraction.extract[ReductionChange](reductionChangeMinimalJson) mustBe reductionChangeMinimal
        Extraction.extract[ReductionChange](reductionChangeNonOptionalJson)
        Extraction.extract[ReductionChange](reductionChangeNonOptionalNoSizesJson) mustBe reductionChangeNonOptionalNoSizes
        Extraction.extract[ReductionChange](reductionChangeNonOptionalNoSizesEmptyListJson) mustBe reductionChangeNonOptionalNoSizes
        Extraction.extract[ReductionChange](reductionChangeChangesJson) mustBe reductionChangeChanges
        Extraction.extract[ReductionChange](reductionChangeChangesJsonAlt) mustBe reductionChangeChangesAlt
        Extraction.extract[ReductionChange](Extraction.decompose(reductionChangeChanges)(formatsWithReducibleSerializerOnly)) mustBe reductionChangeChanges
      }
    }
  }


  trait BaseFixture {
    val key: String = "key"
    val index: Int = 1337
    val originalSize: Int = 42
    val reducedSize: Int = 21

    val removedKeys: Seq[String] = Seq(key)
    val removedIndices: Seq[Int] = Seq(index)

    val reductionChangeMinimal: ReductionChange = ReductionChange()

    val reductionChangeNonOptional: ReductionChange = ReductionChange(
      `type` = ReducibleJSONDataTypes.JsonArrayReducibleType(),
      originalSize = originalSize,
      reducedSize = reducedSize
    )


    val reductionChangeNonOptionalNoSizesEmptyList: ReductionChange = ReductionChange(
      index = None,
      key = None,
      `type` = ReducibleJSONDataTypes.JsonArrayReducibleType(),
      originalSize = -1,
      reducedSize = -1,
      removedElement = None,
      changes = Some(ReductionChanges(Seq.empty[ReductionChange]))
    )


    val reductionChangeNonOptionalNoSizes: ReductionChange = ReductionChange(
      `type` = ReducibleJSONDataTypes.JsonArrayReducibleType(),
      originalSize = -1,
      reducedSize = -1
    )


    val changes: Seq[ReductionChange] = Seq(reductionChangeMinimal, reductionChangeNonOptional)
    val changesAlt: Seq[ReductionChange] = Seq(reductionChangeMinimal)

    val reductionChangeChanges: ReductionChange =  ReductionChange(
      index = Some(index),
      key = Some(key),
      `type` = ReducibleJSONDataTypes.JsonArrayReducibleType(),
      originalSize = originalSize,
      reducedSize = reducedSize,
      removedElement = None,
      changes = Some(
        ReductionChanges(
          changes = changes
        )
      )
    )

    val reductionChangeChangesAlt: ReductionChange =  ReductionChange(
      index = Some(index),
      key = Some(key),
      `type` = ReducibleJSONDataTypes.JsonArrayReducibleType(),
      originalSize = originalSize,
      reducedSize = reducedSize,
      removedElement = Some(
        new RemovedElement(
          removedKeys = removedKeys,
          removedIndices = removedIndices
        )
      ),
      changes = Some(
        ReductionChanges(
          changes = changesAlt
        )
      )
    )
  }


  trait JsonRepresentation extends BaseFixture {
    val reductionChangeMinimalJson: JValue = JObject(
      List(
        JField(FIELD_NAME_INDEX, JNothing),
        JField(FIELD_NAME_KEY, JNothing),
        JField(FIELD_NAME_TYPE, NonReducibleJson().toJson),
        JField(FIELD_NAME_ORIGINAL_SIZE, JNothing),
        JField(FIELD_NAME_REDUCED_SIZE, JNothing),
        JField(FIELD_NAME_REMOVED_ELEMENT, JNothing),
        JField(FIELD_NAME_CHANGES, JNothing))
    )

    val reductionChangeNonOptionalJson: JValue = JObject(
      List(
        JField(FIELD_NAME_INDEX, JNothing),
        JField(FIELD_NAME_KEY, JNothing),
        JField(FIELD_NAME_TYPE, JsonArrayReducibleType().toJson),
        JField(FIELD_NAME_ORIGINAL_SIZE, JString(s"$originalSize [Byte] (Compactly Rendered JSON-Array Size)")),
        JField(FIELD_NAME_REDUCED_SIZE, JString(s"$reducedSize [Byte] (Compactly Rendered JSON-Array Size)")),
        JField(FIELD_NAME_REMOVED_ELEMENT, JNothing),
        JField(FIELD_NAME_CHANGES, JNothing))
    )

    val reductionChangeNonOptionalNoSizesJson: JValue = JObject(
      List(
        JField(FIELD_NAME_INDEX, JNothing),
        JField(FIELD_NAME_KEY, JNothing),
        JField(FIELD_NAME_TYPE, JsonArrayReducibleType().toJson),
        JField(FIELD_NAME_ORIGINAL_SIZE, JNothing),
        JField(FIELD_NAME_REDUCED_SIZE, JNothing),
        JField(FIELD_NAME_REMOVED_ELEMENT, JNothing),
        JField(FIELD_NAME_CHANGES, JNothing))
    )

    val reductionChangeNonOptionalNoSizesEmptyListJson: JValue = JObject(
      List(
        JField(FIELD_NAME_INDEX, JNothing),
        JField(FIELD_NAME_KEY, JNothing),
        JField(FIELD_NAME_TYPE, JsonArrayReducibleType().toJson),
        JField(FIELD_NAME_ORIGINAL_SIZE, JNothing),
        JField(FIELD_NAME_REDUCED_SIZE, JNothing),
        JField(FIELD_NAME_REMOVED_ELEMENT, JNothing),
        JField(FIELD_NAME_CHANGES, JArray(List())))
    )

    val reductionChangeChangesJson: JValue = JObject(
      List(
        JField(FIELD_NAME_INDEX, JInt(index)),
        JField(FIELD_NAME_KEY, JString(key)),
        JField(FIELD_NAME_TYPE, JsonArrayReducibleType().toJson),
        JField(FIELD_NAME_ORIGINAL_SIZE, JString(s"$originalSize [Byte] (Compactly Rendered JSON-Array Size)")),
        JField(FIELD_NAME_REDUCED_SIZE, JString(s"$reducedSize [Byte] (Compactly Rendered JSON-Array Size)")),
        JField(FIELD_NAME_REMOVED_ELEMENT, JNothing),
        JField(FIELD_NAME_CHANGES, JObject(
          JField(FIELD_NAME_CHANGED_AMOUNT, JInt(changes.size)),
          JField(FIELD_NAME_CHANGED_ELEMENTS, JArray(
            List(
              reductionChangeMinimalJson,
              reductionChangeNonOptionalJson
            )
          ))
        ))
      ))


    val reductionChangeChangesJsonAlt: JValue = JObject(
      List(
        JField(FIELD_NAME_INDEX, JInt(index)),
        JField(FIELD_NAME_KEY, JString(key)),
        JField(FIELD_NAME_TYPE, JsonArrayReducibleType().toJson),
        JField(FIELD_NAME_ORIGINAL_SIZE, JString(s"$originalSize [Byte] (Compactly Rendered JSON-Array Size)")),
        JField(FIELD_NAME_REDUCED_SIZE, JString(s"$reducedSize [Byte] (Compactly Rendered JSON-Array Size)")),
        JField(FIELD_NAME_REMOVED_ELEMENT, JObject(
          JField(FIELD_NAME_REMOVED_KEYS, JArray(
            List(
              JString(key)
            )
          )),
          JField(FIELD_NAME_REMOVED_INDICES, JArray(
            List(
              JInt(index)
            )
          )))),
        JField(FIELD_NAME_CHANGES, JObject(
          JField(FIELD_NAME_CHANGED_AMOUNT, JInt(changes.size)),
          JField(FIELD_NAME_CHANGED_ELEMENTS, reductionChangeMinimalJson)
        ))
      ))
  }


  trait StringRepresentation extends BaseFixture {
    val reductionChangeMinimalString: String = "A Non-Reducible JSON change without an index, without a key, without an original size, without a reduced size, without any removed element and without any changes."
    val reductionChangeNonOptionalString: String = "A JSON Array change without an index, without a key, with the original size: [42], with the reduced size: [21], without any removed element and without any changes."
    val reductionChangeNonOptionalNoSizesString: String = "A JSON Array change without an index, without a key, without an original size, without a reduced size, without any removed element and without any changes."
    val reductionChangeNonOptionalNoSizesEmptyListString: String = "A JSON Array change without an index, without a key, without an original size, without a reduced size, without any removed element and with no occurred reductions."
    val reductionChangeChangesString: String = "A JSON Array change with the index: [1337], with the key: [key], with the original size: [42], with the reduced size: [21], without any removed element and with 2 reduced elements."
    val reductionChangeChangesAltString: String = "A JSON Array change with the index: [1337], with the key: [key], with the original size: [42], with the reduced size: [21], with a removed mixed element with 1 removed key: [key] and with 1 removed index: [1337] and with 1 reduced element."
  }


  trait Serializer extends JsonRepresentation {
    implicit val formats: Formats = DefaultFormats.lossless + ReductionChangeSerializer
    val formatsWithReducibleSerializerOnly: Formats = DefaultFormats.lossless.withPre36DeserializationBehavior +
      REDUCIBLE_JSON_DATA_TYPES_TYPE_HINTS + ReducibleJSONDataTypes.ReducibleJSONDataTypesSerializer

    val oneChangeSerialized: JValue = JObject(
      JField(FIELD_NAME_CHANGED_AMOUNT, JInt(1)),
      JField(FIELD_NAME_CHANGED_ELEMENTS, JArray(
        List(
          reductionChangeMinimalJson
        )
      ))
    )

    val twoChangedSerialized: JValue = JObject(
      JField(FIELD_NAME_CHANGED_AMOUNT, JInt(2)),
      JField(FIELD_NAME_CHANGED_ELEMENTS, JArray(
        List(
          reductionChangeMinimalJson,
          reductionChangeMinimalJson
        )
      ))
    )

    val twoChangedSerializedJArray: JValue = JArray(
      List(
        reductionChangeMinimalJson,
        reductionChangeMinimalJson
      )
    )
  }


  trait EqualsAndHashCode extends BaseFixture {
    val newTwoReduction: ReductionChanges = ReductionChanges(
      Seq(
        ReductionChange(),
        ReductionChange()
      )
    )
  }

}