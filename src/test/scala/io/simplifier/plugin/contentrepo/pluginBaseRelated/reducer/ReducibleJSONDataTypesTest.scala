package io.simplifier.plugin.contentrepo.pluginBaseRelated.reducer

import io.simplifier.plugin.contentrepo.pluginBaseRelated.reducer.ReducibleJSONDataTypes.{JsonArrayReducibleType, JsonByteArrayEquivalentReducibleType, JsonObjectReducibleType, JsonStringReducibleType, NonReducibleJson, REDUCIBLE_JSON_DATA_TYPES_TYPE_HINTS}
import io.simplifier.plugin.contentrepo.pluginBaseRelated.reducer.reductionChanges.ReductionChange
import org.json4s.{JValue, _}
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec

class ReducibleJSONDataTypesTest extends AnyWordSpec with Matchers {


  "A Reducible JSON Data Types Trait" when {
    "returning the JSON representation" should {
      "yield the correct JSON representation" in new JsonRepresentation {
        JsonStringReducibleType().toJson mustBe JsonStringReducibleTypeJson
        JsonObjectReducibleType().toJson mustBe JsonObjectReducibleTypeJson
        JsonArrayReducibleType().toJson mustBe JsonArrayReducibleTypeJson
        JsonByteArrayEquivalentReducibleType().toJson mustBe JsonByteArrayEquivalentReducibleTypeJson
        NonReducibleJson().toJson mustBe NonReducibleJsonJson
      }
    }

    "reducing the String representation" should {
      "yield the correct String representation" in new StringRepresentation {
        JsonStringReducibleType().toString mustBe JsonStringReducibleTypeString
        JsonObjectReducibleType().toString mustBe JsonObjectReducibleTypeString
        JsonArrayReducibleType().toString mustBe JsonArrayReducibleTypeString
        JsonByteArrayEquivalentReducibleType().toString mustBe JsonByteArrayEquivalentReducibleTypeString
        NonReducibleJson().toString mustBe NonReducibleJsonString
      }
    }

    "creating the respective element from a String" should {
      "yield the correct JSON representation" in new Creation {
        ReducibleJSONDataTypes(JsonStringReducibleTypeString) mustBe JsonStringReducibleType()
        ReducibleJSONDataTypes(JsonObjectReducibleTypeString) mustBe JsonObjectReducibleType()
        ReducibleJSONDataTypes(JsonArrayReducibleTypeString) mustBe JsonArrayReducibleType()
        ReducibleJSONDataTypes(JsonByteArrayEquivalentReducibleTypeString) mustBe JsonByteArrayEquivalentReducibleType()
        ReducibleJSONDataTypes(NonReducibleJsonString) mustBe NonReducibleJson()
        ReducibleJSONDataTypes(other) mustBe NonReducibleJson()
      }
    }

    "serializing a Case Class" should {
      "yield the correct JSON" in new Serializer {
        Extraction.decompose(JsonStringReducibleType()) mustBe JsonStringReducibleTypeJson
        Extraction.decompose(JsonObjectReducibleType()) mustBe JsonObjectReducibleTypeJson
        Extraction.decompose(JsonArrayReducibleType()) mustBe JsonArrayReducibleTypeJson
        Extraction.decompose(JsonByteArrayEquivalentReducibleType()) mustBe JsonByteArrayEquivalentReducibleTypeJson
        Extraction.decompose(NonReducibleJson()) mustBe NonReducibleJsonJson
      }
    }

    "deserializing a JSON" should {
      "yield the correct Case Class" in new Serializer {
        Extraction.extract[ReducibleJSONDataTypes](JNothing) mustBe NonReducibleJson()
        Extraction.extract[ReducibleJSONDataTypes](otherString) mustBe NonReducibleJson()
        Extraction.extract[ReducibleJSONDataTypes](Extraction.decompose(oneChange)(formatsWithoutSerializer)) mustBe NonReducibleJson()
        Extraction.extract[ReducibleJSONDataTypes](JsonStringReducibleTypeJson) mustBe JsonStringReducibleType()
        Extraction.extract[ReducibleJSONDataTypes](JsonObjectReducibleTypeJson) mustBe JsonObjectReducibleType()
        Extraction.extract[ReducibleJSONDataTypes](JsonArrayReducibleTypeJson) mustBe JsonArrayReducibleType()
        Extraction.extract[ReducibleJSONDataTypes](JsonByteArrayEquivalentReducibleTypeJson) mustBe JsonByteArrayEquivalentReducibleType()
        Extraction.extract[ReducibleJSONDataTypes](NonReducibleJsonJson) mustBe NonReducibleJson()
      }
    }


  }


  trait BaseFixture {
    val oneChange: ReductionChanges = ReductionChanges(
      Seq(
        ReductionChange()
      )
    )

    val twoChanges: ReductionChanges = ReductionChanges(
      Seq(
        ReductionChange(),
        ReductionChange()
      )
    )
  }


  trait StringRepresentation extends BaseFixture {
    val JsonStringReducibleTypeString: String = "JSON String"
    val JsonObjectReducibleTypeString: String = "JSON Object"
    val JsonArrayReducibleTypeString: String = "JSON Array"
    val JsonByteArrayEquivalentReducibleTypeString: String = "JSON Array (Byte Array Equivalent)"
    val NonReducibleJsonString: String = "Non-Reducible JSON"
  }

  trait JsonRepresentation extends StringRepresentation {
    val JsonStringReducibleTypeJson: JValue = JString(JsonStringReducibleType().toString)
    val JsonObjectReducibleTypeJson: JValue = JString(JsonObjectReducibleType().toString)
    val JsonArrayReducibleTypeJson: JValue = JString(JsonArrayReducibleType().toString)
    val JsonByteArrayEquivalentReducibleTypeJson: JValue = JString(JsonByteArrayEquivalentReducibleType().toString)
    val NonReducibleJsonJson: JValue = JString(NonReducibleJson().toString)
  }

  trait Creation extends StringRepresentation {
    val other: String = "NOOOOOO"
  }

  trait Serializer extends JsonRepresentation {
    implicit val formats: Formats = DefaultFormats.lossless.withPre36DeserializationBehavior + ReducibleJSONDataTypes.ReducibleJSONDataTypesSerializer + REDUCIBLE_JSON_DATA_TYPES_TYPE_HINTS
    val formatsWithoutSerializer: Formats = DefaultFormats.lossless.withPre36DeserializationBehavior
    val otherString: JString = JString("other")
  }

}
