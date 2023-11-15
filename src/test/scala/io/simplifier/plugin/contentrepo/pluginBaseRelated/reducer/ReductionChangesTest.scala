package io.simplifier.plugin.contentrepo.pluginBaseRelated.reducer

import io.simplifier.plugin.contentrepo.pluginBaseRelated.reducer.ReducibleJSONDataTypes.NonReducibleJson
import io.simplifier.plugin.contentrepo.pluginBaseRelated.reducer.ReductionChanges.{FIELD_NAME_CHANGED_AMOUNT, FIELD_NAME_CHANGED_ELEMENTS, ReductionChangesSerializer}
import io.simplifier.plugin.contentrepo.pluginBaseRelated.reducer.ReductionResult.{FIELD_NAME_CHANGES, FIELD_NAME_ORIGINAL_SIZE, FIELD_NAME_REDUCED_SIZE, FIELD_NAME_TYPE}
import io.simplifier.plugin.contentrepo.pluginBaseRelated.reducer.reductionChanges.ReductionChange
import io.simplifier.plugin.contentrepo.pluginBaseRelated.reducer.reductionChanges.ReductionChange.{FIELD_NAME_INDEX, FIELD_NAME_KEY, FIELD_NAME_REMOVED_ELEMENT}
import org.json4s.{JValue, _}
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec

class ReductionChangesTest extends AnyWordSpec with Matchers {


  "A Reduction Result Case Class" when {
    "returning the JSON representation" should {
      "yield the correct JSON representation" in new JsonRepresentation {
        zeroChanges.toJson mustBe zeroChangesJson
        zeroChanges2.toJson mustBe zeroChangesJson
        oneChange.toJson mustBe oneChangeJson
        twoChanges.toJson mustBe twoChangesJson
      }
    }

    "reducing the String representation" should {
      "yield the correct String representation" in new StringRepresentation {
        zeroChanges.toString mustBe zeroChangesString
        zeroChanges2.toString mustBe zeroChangesString
        oneChange.toString mustBe oneChangeString
        twoChanges.toString mustBe twoChangesString
      }
    }

    "returning the reduced value as a JSON" should {
      "yield the correct JSON" in new Serializer {
        Extraction.decompose(zeroChanges) mustBe JNothing
        Extraction.decompose(zeroChanges2) mustBe JNothing
        Extraction.decompose(oneChange) mustBe oneChangeSerialized
        Extraction.decompose(twoChanges) mustBe twoChangedSerialized
      }
    }
  }


  trait BaseFixture {
    val zeroChanges: ReductionChanges = ReductionChanges()
    val zeroChanges2: ReductionChanges = ReductionChanges(Seq.empty)
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


    val zeroChangesJson: JValue = JNothing
    val oneChangeJson: JValue = JObject(
      JField(FIELD_NAME_CHANGED_AMOUNT, JInt(1)),
      JField(FIELD_NAME_CHANGED_ELEMENTS, JArray(
        List(
          reductionChangeMinimalJson
        )
      ))
    )
    val twoChangesJson: JValue = JObject(
      JField(FIELD_NAME_CHANGED_AMOUNT, JInt(2)),
      JField(FIELD_NAME_CHANGED_ELEMENTS, JArray(
        List(
          reductionChangeMinimalJson,
          reductionChangeMinimalJson
        )
      ))
    )
  }


  trait StringRepresentation extends BaseFixture {
    val zeroChangesString: String = "No occurred reductions."
    val oneChangeString: String = "1 reduced element."
    val twoChangesString: String = "2 reduced elements."
  }


  trait Serializer extends JsonRepresentation {
    implicit val formats: Formats = DefaultFormats.lossless + ReductionChangesSerializer
    val formatsWithoutReductionChangesSerializer: Formats = DefaultFormats.lossless

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
