package io.simplifier.plugin.contentrepo.pluginBaseRelated.reducer.reductionChanges

import org.json4s.{JValue, _}


/**
  * The removed element case class containing all information about the removed element.
  *
  * @param removedKeys      the removed keys.
  * @param removedIndices   the removed indices.
  */
 case class RemovedElement(removedKeys: Seq[String],
                                           removedIndices: Seq[Int]) {

  import RemovedElement._


  /**
    * Returns whether this removed elements is empty i.e. neither keys nor indices were removed.
    *
    * @return    <b>true</b> if neither removed keys nor indices were removed, <b>false</b> otherwise.
    */
  def isEmpty: Boolean = removedKeys.isEmpty && removedIndices.isEmpty


  /**
    * Returns the respective Json representation.
    *
    * @return    the respective Json representation.
    */
  def toJson: JValue = {
    JObject(
      JField(FIELD_NAME_REMOVED_KEYS, if (removedKeys.isEmpty) JNothing else JArray(removedKeys.map(JString).toList)),
      JField(FIELD_NAME_REMOVED_INDICES, if (removedIndices.isEmpty) JNothing else JArray(removedIndices.map(JInt(_)).toList))
    )
  }


  /**
    * Returns the respective String representation.
    *
    * @return    the respective String representation.
    */
  override def toString: String = {
    val removedKeysString: String = if (removedKeys.isEmpty) "without any removed keys" else
      s"with ${removedKeys.size} removed key${if (removedKeys.size == 1) "" else "s"}: [${removedKeys.mkString(", ")}]"
    val removedIndicesString: String = if (removedIndices.isEmpty) "without any removed indices" else
      s"with ${removedIndices.size} removed ${if (removedIndices.size == 1) "index" else "indices"}: [${removedIndices.mkString(", ")}]"

    (removedKeys.isEmpty, removedIndices.isEmpty) match {
      case (false, false) => s"A removed mixed element $removedKeysString and $removedIndicesString."
      case (false, true) => s"An object $removedKeysString."
      case (true, false) => s"An array $removedIndicesString."
      case (true, true) => "No removed element."
    }
  }
}


/** The Removed Element Companion Object */
  object RemovedElement {


  /** JSON Serializer for [[RemovedElement]]. */
  lazy val RemovedElementSerializer: CustomSerializer[RemovedElement] = {
    new CustomSerializer[RemovedElement](_ => {
      implicit val formats: Formats = DefaultFormats

      def canBeExtracted(`object`: JObject): Boolean = {
        Extraction.extractOpt[RemovedElement](`object`).nonEmpty &&
          Extraction.extractOpt[RemovedElement](`object`).exists(!_.isEmpty)
      }


      val deserializer: PartialFunction[JValue, RemovedElement] = {
        case obj: JObject if canBeExtracted(obj) => Extraction.extract[RemovedElement](obj)
        case JObject(fields) => constructFromFields(fields)
        case _ => apply()
      }
      val serializer: PartialFunction[Any, JValue] = {
        case element: RemovedElement => element.toJson
      }
      (deserializer, serializer)
    })
  }


  /**
    * Creates a default [[RemovedElement]].
    *
    * @return    the default [[RemovedElement]].
    */
  def apply(): RemovedElement = {
    apply(
      removedKeysOrIndices = Seq.empty
    )
  }


  /**
    * Creates a new [[RemovedElement]] without any optional information.
    *
    * @param removedKeysOrIndices    the removed elements.
    *
    * @return                        the [[RemovedElement]] without any optional information.
    */
  def apply(removedKeysOrIndices: Seq[_]): RemovedElement = {
    new RemovedElement(
      removedKeys = removedKeysOrIndices.collect { case key: String => key },
      removedIndices = removedKeysOrIndices.collect { case index: Int => index }
    )
  }


  protected[reductionChanges] val FIELD_NAME_REMOVED_KEYS: String = "Removed Keys"
  protected[reductionChanges] val FIELD_NAME_REMOVED_INDICES: String = "Removed Indices"


  private[this] def constructFromFields(fields: Seq[JField]): RemovedElement = {
    new RemovedElement(
      removedKeys = fields.collectFirst {
        case JField(FIELD_NAME_REMOVED_KEYS, JArray(values)) => values.collect {
          case JString(k) => k
        }
      }.getOrElse(Seq.empty[String]),

      removedIndices = fields.collectFirst {
        case JField(FIELD_NAME_REMOVED_INDICES, JArray(values)) => values.collect {
          case JInt(i) if i <= Integer.MAX_VALUE => i.toInt
        }
      }.getOrElse(Seq.empty[Int])
    )
  }
}