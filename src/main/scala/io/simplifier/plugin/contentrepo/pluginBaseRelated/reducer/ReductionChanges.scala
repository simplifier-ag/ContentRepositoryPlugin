package io.simplifier.plugin.contentrepo.pluginBaseRelated.reducer

import io.simplifier.pluginbase.util.json.NamedTupleAccess._
import io.simplifier.plugin.contentrepo.pluginBaseRelated.reducer.ReductionChanges._
import io.simplifier.plugin.contentrepo.pluginBaseRelated.reducer.reductionChanges.ReductionChange
import io.simplifier.plugin.contentrepo.pluginBaseRelated.reducer.reductionChanges.ReductionChange.ReductionChangeSerializer
import org.json4s._



/**
  * This case class incorporates all occurred reduction changes.
  *
  * @param changes    the sequence containing the respective [[ReductionChange]].
  */
case class ReductionChanges(changes: Seq[ReductionChange]) {

  /**
    * Returns the respective Json representation.
    *
    * @return    the respective Json representation.
    */
  def toJson: JValue = {
    if (changes.isEmpty) JNothing
    else {
      JObject(
        JField(FIELD_NAME_CHANGED_AMOUNT, JInt(changes.size)),
        JField(FIELD_NAME_CHANGED_ELEMENTS, JArray(
          changes.map(_.toJson).toList
        ))
      )
    }
  }


  /**
    * Returns the respective String representation.
    *
    * @return    the respective String representation.
    */
  override def toString: String = {
    if (changes.isEmpty) "No occurred reductions." else s"${changes.size} reduced element${if (changes.size != 1) "s" else ""}."
  }


  /**
    * Overwrites the equals function, to allow comparision.
    *
    * @param obj    the object to compare.
    *
    * @return       <b>true</b> if all changes are equal, <b>false</b> otherwise.
    */
  override def equals(obj: Any): Boolean = {
    obj match {
      case ReductionChanges(c) => c == changes
      case _ => false
    }
  }


  /** ReductionChanges have the same hash code, when their changes are equal */
  override def hashCode(): Int = changes.hashCode()
}


/** The Reduction Changes Companion Object */
object ReductionChanges {

  /** JSON Serializer for [[ReductionChanges]] */
  lazy val ReductionChangesSerializer: CustomSerializer[ReductionChanges] = {
    new CustomSerializer[ReductionChanges](_ => {
      val deserializer: PartialFunction[JValue, ReductionChanges] = {
        case JObject(fields) => constructFromValues(fields)
        case JArray(values) => constructFromValues(values)
        case _ => apply()
      }
      val serializer: PartialFunction[Any, JValue] = {
        case changes: ReductionChanges => changes.toJson
      }
      (deserializer, serializer)
    })
  }


  /**
    * Creates a default [[ReductionChanges]].
    *
    * @return    the default [[ReductionChanges]].
    */
  def apply(): ReductionChanges = {
    new ReductionChanges(
      changes = Seq.empty[ReductionChange]
    )
  }


  val FIELD_NAME_CHANGED_AMOUNT: String = "Changed Amount"
  val FIELD_NAME_CHANGED_ELEMENTS: String = "Changed Elements"


  private[this] def constructFromValues(values: Seq[_]): ReductionChanges = {
    implicit val formats: Formats = DefaultFormats.withPre36DeserializationBehavior  + ReductionChangeSerializer

    def areAllValuesJFields:Boolean = {
      values.forall {
        case JField(_, _) => true
        case _ => false
      }
    }

    val changes: Seq[ReductionChange] = if (areAllValuesJFields) {
      val jFields: List[JField] = values.collect {
        case field: JField => field
      }.toList

      if (jFields.exists(_.name == FIELD_NAME_CHANGED_ELEMENTS)) {
        jFields.collectFirst {
          case JField(FIELD_NAME_CHANGED_ELEMENTS, JArray(v)) => constructFromValues(v).changes
        }.getOrElse(Seq.empty[ReductionChange])
      } else Extraction.extractOpt[ReductionChange](JObject(jFields)).fold(Seq.empty[ReductionChange])(change => Seq(change))
    } else {
      values.collect {
        case value: JValue => Extraction.extractOpt[ReductionChange](value)
      }.collect {
        case Some(change) => change
      }
    }

    new ReductionChanges(
      changes = changes
    )
  }
}