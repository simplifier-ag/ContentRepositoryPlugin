package io.simplifier.plugin.contentrepo.pluginBaseRelated

import io.simplifier.plugin.contentrepo.pluginBaseRelated.reducer.ReducibleJSONDataTypes.{JsonArrayReducibleType, JsonByteArrayEquivalentReducibleType, JsonObjectReducibleType, JsonStringReducibleType, NonReducibleJson}
import io.simplifier.plugin.contentrepo.pluginBaseRelated.reducer.reductionResults.ReductionResultString
import io.simplifier.pluginbase.util.json.JSONFormatter.renderJSONCompact
import io.simplifier.pluginbase.util.xcoder.ByteArrayEncoding
import org.json4s.{JValue, _}

import java.text.{DecimalFormat, DecimalFormatSymbols}
import java.util.Locale

package object reducer {

  /** The Default Size */
  private[reducer] val DEFAULT_SIZE: Int = -1


  /** The Length Formatter */
  private[reducer] final val LENGTH_FORMATTER: DecimalFormat = {
    val symbols: DecimalFormatSymbols = new DecimalFormatSymbols(Locale.GERMAN)
    symbols.setDecimalSeparator(',')
    symbols.setGroupingSeparator('.')
    new DecimalFormat("###,###", symbols)
  }


  /**
    * Determine, whether the provided array values denotes a byte-array equivalent.
    *
    * @param arrayValues    the provided array values to check.
    *
    * @return               <b>true</b> if the values denotes either a signed, unsigned or hex array, <b>false</b> otherwise.
    */
  protected[reducer] def isByteArrayEquivalent(arrayValues: Seq[JValue]): Boolean = {
    JArray(arrayValues.toList) match {
      case array if !containsJNothing(array) && ByteArrayEncoding.ByteArrayEncoder.Hex.isArray(array) => true
      case array if !containsJNothing(array) && ByteArrayEncoding.ByteArrayEncoder.Signed.isArray(array) => true
      case array if !containsJNothing(array) && ByteArrayEncoding.ByteArrayEncoder.Unsigned.isArray(array) => true
      case _ => false
    }
  }


  /**
    * Determines the serialized length of a JSON.
    *
    * @note         # the reduced messages in strings will not be counted as a part to count.
    *               # the length will be calculated with a compact serialization.
    * @param value  the provided Json value.
    *
    * @return       the length of the provided Json value.
    */
  protected[reducer] def determineSerializedLength(value: JValue): Int = {
    renderJSONCompact(value)
      .replaceAll(ReductionResultString.SHORTENED_STRING_DETERMINER_REGEX.regex, "")
      .length
  }


  /**
    * Determines the [[ReducibleJSONDataTypes]] from a provided Json-Value.
    *
    * @param value    the provided Json-Value.
    *
    * @return         the respective [[ReducibleJSONDataTypes]].
    */
  protected[reducer] def determineJsonType(value: JValue): ReducibleJSONDataTypes = {
    value match {
      case JString(_) => JsonStringReducibleType()
      case JObject(_) => JsonObjectReducibleType()
      case JArray(values) if isByteArrayEquivalent(values) => JsonByteArrayEquivalentReducibleType()
      case JArray(_) => JsonArrayReducibleType()
      case _ => NonReducibleJson()
    }
  }


  /**
    * Determines the Json-representation for a size.
    *
    * @param size      the provided size.
    * @param `type`    the [[ReducibleJSONDataTypes]].
    *
    * @return          the respective size Json-representation.
    */
  protected[reducer] def determineSizeString(size: Int,
                                             `type`: ReducibleJSONDataTypes): JValue = {
    if (size == DEFAULT_SIZE) JNothing
    else {
      `type` match {
        case NonReducibleJson() => JNothing
        case JsonStringReducibleType() | JsonByteArrayEquivalentReducibleType() => JString(s"${LENGTH_FORMATTER.format(size)} [Byte]")
        case JsonArrayReducibleType() => JString(s"${LENGTH_FORMATTER.format(size)} [Byte] (Compactly Rendered JSON-Array Size)")
        case JsonObjectReducibleType() => JString(s"${LENGTH_FORMATTER.format(size)} [Byte] (Compactly Rendered JSON-Object Size)")
      }
    }
  }


  private[this] def containsJNothing(array: JArray): Boolean = {
    array.arr.contains(JNothing)
  }
}
