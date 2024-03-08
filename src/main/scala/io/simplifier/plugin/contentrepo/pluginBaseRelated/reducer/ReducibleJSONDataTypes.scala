package io.simplifier.plugin.contentrepo.pluginBaseRelated.reducer

import org.json4s.{JValue, _}


/**
 * The sealed trait for all [[ReducibleJSONDataTypes]].
 */
sealed trait ReducibleJSONDataTypes {


  /**
   * Returns the respective Json representation.
   *
   * @return the respective Json representation.
   */
  def toJson: JValue = JString(this.toString)
}


/** The Reducible JSON Data Types Companion Object */
object ReducibleJSONDataTypes {

  lazy val REDUCIBLE_JSON_DATA_TYPES_TYPE_HINTS: FullTypeHints = FullTypeHints(
    List(
      classOf[NonReducibleJson],
      classOf[JsonStringReducibleType],
      classOf[JsonObjectReducibleType],
      classOf[JsonArrayReducibleType],
      classOf[JsonByteArrayEquivalentReducibleType]
    )
  )

  /** JSON Serializer for [[ReducibleJSONDataTypes]]. */
  protected[reducer] lazy val ReducibleJSONDataTypesSerializer: CustomSerializer[ReducibleJSONDataTypes] = {
    new CustomSerializer[ReducibleJSONDataTypes](_ => {
      val deserializer: PartialFunction[JValue, ReducibleJSONDataTypes] = {
        case JString(string) => apply(string)
        case _ => NonReducibleJson()
      }
      val serializer: PartialFunction[Any, JValue] = {
        case reducibleDataType: ReducibleJSONDataTypes => reducibleDataType.toJson
      }
      (deserializer, serializer)
    })
  }


  /**
   * Creates a [[ReducibleJSONDataTypes]] from a [[String]].
   *
   * @param string the provided [[String]].
   * @return the respective [[ReducibleJSONDataTypes]].
   */
  def apply(string: String): ReducibleJSONDataTypes = {
    string match {
      case _ if string == NonReducibleJson().toString => NonReducibleJson()
      case _ if string == JsonStringReducibleType().toString => JsonStringReducibleType()
      case _ if string == JsonObjectReducibleType().toString => JsonObjectReducibleType()
      case _ if string == JsonArrayReducibleType().toString => JsonArrayReducibleType()
      case _ if string == JsonByteArrayEquivalentReducibleType().toString => JsonByteArrayEquivalentReducibleType()
      case _ => NonReducibleJson()
    }
  }

  case class NonReducibleJson() extends ReducibleJSONDataTypes {
    override def toString: String = "Non-Reducible JSON"
  }

  case class JsonStringReducibleType() extends ReducibleJSONDataTypes {
    override def toString: String = "JSON String"
  }

  case class JsonObjectReducibleType() extends ReducibleJSONDataTypes {
    override def toString: String = "JSON Object"
  }

  case class JsonArrayReducibleType() extends ReducibleJSONDataTypes {
    override def toString: String = "JSON Array"
  }

  case class JsonByteArrayEquivalentReducibleType() extends ReducibleJSONDataTypes {
    override def toString: String = "JSON Array (Byte Array Equivalent)"
  }

}