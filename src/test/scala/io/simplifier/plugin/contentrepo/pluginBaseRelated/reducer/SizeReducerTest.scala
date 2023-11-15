package io.simplifier.plugin.contentrepo.pluginBaseRelated.reducer

import akka.util.ByteString
import io.simplifier.plugin.contentrepo.pluginBaseRelated.reducer.ReducibleJSONDataTypes.{JsonArrayReducibleType, JsonByteArrayEquivalentReducibleType, JsonObjectReducibleType, JsonStringReducibleType}
import io.simplifier.plugin.contentrepo.pluginBaseRelated.reducer.SizeReducer.{MaximumLengthMayNotBeNegative, MaximumLengthMayNotBeZero}
import io.simplifier.plugin.contentrepo.pluginBaseRelated.reducer.reductionChanges.{ReductionChange, RemovedElement}
import io.simplifier.plugin.contentrepo.pluginBaseRelated.reducer.reductionResults.{OriginalResult, OriginalResultJValue, ReductionResultByteString, ReductionResultJArray, ReductionResultJObject, ReductionResultJString, ReductionResultString}
import io.simplifier.plugin.contentrepo.pluginBaseRelated.reducer.sizeReducer.SizeReducerGenericWorkflow.UnexpectedErrorDuringReduction
import io.simplifier.pluginbase.util.json.JSONFormatter.renderJSONCompact
import io.simplifier.pluginbase.util.json.SimplifierFormats
import io.simplifier.pluginbase.util.xcoder.ByteArrayEncoding
import org.json4s.{JValue, _}
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec

import java.nio.charset.StandardCharsets
import scala.util.{Failure, Success}


class SizeReducerTest extends AnyWordSpec with Matchers {


  "A Size Reducer reducing a ByteString" when {
    "not reducing a ByteString" should {
      "yield the same ByteString" in new ByteStringReducing {
        shortenByteString(maximumLength, reduce = false, None, shortByteString) mustBe Success(shortResult)
      }
    }

    "reducing a passable ByteString" should {
      "yield the same ByteString" in new ByteStringReducing {
        shortenByteString(maximumLength, reduce = true, None, shortByteString) mustBe Success(shortResult)
      }
    }

    "reducing a too long ByteString" should {
      "yield the shortened ByteString" in new ByteStringReducing {
        shortenByteString(maximumLength, reduce = true, None, longByteString) mustBe Success(longResult)
      }
    }
  }


  "A Size Reducer reducing a String" when {
    "not reducing a String" should {
      "yield the same String" in new StringReducing {
        shortenString(maximumLength, reduce = false, None, shortString) mustBe Success(shortResult)
      }
    }

    "reducing a passable String" should {
      "yield the same String" in new StringReducing {
        shortenString(maximumLength, reduce = true, None, shortString) mustBe Success(shortResult)

      }
    }

    "reducing a too long String" should {
      "yield the shortened String" in new StringReducing {
        shortenString(maximumLength, reduce = true, None, longString) mustBe Success(longResult)
      }
    }
  }


  "A Size Reducer reducing a JSON String" when {
    "not reducing a JSON String" should {
      "yield the same JSON String" in new JSONStringReducing {
        shortenJSONString(maximumLength, reduce = false, None, shortString) mustBe Success(shortResultPassthrough)
      }
    }

    "reducing a passable JSON String" should {
      "yield the same JSON String" in new JSONStringReducing {
        shortenJSONString(maximumLength, reduce = true, None, shortString) mustBe Success(shortResult)

      }
    }

    "reducing a too long JSON String" should {
      "yield the shortened JSON String" in new JSONStringReducing {
        shortenJSONString(maximumLength, reduce = true, None, longString) mustBe Success(longResult)
      }
    }
  }


  "A Size Reducer reducing a JSON Array" when {
    "not reducing a JSON Array" should {
      "yield the same JSON Array" in new JSONArrayReducing {
        shortenJSONArray(maximumLength, None, reduce = false, None, None, shortArraySigned) mustBe Success(shortArraySignedResultPassthrough)
        shortenJSONArray(maximumLength, None, reduce = false, None, None, shortArrayUnsigned) mustBe Success(shortArrayUnsignedResultPassthrough)
        shortenJSONArray(maximumLength, None, reduce = false, None, None, shortArrayHex) mustBe Success(shortArrayHexResultPassthrough)
        shortenJSONArray(maximumLength, None, reduce = false, None, None, arrayWithShortString) mustBe Success(arrayWithShortStringResultPassthrough)

        shortenJSONArray(maximumLength, maximumWholeLength, reduce = false, None, None, shortArraySigned) mustBe Success(shortArraySignedResultPassthrough)
        shortenJSONArray(maximumLength, maximumWholeLength, reduce = false, None, None, shortArrayUnsigned) mustBe Success(shortArrayUnsignedResultPassthrough)
        shortenJSONArray(maximumLength, maximumWholeLength, reduce = false, None, None, shortArrayHex) mustBe Success(shortArrayHexResultPassthrough)
        shortenJSONArray(maximumLength, maximumWholeLength, reduce = false, None, None, arrayWithShortString) mustBe Success(arrayWithShortStringResultPassthrough)
      }
    }

    "reducing a passable JSON Array" should {
      "yield the same JSON Array" in new JSONArrayReducing {
        shortenJSONArray(maximumLength, None, reduce = true, None, None, shortArraySigned) mustBe Success(shortArraySignedResult)
        shortenJSONArray(maximumLength, None, reduce = true, None, None, shortArrayUnsigned) mustBe Success(shortArrayUnsignedResult)
        shortenJSONArray(maximumLength, None, reduce = true, None, None, shortArrayHex) mustBe Success(shortArrayHexResult)
        shortenJSONArray(maximumLength, None, reduce = true, None, None, arrayWithShortString) mustBe Success(arrayWithShortStringResult)

        shortenJSONArray(maximumLength, maximumWholeLength, reduce = true, None, None, shortArraySigned) mustBe Success(shortArraySignedResult)
        shortenJSONArray(maximumLength, maximumWholeLength, reduce = true, None, None, shortArrayUnsigned) mustBe Success(shortArrayUnsignedResult)
        shortenJSONArray(maximumLength, maximumWholeLength, reduce = true, None, None, shortArrayHex) mustBe Success(shortArrayHexResult)
        shortenJSONArray(maximumLength, maximumWholeLength, reduce = true, None, None, arrayWithShortString) mustBe Success(arrayWithShortStringResult)
      }
    }

    "reducing a too long JSON Array" should {
      "yield the shortened JSON Array" in new JSONArrayReducing {
        shortenJSONArray(maximumLength, None, reduce = true, None, None, longArraySigned) mustBe Success(longArraySignedResult)
        shortenJSONArray(maximumLength, None, reduce = true, None, None, longArrayUnsigned) mustBe Success(longArrayUnsignedResult)
        shortenJSONArray(maximumLength, None, reduce = true, None, None, longArrayHex) mustBe Success(longArrayHexResult)
        shortenJSONArray(maximumLength, None, reduce = true, None, None, arrayWithLongString) mustBe Success(arrayWithLongStringResult)

        shortenJSONArray(maximumLength, maximumWholeLength, reduce = true, None, None, longArraySigned) mustBe Success(longArraySignedResult)
        shortenJSONArray(maximumLength, maximumWholeLength, reduce = true, None, None, longArrayUnsigned) mustBe Success(longArrayUnsignedResult)
        shortenJSONArray(maximumLength, maximumWholeLength, reduce = true, None, None, longArrayHex) mustBe Success(longArrayHexResult)
        shortenJSONArray(maximumLength, maximumWholeLength, reduce = true, None, None, arrayWithLongString) mustBe Success(arrayWithLongStringResult)
      }
    }


    "reducing a mixed JSON Array" should {
      "yield the correct JSON Array" in new JSONMixedArrayReducing {
        shortenJSONArray(maximumLength, None, reduce = true, None, None, mixedArray) mustBe Success(mixedArrayResult)
        shortenJSONArray(maximumLength, None, reduce = true, None, None, mixedDeeperArray) mustBe Success(mixedDeeperArrayResult)

        shortenJSONArray(maximumLength, maximumWholeLength, reduce = true, None, None, mixedArray) mustBe Success(mixedArrayResult)
        shortenJSONArray(maximumLength, maximumWholeLength, reduce = true, None, None, mixedDeeperArray) mustBe Success(mixedDeeperArrayResult)
      }
    }
  }


  "A Size Reducer reducing a JSON Object" when {
    "not reducing a JSON Object" should {
      "yield the same JSON Object" in new JSONObjectReducing {
        shortenJSONObject(maximumLength, None, reduce = false, None, None, shortObject) mustBe Success(shortObjectResultPassthrough)
        shortenJSONObject(maximumLength, maximumWholeLength, reduce = false, None, None, shortObject) mustBe Success(shortObjectResultPassthrough)
      }
    }

    "reducing a passable JSON Object" should {
      "yield the same JSON Object" in new JSONObjectReducing {
        shortenJSONObject(maximumLength, None, reduce = true, None, None, shortObject) mustBe Success(shortObjectResult)
        shortenJSONObject(maximumLength, maximumWholeLength, reduce = true, None, None, shortObject) mustBe Success(shortObjectResult)
      }
    }

    "reducing a too long JSON Object" should {
      "yield the shortened JSON Object" in new JSONObjectReducing {
        shortenJSONObject(maximumLength, None, reduce = true, None, None, longObject) mustBe Success(longObjectResult)
        shortenJSONObject(maximumLength, maximumWholeLength, reduce = true, None, None, longObject) mustBe Success(longObjectResult)
      }
    }

    "reducing a mixed JSON Object" should {
      "yield the correct JSON Object" in new JSONMixedObjectReducing {
        shortenJSONObject(maximumLength, None, reduce = true, None, None, deeperObject) mustBe Success(deeperObjectResult)
        shortenJSONObject(maximumLength, None, reduce = true, None, None, deeperLongObject) mustBe Success(deeperLongObjectResult)

        shortenJSONObject(maximumLength, maximumWholeLength, reduce = true, None, None, deeperObject) mustBe Success(deeperObjectResult)
        shortenJSONObject(maximumLength, maximumWholeLength, reduce = true, None, None, deeperLongObject) mustBe Success(deeperLongObjectResult)
      }
    }

    "reducing with a frame length limitation" should {
      "yield the correct JSON Array" in new JSONArrayReducingWithFrameLength {
        shortenJSONArray(length2, maxLength22, reduce = true, None, None, flatArray) mustBe Success(flatArrayResult)
        shortenJSONArray(length5, maxLength20, reduce = true, None, None, flatArray2) mustBe Success(flatArray2Result)
        shortenJSONArray(length3, maxLength40, reduce = true, None, None, deepArray1) mustBe Success(deepArray1Result)
        shortenJSONArray(length3, maxLength70, reduce = true, None, None, deepArray2) mustBe Success(deepArray2Result)
        shortenJSONArray(length3, maxLength40, reduce = true, None, None, deepArray3) mustBe Success(deepArray3Result)
      }


      "yield the correct JSON Object" in new JSONObjectReducingWithFrameLength {
        shortenJSONObject(length2, maxLength40, reduce = true, None, None, flatObject) mustBe Success(flatObjectResult)
        shortenJSONObject(length5, maxLength40, reduce = true, None, None, flatObject2) mustBe Success(flatObject2Result)
        shortenJSONObject(length3, maxLength60, reduce = true, None, None, deepObject1) mustBe Success(deepObject1Result)
        shortenJSONObject(length3, maxLength110, reduce = true, None, None, deepObject2) mustBe Success(deepObject2Result)
        shortenJSONObject(length3, maxLength40, reduce = true, None, None, deepObject3) mustBe Success(deepObject3Result)
      }
    }
  }


  "A Size Reducer reducing a JSON" when {
    "not reducing a JSON Object" should {
      "yield the same JSON Object" in new JSONMixedReducing {
        shortenJSONValue(maximumLength, None, reduce = false, None, None, mixedJString) mustBe Success(mixedJStringResult)
        shortenJSONValue(maximumLength, None, reduce = false, None, None, mixedJInt) mustBe Success(mixedJIntResult)
        shortenJSONValue(maximumLength, None, reduce = false, None, None, mixedObject) mustBe Success(mixedObjectResult)
        shortenJSONValue(maximumLength, None, reduce = false, None, None, mixedJString) mustBe Success(mixedJStringResult)
        shortenJSONValue(maximumLength, maximumWholeLengthShort, reduce = false, None, None, mixedJInt) mustBe Success(mixedJIntResult)
        shortenJSONValue(maximumLength, maximumWholeLengthShort, reduce = false, None, None, mixedArray) mustBe Success(mixedArrayResult)
        shortenJSONValue(maximumLength, maximumWholeLength, reduce = false, None, None, mixedJInt) mustBe Success(mixedJIntResult)
        shortenJSONValue(maximumLength, maximumWholeLength, reduce = false, None, None, mixedArray) mustBe Success(mixedArrayResult)
        shortenJSONValue(maximumLength, maximumWholeLength, reduce = false, None, None, mixedObject) mustBe Success(mixedObjectResult)
        shortenJSONValue(maximumLength, maximumWholeLength, reduce = false, None, None, mixedArray) mustBe Success(mixedArrayResult)
      }
    }

    "reducing a passable mixed deep JSON" should {
      "yield the same mixed deep JSON" in new JSONMixedReducing {
        shortenJSONValue(maximumLength, None, reduce = true, None, None, mixedJString) mustBe Success(mixedJStringResult)
        shortenJSONValue(maximumLength, None, reduce = true, None, None, mixedJInt) mustBe Success(mixedJIntResult)
        shortenJSONValue(maximumLength, None, reduce = true, None, None, mixedObject) mustBe Success(mixedObjectResult)
        shortenJSONValue(maximumLength, None, reduce = true, None, None, mixedArray) mustBe Success(mixedArrayResult)
        shortenJSONValue(maximumLength, maximumWholeLengthShort, reduce = true, None, None, mixedJString) mustBe Success(mixedJStringResult)
        shortenJSONValue(maximumLength, maximumWholeLengthShort, reduce = true, None, None, mixedJInt) mustBe Success(mixedJIntResult)
        shortenJSONValue(maximumLength, maximumWholeLength, reduce = true, None, None, mixedJString) mustBe Success(mixedJStringResult)
        shortenJSONValue(maximumLength, maximumWholeLength, reduce = true, None, None, mixedJInt) mustBe Success(mixedJIntResult)
        shortenJSONValue(maximumLength, maximumWholeLength, reduce = true, None, None, mixedObject) mustBe Success(mixedObjectResult)
        shortenJSONValue(maximumLength, maximumWholeLength, reduce = true, None, None, mixedArray) mustBe Success(mixedArrayResult)
      }
    }

    "reducing a too long mixed deep JSON" should {
      "yield the shortened mixed deep JSON" in new JSONMixedReducing {
        shortenJSONValue(maximumLength, None, reduce = true, None, None, longMixedJString) mustBe Success(longMixedJStringResult)
        shortenJSONValue(maximumLength, None, reduce = true, None, None, longMixedObject) mustBe Success(longMixedObjectResult)
        shortenJSONValue(maximumLength, None, reduce = true, None, None, longMixedArray) mustBe Success(longMixedArrayResult)

        shortenJSONValue(maximumLength, maximumWholeLengthShort, reduce = true, None, None, longMixedJString) mustBe Success(longMixedJStringResult)
        shortenJSONValue(maximumLength, maximumWholeLength, reduce = true, None, None, longMixedJString) mustBe Success(longMixedJStringResult)
        shortenJSONValue(maximumLength, maximumWholeLength, reduce = true, None, None, longMixedObject) mustBe Success(longMixedObjectResult)
        shortenJSONValue(maximumLength, maximumWholeLength, reduce = true, None, None, longMixedArray) mustBe Success(longMixedArrayResult)
      }
    }
  }


  "A Size Reducer encountering errors" when {
    "reducing a value with a negative maximum size" should {
      "yield the correct error" in new Errors {
        shortenString(negativeSize, reduce = true, None, null) must matchPattern { case Failure(MaximumLengthMayNotBeNegative(None)) => }
        shortenByteString(negativeSize, reduce = true, None, null) must matchPattern { case Failure(MaximumLengthMayNotBeNegative(None)) => }
        shortenJSONString(negativeSize, reduce = true, None, null) must matchPattern { case Failure(MaximumLengthMayNotBeNegative(None)) => }
        shortenJSONObject(negativeSize, None, reduce = true, None, None, null) must matchPattern { case Failure(MaximumLengthMayNotBeNegative(None)) => }
        shortenJSONArray(negativeSize, None, reduce = true, None, None, null) must matchPattern { case Failure(MaximumLengthMayNotBeNegative(None)) => }
        shortenJSONValue(negativeSize, None, reduce = true, None, None, null) must matchPattern { case Failure(MaximumLengthMayNotBeNegative(None)) => }
        shortenJSONObject(negativeSize, maximumWholeLength, reduce = true, None, None, null) must matchPattern { case Failure(MaximumLengthMayNotBeNegative(None)) => }
        shortenJSONArray(negativeSize, maximumWholeLength, reduce = true, None, None, null) must matchPattern { case Failure(MaximumLengthMayNotBeNegative(None)) => }
        shortenJSONValue(negativeSize, maximumWholeLength, reduce = true, None, None, null) must matchPattern { case Failure(MaximumLengthMayNotBeNegative(None)) => }
      }
    }

    "reducing a value with a zero maximum size" should {
      "yield the correct error" in new Errors {
        shortenString(zeroSize, reduce = true, None, null) must matchPattern { case Failure(MaximumLengthMayNotBeZero(None)) => }
        shortenByteString(zeroSize, reduce = true, None, null) must matchPattern { case Failure(MaximumLengthMayNotBeZero(None)) => }
        shortenJSONString(zeroSize, reduce = true, None, null) must matchPattern { case Failure(MaximumLengthMayNotBeZero(None)) => }
        shortenJSONObject(zeroSize, None, reduce = true, None, None, null) must matchPattern { case Failure(MaximumLengthMayNotBeZero(None)) => }
        shortenJSONArray(zeroSize, None, reduce = true, None, None, null) must matchPattern { case Failure(MaximumLengthMayNotBeZero(None)) => }
        shortenJSONValue(zeroSize, None, reduce = true, None, None, null) must matchPattern { case Failure(MaximumLengthMayNotBeZero(None)) => }
        shortenJSONObject(zeroSize, maximumWholeLength, reduce = true, None, None, null) must matchPattern { case Failure(MaximumLengthMayNotBeZero(None)) => }
        shortenJSONArray(zeroSize, maximumWholeLength, reduce = true, None, None, null) must matchPattern { case Failure(MaximumLengthMayNotBeZero(None)) => }
        shortenJSONValue(zeroSize, maximumWholeLength, reduce = true, None, None, null) must matchPattern { case Failure(MaximumLengthMayNotBeZero(None)) => }
      }
    }

    "encountering an unexpected error" should {
      "yield the correct error" in new Errors {
        shortenString(maximumLength, reduce = true, None, null) must matchPattern { case Failure(UnexpectedErrorDuringReduction(_, _, _, _, _)) => }
        shortenByteString(maximumLength, reduce = true, None, null) must matchPattern { case Failure(UnexpectedErrorDuringReduction(_, _, _, _, _)) => }
        shortenJSONString(maximumLength, reduce = true, None, null) must matchPattern { case Failure(UnexpectedErrorDuringReduction(_, _, _, _, _)) => }
        shortenJSONObject(maximumLength, None, reduce = true, None, None, null) must matchPattern { case Failure(UnexpectedErrorDuringReduction(_, _, _, _, _)) => }
        shortenJSONArray(maximumLength, None, reduce = true, None, None, null) must matchPattern { case Failure(UnexpectedErrorDuringReduction(_, _, _, _, _)) => }
        shortenJSONValue(maximumLength, None, reduce = true, None, None, JArray(null)) must matchPattern { case Failure(UnexpectedErrorDuringReduction(_, _, _, _, _)) => }
        shortenJSONObject(maximumLength, maximumWholeLength, reduce = true, None, None, null) must matchPattern { case Failure(UnexpectedErrorDuringReduction(_, _, _, _, _)) => }
        shortenJSONArray(maximumLength, maximumWholeLength, reduce = true, None, None, null) must matchPattern { case Failure(UnexpectedErrorDuringReduction(_, _, _, _, _)) => }
        shortenJSONValue(maximumLength, maximumWholeLength, reduce = true, None, None, JArray(null)) must matchPattern { case Failure(UnexpectedErrorDuringReduction(_, _, _, _, _)) => }
      }
    }
  }


  trait BaseFixture extends SizeReducer {
    implicit val formats: Formats = SimplifierFormats.formats
    val maximumLength: Int = 5
    val maximumWholeLengthShort: Option[Int] = Some(1)
    val maximumWholeLength: Option[Int] = Some(Integer.MAX_VALUE)

    val shortValue: String = "Short"
    val longValue: String = "I am too long for this world"
    val shortenedValue: String = s"I am  (... shortened $maximumLength of ${longValue.length} [Byte])"
  }

  trait ByteStringReducing extends BaseFixture {
    val shortByteString: ByteString = ByteString(shortValue, StandardCharsets.UTF_8)
    val longByteString: ByteString = ByteString(longValue, StandardCharsets.UTF_8)


    val shortResult: OriginalResult[ByteString] = OriginalResult(
      reducedValue = shortByteString,
      originalSize = shortByteString.size
    )

    val longResult: ReductionResultByteString = ReductionResultByteString(
      originalByteString = longByteString,
      reducedByteString = longByteString.take(maximumLength)
    )
  }

  trait StringReducing extends BaseFixture {
    val shortString: String = shortValue
    val longString: String = longValue


    val shortResult: OriginalResult[String] = OriginalResult(
      reducedValue = shortString,
      originalSize = shortString.length
    )

    val longResult: ReductionResultString = ReductionResultString(
      originalString = longString,
      reducedString = shortenedValue
    )
  }

  trait JSONStringReducing extends BaseFixture {
    val shortString: JString = JString(shortValue)
    val longString: JString = JString(longValue)

    val shortResultPassthrough: OriginalResultJValue[JString] = OriginalResultJValue(
      reducedJValue = shortString,
      originalSize = shortValue.length
    )

    val shortResult: OriginalResultJValue[JString] = OriginalResultJValue(
      reducedJValue = shortString,
      originalSize = shortValue.length
    )

    val longResult: ReductionResultJString = ReductionResultJString(
      originalJString = longString,
      reducedJString = JString(shortenedValue)
    )
  }

  trait JSONArrayReducing extends BaseFixture with ByteArrayEncoding {
    val shortArraySigned: JArray = Signed.encode(shortValue.getBytes(StandardCharsets.UTF_8)).get.toJson.asInstanceOf[JArray]
    val shortArrayUnsigned: JArray = Unsigned.encode(shortValue.getBytes(StandardCharsets.UTF_8)).get.toJson.asInstanceOf[JArray]
    val shortArrayHex: JArray = Hex.encode(shortValue.getBytes(StandardCharsets.UTF_8)).get.toJson.asInstanceOf[JArray]


    val longArraySigned: JArray = Signed.encode(longValue.getBytes(StandardCharsets.UTF_8)).get.toJson.asInstanceOf[JArray]
    val longArrayUnsigned: JArray = Unsigned.encode(longValue.getBytes(StandardCharsets.UTF_8)).get.toJson.asInstanceOf[JArray]
    val longArrayHex: JArray = Hex.encode(longValue.getBytes(StandardCharsets.UTF_8)).get.toJson.asInstanceOf[JArray]


    val arrayWithShortString: JArray = JArray(List(JString(shortValue)))

    val arrayWithLongString: JArray = JArray(List(JString(longValue)))


    val shortenedArraySigned: JArray = JArray(Signed.encode(longValue.getBytes(StandardCharsets.UTF_8)).get.toJson.asInstanceOf[JArray].arr.take(maximumLength))
    val shortenedArrayUnsigned: JArray = JArray(Unsigned.encode(longValue.getBytes(StandardCharsets.UTF_8)).get.toJson.asInstanceOf[JArray].arr.take(maximumLength))
    val shortenedArrayHex: JArray = JArray(Hex.encode(longValue.getBytes(StandardCharsets.UTF_8)).get.toJson.asInstanceOf[JArray].arr.take(maximumLength))


    val shortenedArrayWithString: JArray = JArray(List(JString(shortenedValue)))

    val shortArraySignedResultPassthrough: OriginalResultJValue[JArray] = OriginalResultJValue(
      reducedJValue = shortArraySigned,
      originalSize = shortArraySigned.arr.size
    )
    val shortArrayUnsignedResultPassthrough: OriginalResultJValue[JArray] = OriginalResultJValue(
      reducedJValue = shortArrayUnsigned,
      originalSize = shortArrayUnsigned.arr.size
    )
    val shortArrayHexResultPassthrough: OriginalResultJValue[JArray] = OriginalResultJValue(
      reducedJValue = shortArrayHex,
      originalSize = shortArrayHex.arr.size
    )
    val arrayWithShortStringResultPassthrough: OriginalResultJValue[JArray] = OriginalResultJValue(
      reducedJValue = arrayWithShortString,
      originalSize = renderJSONCompact(arrayWithShortString).length
    )


    val shortArraySignedResult: OriginalResultJValue[JArray] = OriginalResultJValue(
      reducedJValue = shortArraySigned,
      originalSize = shortArraySigned.arr.size
    )
    val shortArrayUnsignedResult: OriginalResultJValue[JArray] = OriginalResultJValue(
      reducedJValue = shortArrayUnsigned,
      originalSize = shortArrayUnsigned.arr.size
    )
    val shortArrayHexResult: OriginalResultJValue[JArray] = OriginalResultJValue(
      reducedJValue = shortArrayHex,
      originalSize = shortArrayHex.arr.size
    )
    val arrayWithShortStringResult: OriginalResultJValue[JArray] = OriginalResultJValue(
      reducedJValue = arrayWithShortString,
      originalSize = renderJSONCompact(arrayWithShortString).length
    )

    val longArraySignedResult: ReductionResultJArray = ReductionResultJArray(
      originalArray = longArraySigned,
      reducedArray = shortenedArraySigned,
      changes = ReductionChanges(
        Seq(
          ReductionChange(
            index = None,
            key = None,
            `type` = JsonByteArrayEquivalentReducibleType(),
            originalSize = longArraySigned.arr.size,
            reducedSize = maximumLength,
            removedElement = None,
            changes = None
          )
        )
      )
    )
    val longArrayUnsignedResult: ReductionResultJArray = ReductionResultJArray(
      originalArray = longArrayUnsigned,
      reducedArray = shortenedArrayUnsigned,
      changes = ReductionChanges(
        Seq(
          reductionChanges.ReductionChange(
            index = None,
            key = None,
            `type` = JsonByteArrayEquivalentReducibleType(),
            originalSize = longArrayUnsigned.arr.size,
            reducedSize = maximumLength,
            removedElement = None,
            changes = None
          )
        )
      )
    )
    val longArrayHexResult: ReductionResultJArray = ReductionResultJArray(
      originalArray = longArrayHex,
      reducedArray = shortenedArrayHex,
      changes = ReductionChanges(
        Seq(
          reductionChanges.ReductionChange(
            index = None,
            key = None,
            `type` = JsonByteArrayEquivalentReducibleType(),
            originalSize = longArrayHex.arr.size,
            reducedSize = maximumLength,
            removedElement = None,
            changes = None
          )
        )
      )
    )
    val arrayWithLongStringResult: ReductionResultJArray = ReductionResultJArray(
      originalArray = arrayWithLongString,
      reducedArray = shortenedArrayWithString,
      changes = ReductionChanges(
        Seq(
          reductionChanges.ReductionChange(
            index = Some(0),
            key = None,
            `type` = JsonStringReducibleType(),
            originalSize = longValue.length,
            reducedSize = maximumLength,
            removedElement = None,
            changes = None
          )
        )
      )
    )
  }

  trait JSONMixedArrayReducing extends BaseFixture with ByteArrayEncoding {
    val mixedArray: JArray = JArray(List(JString(shortValue), JString(longValue)))


    val deepestArray: JArray = JArray(
      List(
        JString(shortValue),
        JString(shortValue),
        JString(shortValue),
        JNull,
        JString(longValue)
      )
    )
    val deepArray: JArray = JArray(
      List(
        JString(shortValue),
        JString(shortValue),
        JString(longValue),
        deepestArray
      )
    )
    val mixedDeeperArray: JArray = JArray(
      List(
        JString(shortValue),
        JString(longValue),
        deepArray
      )
    )


    val deepestArrayReduced: JArray = JArray(
      List(
        JString(shortValue),
        JString(shortValue),
        JString(shortValue),
        JNull,
        JString(shortenedValue)
      )
    )

    val deepArrayReduced: JArray = JArray(
      List(
        JString(shortValue),
        JString(shortValue),
        JString(shortenedValue),
        deepestArrayReduced
      )
    )


    val mixedReducedArray: JArray = JArray(List(JString(shortValue), JString(shortenedValue)))
    val mixedReducedDeeperArray: JArray = JArray(
      List(
        JString(shortValue),
        JString(shortenedValue),
        JArray(
          List(
            JString(shortValue),
            JString(shortValue),
            JString(shortenedValue),
            JArray(
              List(
                JString(shortValue),
                JString(shortValue),
                JString(shortValue),
                JNull,
                JString(shortenedValue)
              )
            )
          )
        )
      )
    )


    val mixedArrayResult: ReductionResultJArray = ReductionResultJArray(
      originalArray = mixedArray,
      reducedArray = mixedReducedArray,
      changes = ReductionChanges(
        Seq(
          reductionChanges.ReductionChange(
            index = Some(1),
            key = None,
            `type` = JsonStringReducibleType(),
            originalSize = longValue.length,
            reducedSize = maximumLength,
            removedElement = None,
            changes = None
          )
        )
      )
    )

    val mixedDeeperArrayResult: ReductionResultJArray = ReductionResultJArray(
      originalArray = mixedDeeperArray,
      reducedArray = mixedReducedDeeperArray,
      changes = ReductionChanges(
        Seq(
          reductionChanges.ReductionChange(
            index = Some(1),
            key = None,
            `type` = JsonStringReducibleType(),
            originalSize = longValue.length,
            reducedSize = maximumLength,
            removedElement = None,
            changes = None
          ),
          reductionChanges.ReductionChange(
            index = Some(2),
            key = None,
            `type` = JsonArrayReducibleType(),
            originalSize = renderJSONCompact(deepArray).length,
            reducedSize = determineSerializedLength(deepArrayReduced),
            removedElement = None,
            changes = Some(ReductionChanges(
              Seq(
                reductionChanges.ReductionChange(
                  index = Some(2),
                  key = None,
                  `type` = JsonStringReducibleType(),
                  originalSize = longValue.length,
                  reducedSize = maximumLength,
                  removedElement = None,
                  changes = None
                ),
                reductionChanges.ReductionChange(
                  index = Some(3),
                  key = None,
                  `type` = JsonArrayReducibleType(),
                  originalSize = renderJSONCompact(deepestArray).length,
                  reducedSize = determineSerializedLength(deepestArrayReduced),
                  removedElement = None,
                  changes = Some(ReductionChanges(
                    Seq(
                      reductionChanges.ReductionChange(
                        index = Some(4),
                        key = None,
                        `type` = JsonStringReducibleType(),
                        originalSize = longValue.length,
                        reducedSize = maximumLength,
                        removedElement = None,
                        changes = None
                      )
                    )
                  ))
                )
              )
            ))
          )
        )
      )
    )
  }

  trait JSONObjectReducing extends BaseFixture {
    val key1: String = "key1"
    val key2: String = "key2"
    val key3: String = "key3"
    val key4: String = "key4"


    val shortObject: JObject = JObject(
      JField(key1, JString(shortValue)),
      JField(key2, JString(shortValue)),
      JField(key3, JNull)
    )

    val longObject: JObject = JObject(
      JField(key1, JString(shortValue)),
      JField(key2, JString(longValue)),
      JField(key3, JNull),
      JField(key4, JString(longValue))
    )

    val shortenedObject: JObject = JObject(
      JField(key1, JString(shortValue)),
      JField(key2, JString(shortenedValue)),
      JField(key3, JNull),
      JField(key4, JString(shortenedValue))
    )

    val shortObjectResultPassthrough: OriginalResultJValue[JObject] = OriginalResultJValue(
      reducedJValue = shortObject,
      originalSize = renderJSONCompact(shortObject).length
    )

    val shortObjectResult: OriginalResultJValue[JObject] = OriginalResultJValue(
      reducedJValue = shortObject,
      originalSize = renderJSONCompact(shortObject).length
    )

    val longObjectResult: ReductionResultJObject = ReductionResultJObject(
      originalObject = longObject,
      reducedObject = shortenedObject,
      changes = ReductionChanges(
        Seq(
          reductionChanges.ReductionChange(
            index = None,
            key = Some(key2),
            `type` = JsonStringReducibleType(),
            originalSize = longValue.length,
            reducedSize = maximumLength,
            removedElement = None,
            changes = None
          ),
          reductionChanges.ReductionChange(
            index = None,
            key = Some(key4),
            `type` = JsonStringReducibleType(),
            originalSize = longValue.length,
            reducedSize = maximumLength,
            removedElement = None,
            changes = None
          )
        )
      )
    )
  }

  trait JSONMixedObjectReducing extends BaseFixture {

    val key1: String = "key1"
    val key2: String = "key2"
    val key3: String = "key3"
    val key4: String = "key4"

    val key11: String = "key11"
    val key12: String = "key12"
    val key13: String = "key13"
    val key14: String = "key14"

    val key21: String = "key21"
    val key22: String = "key22"
    val key23: String = "key23"
    val key24: String = "key24"


    val shortObject2: JObject = JObject(
      JField(key21, JString(shortValue)),
      JField(key22, JString(shortValue)),
      JField(key23, JNull),
      JField(key24, JNull)
    )


    val shortObject: JObject = JObject(
      JField(key11, JString(shortValue)),
      JField(key12, JString(shortValue)),
      JField(key13, shortObject2),
      JField(key14, JNull)
    )

    val deeperObject: JObject = JObject(
      JField(key1, JString(shortValue)),
      JField(key2, JString(shortValue)),
      JField(key3, JNull),
      JField(key4, shortObject)
    )


    val longObject2: JObject = JObject(
      JField(key21, JString(longValue)),
      JField(key22, JString(shortValue)),
      JField(key23, JNull),
      JField(key24, JNull)
    )


    val longObject2Reduced: JObject = JObject(
      JField(key21, JString(shortenedValue)),
      JField(key22, JString(shortValue)),
      JField(key23, JNull),
      JField(key24, JNull)
    )


    val longObject: JObject = JObject(
      JField(key11, JString(longValue)),
      JField(key12, JString(shortValue)),
      JField(key13, longObject2),
      JField(key14, JNull)
    )

    val longObjectReduced: JObject = JObject(
      JField(key11, JString(shortenedValue)),
      JField(key12, JString(shortValue)),
      JField(key13, longObject2Reduced),
      JField(key14, JNull)
    )


    val deeperLongObject: JObject = JObject(
      JField(key1, JString(shortValue)),
      JField(key2, JString(shortValue)),
      JField(key3, JNull),
      JField(key4, longObject)
    )


    val shortenedObject2: JObject = JObject(
      JField(key21, JString(shortenedValue)),
      JField(key22, JString(shortValue)),
      JField(key23, JNull),
      JField(key24, JNull)
    )


    val shortenedObject: JObject = JObject(
      JField(key11, JString(shortenedValue)),
      JField(key12, JString(shortValue)),
      JField(key13, shortenedObject2),
      JField(key14, JNull)
    )


    val shortenedLongObject: JObject = JObject(
      JField(key1, JString(shortValue)),
      JField(key2, JString(shortValue)),
      JField(key3, JNull),
      JField(key4, shortenedObject)
    )


    val deeperObjectResult: OriginalResultJValue[JObject] = OriginalResultJValue(
      reducedJValue = deeperObject,
      originalSize = renderJSONCompact(deeperObject).length
    )

    val deeperLongObjectResult: ReductionResultJObject = ReductionResultJObject(
      originalObject = deeperLongObject,
      reducedObject = shortenedLongObject,
      changes = ReductionChanges(
        Seq(
          reductionChanges.ReductionChange(
            index = None,
            key = Some(key4),
            `type` = JsonObjectReducibleType(),
            originalSize = renderJSONCompact(longObject).length,
            reducedSize = determineSerializedLength(longObjectReduced),
            removedElement = None,
            changes = Some(ReductionChanges(
              Seq(
                reductionChanges.ReductionChange(
                  index = None,
                  key = Some(key11),
                  `type` = JsonStringReducibleType(),
                  originalSize = longValue.length,
                  reducedSize = maximumLength,
                  removedElement = None,
                  changes = None),
                reductionChanges.ReductionChange(
                  index = None,
                  key = Some(key13),
                  `type` = JsonObjectReducibleType(),
                  originalSize = renderJSONCompact(longObject2).length,
                  reducedSize = determineSerializedLength(longObject2Reduced),
                  removedElement = None,
                  changes = Some(ReductionChanges(
                    Seq(
                      reductionChanges.ReductionChange(
                        index = None,
                        key = Some(key21),
                        `type` = JsonStringReducibleType(),
                        originalSize = longValue.length,
                        reducedSize = maximumLength,
                        removedElement = None,
                        changes = None))
                  ))
                )
              )
            ))
          )
        )
      )

    )
  }


  trait JSONArrayReducingWithFrameLength extends BaseFixture {
    val length2: Int = 2
    val length3: Int = 3
    val length5: Int = 5
    val maxLength20: Option[Int] = Some(20)
    val maxLength22: Option[Int] = Some(22)
    val maxLength40: Option[Int] = Some(40)
    val maxLength70: Option[Int] = Some(70)
    val string2: JString = JString("12")
    val string3: JString = JString("123")
    val string5: JString = JString("12345")
    val string7: JString = JString("1234567")
    val string10: JString = JString("1234567890")
    val string100: JString = {
      JString((for (_ <- 0 until 100) yield "#").mkString(""))
    }

    def shortenedString(string: String, length: Int): String = s"${string.take(length)} (... shortened $length of ${string.length} [Byte])"

    val flatArray: JArray = JArray(
      List(
        string10,
        string7,
        string3,
        string2
      )
    )
    val flatArray2: JArray = JArray(
      List(
        string5,
        string5,
        string7,
        string3
      )
    )

    val arrayTooBig: JArray = JArray(
      List(
        string10,
        string10,
        string100,
        string3
      )
    )

    val deepArray1: JArray = JArray(
      List(
        string3,
        arrayTooBig,
        string3
      )
    )

    val deepArrayTooBig2: JArray = JArray(
      List(
        string3,
        deepArray1
      )
    )

    val deepArray2: JArray = JArray(
      List(
        string3,
        deepArrayTooBig2,
        string10
      )
    )

    val deepArray3: JArray = JArray(
      List(
        string3,
        deepArray2
      )
    )


    val flatArrayReduced: JArray = {
      JArray(
        List(
          JString(shortenedString(string10.s, length2)),
          JString(shortenedString(string7.s, length2)),
          JString(shortenedString(string3.s, length2)),
          string2
        )
      )
    }


    val flatArray2Reduced: JArray = JArray(
      List(
        JNothing,
        string5,
        JNothing,
        string3
      )
    )


    val arrayTooBigReduced: JArray = JArray(
      List(
        JString(shortenedString(string10.s, length3)),
        JString(shortenedString(string10.s, length3)),
        JString(shortenedString(string100.s, length3)),
        string3
      )
    )

    val deepArray1Reduced: JArray = JArray(
      List(
        string3,
        arrayTooBigReduced,
        string3
      )
    )


    val deepArrayTooBig2Reduced: JArray = JArray(
      List(
        string3,
        deepArray1Reduced
      )
    )


    val deepArray2Reduced: JArray = JArray(
      List(
        string3,
        deepArrayTooBig2Reduced,
        JString(shortenedString(string10.s, length3)),
      )
    )


    val deepArray3Reduced: JArray = JArray(
      List(
        string3,
        JNothing
      )
    )

    val flatArrayResult: ReductionResultJArray = ReductionResultJArray(
      originalArray = flatArray,
      reducedArray = flatArrayReduced,
      changes = ReductionChanges(
        Seq(
          ReductionChange(
            index = Some(0),
            key = None,
            `type` = JsonStringReducibleType(),
            originalSize = string10.s.length,
            reducedSize = length2,
            removedElement = None,
            changes = None
          ),
          ReductionChange(
            index = Some(1),
            key = None,
            `type` = JsonStringReducibleType(),
            originalSize = string7.s.length,
            reducedSize = length2,
            removedElement = None,
            changes = None
          ),
          ReductionChange(
            index = Some(2),
            key = None,
            `type` = JsonStringReducibleType(),
            originalSize = string3.s.length,
            reducedSize = length2,
            removedElement = None,
            changes = None
          )
        )
      )
    )

    val flatArray2Result: ReductionResultJArray = ReductionResultJArray(
      originalArray = flatArray2,
      reducedArray = flatArray2Reduced,
      changes = ReductionChanges(
        Seq(
          ReductionChange(
            index = Some(0),
            key = None,
            `type` = JsonStringReducibleType(),
            originalSize = string5.s.length,
            reducedSize = 0,
            removedElement = Some(RemovedElement()),
            changes = None
          ),
          ReductionChange(
            index = Some(2),
            key = None,
            `type` = JsonStringReducibleType(),
            originalSize = string7.s.length,
            reducedSize = 0,
            removedElement = Some(RemovedElement()),
            changes = None
          )
        )
      )
    )

    val deepArray1Result: ReductionResultJArray = ReductionResultJArray(
      originalArray = deepArray1,
      reducedArray = deepArray1Reduced,
      changes = ReductionChanges(
        Seq(
          ReductionChange(
            index = Some(1),
            key = None,
            `type` = JsonArrayReducibleType(),
            originalSize = determineSerializedLength(arrayTooBig),
            reducedSize = determineSerializedLength(arrayTooBigReduced),
            removedElement = None,
            changes = Some(
              ReductionChanges(
                Seq(
                  ReductionChange(
                    index = Some(0),
                    key = None,
                    `type` = JsonStringReducibleType(),
                    originalSize = string10.s.length,
                    reducedSize = length3,
                    removedElement = None,
                    changes = None
                  ),
                  ReductionChange(
                    index = Some(1),
                    key = None,
                    `type` = JsonStringReducibleType(),
                    originalSize = string10.s.length,
                    reducedSize = length3,
                    removedElement = None,
                    changes = None
                  ),
                  ReductionChange(
                    index = Some(2),
                    key = None,
                    `type` = JsonStringReducibleType(),
                    originalSize = string100.s.length,
                    reducedSize = length3,
                    removedElement = None,
                    changes = None
                  )
                )
              )
            )
          )
        )
      )
    )

    val deepArray2Result: ReductionResultJArray = ReductionResultJArray(
      originalArray = deepArray2,
      reducedArray = deepArray2Reduced,
      changes = ReductionChanges(
        Seq(
          ReductionChange(
            index = Some(1),
            key = None,
            `type` = JsonArrayReducibleType(),
            originalSize = determineSerializedLength(deepArrayTooBig2),
            reducedSize = determineSerializedLength(deepArrayTooBig2Reduced),
            removedElement = None,
            changes = Some(ReductionChanges(
              Seq(
                ReductionChange(
                  index = Some(1),
                  key = None,
                  `type` = JsonArrayReducibleType(),
                  originalSize = determineSerializedLength(deepArray1),
                  reducedSize = determineSerializedLength(deepArray1Reduced),
                  removedElement = None,
                  changes = Some(
                    ReductionChanges(
                      Seq(
                        ReductionChange(
                          index = Some(1),
                          key = None,
                          `type` = JsonArrayReducibleType(),
                          originalSize = determineSerializedLength(arrayTooBig),
                          reducedSize = determineSerializedLength(arrayTooBigReduced),
                          removedElement = None,
                          changes = Some(
                            ReductionChanges(
                              Seq(
                                ReductionChange(
                                  index = Some(0),
                                  key = None,
                                  `type` = JsonStringReducibleType(),
                                  originalSize = string10.s.length,
                                  reducedSize = length3,
                                  removedElement = None,
                                  changes = None
                                ),
                                ReductionChange(
                                  index = Some(1),
                                  key = None,
                                  `type` = JsonStringReducibleType(),
                                  originalSize = string10.s.length,
                                  reducedSize = length3,
                                  removedElement = None,
                                  changes = None
                                ),
                                ReductionChange(
                                  index = Some(2),
                                  key = None,
                                  `type` = JsonStringReducibleType(),
                                  originalSize = string100.s.length,
                                  reducedSize = length3,
                                  removedElement = None,
                                  changes = None
                                )
                              )
                            )
                          )
                        )
                      )
                    )
                  )
                )
              )
            )
            )
          ),
          ReductionChange(
            index = Some(2),
            key = None,
            `type` = JsonStringReducibleType(),
            originalSize = string10.s.length,
            reducedSize = length3,
            removedElement = None,
            changes = None
          )
        )
      )
    )

    val deepArray3Result: ReductionResultJArray = ReductionResultJArray(
      originalArray = deepArray3,
      reducedArray = deepArray3Reduced,
      changes = ReductionChanges(
        Seq(
          ReductionChange(
            index = Some(1),
            key = None,
            `type` = JsonArrayReducibleType(),
            originalSize = determineSerializedLength(deepArray2),
            reducedSize = 0,
            removedElement = Some(
              RemovedElement(
                Seq(0, 1, 2)
              )
            ),
            changes = None
          )
        )
      )
    )
  }

  trait JSONObjectReducingWithFrameLength extends JSONArrayReducingWithFrameLength {
    val maxLength60: Option[Int] = Some(60)
    val maxLength80: Option[Int] = Some(80)
    val maxLength110: Option[Int] = Some(110)

    val key1: String = "1"
    val key2: String = "2"
    val key3: String = "3"
    val key4: String = "4"

    val keyDeep1: String = "1-1"
    val keyDeep2: String = "1-2"
    val keyDeep3: String = "1-3"

    val keyDeeper1: String = "1-1-1"
    val keyDeeper2: String = "1-1-2"

    val keyDeepest1: String = "1-1-1-1"
    val keyDeepest2: String = "1-1-1-2"
    val keyDeepest3: String = "1-1-1-3"
    val keyDeepest4: String = "1-1-1-4"


    val flatObject: JObject = JObject(
      JField(key1, string10),
      JField(key2, string7),
      JField(key3, string3),
      JField(key4, string2)
    )
    val flatObject2: JObject = JObject(
      JField(key1, string5),
      JField(key2, string5),
      JField(key3, string7),
      JField(key4, string3)
    )

    val objectTooBig: JObject = JObject(
      JField(key1, string10),
      JField(key2, string10),
      JField(key3, string100),
      JField(key4, string3)
    )

    val deepObject1: JObject = JObject(
      JField(key1, string3),
      JField(key2, arrayTooBig),
      JField(key3, string3)
    )

    val deepObjectTooBig2: JObject = JObject(
      JField(keyDeeper1, string3),
      JField(keyDeeper2, deepObject1)
    )

    val deepObject2: JObject = JObject(
      JField(keyDeep1, string3),
      JField(keyDeep2, deepObjectTooBig2),
      JField(keyDeep3, string10)
    )

    val deepObject3: JObject = JObject(
      JField(keyDeep1, string3),
      JField(keyDeep2, deepObject2)
    )


    val flatObjectReduced: JObject = JObject(
      JField(key1, JString(shortenedString(string10.s, length2))),
      JField(key2, JString(shortenedString(string7.s, length2))),
      JField(key3, JString(shortenedString(string3.s, length2))),
      JField(key4, string2)
    )


    val flatObject2Reduced: JObject = JObject(
      JField(key1, JNothing),
      JField(key2, string5),
      JField(key3, JNothing),
      JField(key4, string3)
    )


    val deepObject1Reduced: JObject = JObject(
      JField(key1, string3),
      JField(key2, arrayTooBigReduced),
      JField(key3, string3)
    )


    val deepObjectTooBig2Reduced: JObject = JObject(
      JField(keyDeeper1, string3),
      JField(keyDeeper2, deepObject1Reduced)
    )

    val deepObject2Reduced: JObject = JObject(
      JField(keyDeep1, string3),
      JField(keyDeep2, deepObjectTooBig2Reduced),
      JField(keyDeep3, JString(shortenedString(string10.s, length3)))
    )

    val deepObject3Reduced: JObject = JObject(
      JField(keyDeep1, string3),
      JField(keyDeep2, JNothing)
    )


    val flatObjectResult: ReductionResultJObject = ReductionResultJObject(
      originalObject = flatObject,
      reducedObject = flatObjectReduced,
      changes = ReductionChanges(
        Seq(
          ReductionChange(
            index = None,
            key = Some(key1),
            `type` = JsonStringReducibleType(),
            originalSize = string10.s.length,
            reducedSize = length2,
            removedElement = None,
            changes = None
          ),
          ReductionChange(
            index = None,
            key = Some(key2),
            `type` = JsonStringReducibleType(),
            originalSize = string7.s.length,
            reducedSize = length2,
            removedElement = None,
            changes = None
          ),
          ReductionChange(
            index = None,
            key = Some(key3),
            `type` = JsonStringReducibleType(),
            originalSize = string3.s.length,
            reducedSize = length2,
            removedElement = None,
            changes = None
          )
        )
      )
    )

    val flatObject2Result: ReductionResultJObject = ReductionResultJObject(
      originalObject = flatObject2,
      reducedObject = flatObject2Reduced,
      changes = ReductionChanges(
        Seq(
          ReductionChange(
            index = None,
            key = Some(key1),
            `type` = JsonStringReducibleType(),
            originalSize = string5.s.length,
            reducedSize = 0,
            removedElement = Some(RemovedElement()),
            changes = None
          ),
          ReductionChange(
            index = None,
            key = Some(key3),
            `type` = JsonStringReducibleType(),
            originalSize = string7.s.length,
            reducedSize = 0,
            removedElement = Some(RemovedElement()),
            changes = None
          )
        )
      )

    )

    val deepObject1Result: ReductionResultJObject = ReductionResultJObject(
      originalObject = deepObject1,
      reducedObject = deepObject1Reduced,
      changes = ReductionChanges(
        Seq(
          ReductionChange(
            index = None,
            key = Some(key2),
            `type` = JsonArrayReducibleType(),
            originalSize = determineSerializedLength(arrayTooBig),
            reducedSize = determineSerializedLength(arrayTooBigReduced),
            removedElement = None,
            changes = Some(
              ReductionChanges(
                Seq(
                  ReductionChange(
                    index = Some(0),
                    key = None,
                    `type` = JsonStringReducibleType(),
                    originalSize = string10.s.length,
                    reducedSize = length3,
                    removedElement = None,
                    changes = None
                  ),
                  ReductionChange(
                    index = Some(1),
                    key = None,
                    `type` = JsonStringReducibleType(),
                    originalSize = string10.s.length,
                    reducedSize = length3,
                    removedElement = None,
                    changes = None
                  ),
                  ReductionChange(
                    index = Some(2),
                    key = None,
                    `type` = JsonStringReducibleType(),
                    originalSize = string100.s.length,
                    reducedSize = length3,
                    removedElement = None,
                    changes = None
                  )
                )
              )
            )
          )
        )
      )
    )

    val deepObject2Result: ReductionResultJObject = ReductionResultJObject(
      originalObject = deepObject2,
      reducedObject = deepObject2Reduced,
      changes = ReductionChanges(
        Seq(
          ReductionChange(
            index = None,
            key = Some(keyDeep2),
            `type` = JsonObjectReducibleType(),
            originalSize = determineSerializedLength(deepObjectTooBig2),
            reducedSize = determineSerializedLength(deepObjectTooBig2Reduced),
            removedElement = None,
            changes = Some(ReductionChanges(
              Seq(
                ReductionChange(
                  index = None,
                  key = Some(keyDeeper2),
                  `type` = JsonObjectReducibleType(),
                  originalSize = determineSerializedLength(deepObject1),
                  reducedSize = determineSerializedLength(deepObject1Reduced),
                  removedElement = None,
                  changes = Some(
                    ReductionChanges(
                      Seq(
                        ReductionChange(
                          index = None,
                          key = Some(key2),
                          `type` = JsonArrayReducibleType(),
                          originalSize = determineSerializedLength(arrayTooBig),
                          reducedSize = determineSerializedLength(arrayTooBigReduced),
                          removedElement = None,
                          changes = Some(
                            ReductionChanges(
                              Seq(
                                ReductionChange(
                                  index = Some(0),
                                  key = None,
                                  `type` = JsonStringReducibleType(),
                                  originalSize = string10.s.length,
                                  reducedSize = length3,
                                  removedElement = None,
                                  changes = None
                                ),
                                ReductionChange(
                                  index = Some(1),
                                  key = None,
                                  `type` = JsonStringReducibleType(),
                                  originalSize = string10.s.length,
                                  reducedSize = length3,
                                  removedElement = None,
                                  changes = None
                                ),
                                ReductionChange(
                                  index = Some(2),
                                  key = None,
                                  `type` = JsonStringReducibleType(),
                                  originalSize = string100.s.length,
                                  reducedSize = length3,
                                  removedElement = None,
                                  changes = None
                                )
                              )
                            )
                          )
                        )
                      )
                    )
                  )
                )
              )
            )
            )
          ),
          ReductionChange(
            index = None,
            key = Some(keyDeep3),
            `type` = JsonStringReducibleType(),
            originalSize = string10.s.length,
            reducedSize = length3,
            removedElement = None,
            changes = None
          )
        )
      )
    )

    val deepObject3Result: ReductionResultJObject = ReductionResultJObject(
      originalObject = deepObject3,
      reducedObject = deepObject3Reduced,
      changes = ReductionChanges(
        Seq(
          ReductionChange(
            index = None,
            key = Some(keyDeep2),
            `type` = JsonObjectReducibleType(),
            originalSize = determineSerializedLength(deepObject2),
            reducedSize = 0,
            removedElement = Some(
              RemovedElement(
                Seq(keyDeep1, keyDeep2, keyDeep3)
              )
            ),
            changes = None
          )
        )
      )
    )


  }

  trait JSONMixedReducing extends JSONArrayReducing with JSONMixedObjectReducing {
    val mixedJString: JString = JString(shortValue)
    val mixedJInt: JInt = JInt(1)

    val mixedArrayInternal: JValue = JArray(
      List(
        JNull, JObject(
          JField(key11, JString(shortValue)),
          JField(key12, JArray(List(
            JNull, shortObject2))),
          JField(key12, JNull)
        ))
    )


    val mixedObject: JObject = JObject(
      JField(key1, JString(shortValue)),
      JField(key2, mixedArrayInternal),
      JField(key3, JNull)
    )

    val mixedArray: JArray = JArray(List(
      JNull, JObject(
        JField(key1, JString(shortValue)),
        JField(key2, JArray(List(
          JNull, shortObject, JObject(
            JField(key11, JArray(List(
              JNull, shortObject2)))
          )))),
        JField(key3, JNull)
      )))


    val mixedJStringResult: OriginalResultJValue[JString] = OriginalResultJValue(
      reducedJValue = mixedJString,
      originalSize = shortValue.length
    )
    val mixedJIntResult: OriginalResultJValue[JInt] = OriginalResultJValue(
      reducedJValue = mixedJInt,
      originalSize = -1
    )
    val mixedObjectResult: OriginalResultJValue[JObject] = OriginalResultJValue(
      reducedJValue = mixedObject,
      originalSize = renderJSONCompact(mixedObject).length
    )
    val mixedArrayResult: OriginalResultJValue[JArray] = OriginalResultJValue(
      reducedJValue = mixedArray,
      originalSize = renderJSONCompact(mixedArray).length
    )


    val longMixedJString: JString = JString(longValue)


    val longMixedObjectInternalArray: JArray = JArray(
      List(
        JNull,
        longObject2)
    )


    val longMixedObjectInternalArrayReduced: JArray = JArray(
      List(
        JNull,
        longObject2Reduced)
    )


    val longMixedObjectInternal2JObjectReduced: JObject =
      JObject(
        JField(key11, JString(shortenedValue)),
        JField(key12, longMixedObjectInternalArrayReduced),
        JField(key12, JNull)
      )


    val longMixedObjectInternal2JObject: JObject =
      JObject(
        JField(key11, JString(longValue)),
        JField(key12, longMixedObjectInternalArray),
        JField(key12, JNull)
      )


    val longMixedObjectInternal2Array: JArray = JArray(
      List(
        JNull,
        longMixedObjectInternal2JObject,
        longArrayUnsigned
      )
    )

    val longMixedObjectInternal2ArrayReduced: JArray = JArray(
      List(
        JNull,
        longMixedObjectInternal2JObjectReduced,
        shortenedArrayUnsigned
      )
    )

    val longMixedObject: JObject = JObject(
      JField(key1, JString(longValue)),
      JField(key2, longMixedObjectInternal2Array),
      JField(key3, JNull)
    )


    val longMixedInternalArrayKey11: JArray = JArray(
      List(
        JNull,
        longObject2,
        longArrayUnsigned
      )
    )


    val longMixedInternalArrayKey11Reduced: JArray = JArray(
      List(
        JNull,
        longObject2Reduced,
        shortenedArrayUnsigned
      )
    )


    val longMixedInternalObject: JObject = JObject(
      JField(key11, longMixedInternalArrayKey11)
    )

    val longMixedInternalObjectReduced: JObject = JObject(
      JField(key11, longMixedInternalArrayKey11Reduced)
    )


    val longMixedInternalArray: JArray = JArray(
      List(
        JNull,
        longObject2,
        longMixedInternalObject
      )
    )

    val longMixedInternalArrayReduced: JArray = JArray(
      List(
        JNull,
        longObject2Reduced,
        longMixedInternalObjectReduced
      )
    )


    val longMixedArrayInternalObject: JObject = JObject(
      JField(key1, JString(longValue)),
      JField(key2, longMixedInternalArray),
      JField(key3, JNull)
    )

    val longMixedArrayInternalObjectReduced: JObject = JObject(
      JField(key1, JString(shortenedValue)),
      JField(key2, longMixedInternalArrayReduced),
      JField(key3, JNull)
    )


    val longMixedArray: JArray = JArray(
      List(
        JNull, longMixedArrayInternalObject
      )
    )


    val shortenedMixedObjectInternalArray: JArray = JArray(
      List(
        JNull,
        shortenedObject2
      )
    )

    val shortenedMixedObject: JObject = JObject(
      JField(key1, JString(shortenedValue)),
      JField(key2, JArray(List(
        JNull,
        JObject(
          JField(key11, JString(shortenedValue)),
          JField(key12, shortenedMixedObjectInternalArray),
          JField(key12, JNull)
        ),
        longArrayUnsignedResult.reducedArray))),
      JField(key3, JNull)
    )

    val shortenedMixedArray: JArray = JArray(List(
      JNull, JObject(
        JField(key1, JString(shortenedValue)),
        JField(key2, JArray(List(
          JNull,
          shortenedObject2,
          JObject(
            JField(key11, JArray(List(
              JNull,
              shortenedObject2,
              longArrayUnsignedResult.reducedArray)))
          )))),
        JField(key3, JNull)
      )))


    val longMixedJStringResult: ReductionResultJString = ReductionResultJString(
      originalJString = JString(longValue),
      reducedJString = JString(shortenedValue)
    )

    val longMixedObjectResult: ReductionResultJObject = ReductionResultJObject(
      originalObject = longMixedObject,
      reducedObject = shortenedMixedObject,
      changes = ReductionChanges(
        List(
          reductionChanges.ReductionChange(
            index = None,
            key = Some(key1),
            `type` = JsonStringReducibleType(),
            originalSize = longValue.length,
            reducedSize = maximumLength,
            removedElement = None,
            changes = None
          ),
          reductionChanges.ReductionChange(
            index = None,
            key = Some(key2),
            `type` = JsonArrayReducibleType(),
            originalSize = renderJSONCompact(longMixedObjectInternal2Array).length,
            reducedSize = determineSerializedLength(longMixedObjectInternal2ArrayReduced),
            removedElement = None,
            changes = Some(ReductionChanges(
              List(
                reductionChanges.ReductionChange(
                  index = Some(1),
                  key = None,
                  `type` = JsonObjectReducibleType(),
                  originalSize = renderJSONCompact(longMixedObjectInternal2JObject).length,
                  reducedSize = determineSerializedLength(longMixedObjectInternal2JObjectReduced),
                  removedElement = None,
                  changes = Some(ReductionChanges(
                    List(
                      reductionChanges.ReductionChange(
                        index = None,
                        key = Some(key11),
                        `type` = JsonStringReducibleType(),
                        originalSize = longValue.length,
                        reducedSize = maximumLength,
                        removedElement = None,
                        changes = None
                      ),
                      reductionChanges.ReductionChange(
                        index = None,
                        key = Some(key12),
                        `type` = JsonArrayReducibleType(),
                        originalSize = renderJSONCompact(longMixedObjectInternalArray).length,
                        reducedSize = determineSerializedLength(longMixedObjectInternalArrayReduced),
                        removedElement = None,
                        changes = Some(ReductionChanges(
                          List(
                            reductionChanges.ReductionChange(
                              index = Some(1),
                              key = None,
                              `type` = JsonObjectReducibleType(),
                              originalSize = renderJSONCompact(longObject2).length,
                              reducedSize = determineSerializedLength(longObject2Reduced),
                              removedElement = None,
                              changes = Some(ReductionChanges(
                                List(
                                  reductionChanges.ReductionChange(
                                    index = None,
                                    key = Some(key21),
                                    `type` = JsonStringReducibleType(),
                                    originalSize = longValue.length,
                                    reducedSize = maximumLength,
                                    removedElement = None,
                                    changes = None)
                                )
                              ))
                            )
                          )
                        ))
                      )
                    )
                  ))
                ),
                ReductionChange(
                  index = Some(2),
                  key = None,
                  `type` = JsonByteArrayEquivalentReducibleType(),
                  originalSize = longArrayUnsigned.arr.size,
                  reducedSize = maximumLength,
                  removedElement = None,
                  changes = Some(ReductionChanges(
                    List(
                      reductionChanges.ReductionChange(
                        index = None,
                        key = None,
                        `type` = JsonByteArrayEquivalentReducibleType(),
                        originalSize = longArrayUnsigned.arr.size,
                        reducedSize = maximumLength,
                        removedElement = None,
                        changes = None
                      )
                    )
                  ))
                )
              )
            ))
          )
        )
      )
    )


    val longMixedArrayResult: ReductionResultJArray = ReductionResultJArray(
      originalArray = longMixedArray,
      reducedArray = shortenedMixedArray,
      changes = ReductionChanges(
        List(
          reductionChanges.ReductionChange(
            index = Some(1),
            key = None,
            `type` = JsonObjectReducibleType(),
            originalSize = renderJSONCompact(longMixedArrayInternalObject).length,
            reducedSize = determineSerializedLength(longMixedArrayInternalObjectReduced),
            removedElement = None,
            changes = Some(ReductionChanges(
              List(
                reductionChanges.ReductionChange(
                  index = None,
                  key = Some(key1),
                  `type` = JsonStringReducibleType(),
                  originalSize = longValue.length,
                  reducedSize = maximumLength,
                  removedElement = None,
                  changes = None),
                reductionChanges.ReductionChange(
                  index = None,
                  key = Some(key2),
                  `type` = JsonArrayReducibleType(),
                  originalSize = renderJSONCompact(longMixedInternalArray).length,
                  reducedSize = determineSerializedLength(longMixedInternalArrayReduced),
                  removedElement = None,
                  changes = Some(ReductionChanges(
                    List(
                      reductionChanges.ReductionChange(
                        index = Some(1),
                        key = None,
                        `type` = JsonObjectReducibleType(),
                        originalSize = renderJSONCompact(longObject2).length,
                        reducedSize = determineSerializedLength(longObject2Reduced),
                        removedElement = None,
                        changes = Some(ReductionChanges(
                          List(
                            reductionChanges.ReductionChange(
                              index = None,
                              key = Some(key21),
                              `type` = JsonStringReducibleType(),
                              originalSize = longValue.length,
                              reducedSize = maximumLength,
                              removedElement = None,
                              changes = None)
                          )
                        ))
                      ),
                      reductionChanges.ReductionChange(
                        index = Some(2),
                        key = None,
                        `type` = JsonObjectReducibleType(),
                        originalSize = renderJSONCompact(longMixedInternalObject).length,
                        reducedSize = determineSerializedLength(longMixedInternalObjectReduced),
                        removedElement = None,
                        changes = Some(ReductionChanges(
                          List(
                            reductionChanges.ReductionChange(
                              index = None,
                              key = Some(key11),
                              `type` = JsonArrayReducibleType(),
                              originalSize = renderJSONCompact(longMixedInternalArrayKey11).length,
                              reducedSize = determineSerializedLength(longMixedInternalArrayKey11Reduced),
                              removedElement = None,
                              changes = Some(ReductionChanges(
                                List(
                                  reductionChanges.ReductionChange(
                                    index = Some(1),
                                    key = None,
                                    `type` = JsonObjectReducibleType(),
                                    originalSize = renderJSONCompact(longObject2).length,
                                    reducedSize = determineSerializedLength(longObject2Reduced),
                                    removedElement = None,
                                    changes = Some(ReductionChanges(
                                      List(
                                        reductionChanges.ReductionChange(
                                          index = None,
                                          key = Some(key21),
                                          `type` = JsonStringReducibleType(),
                                          originalSize = longValue.length,
                                          reducedSize = maximumLength,
                                          removedElement = None,
                                          changes = None)
                                      )
                                    ))
                                  ),
                                  reductionChanges.ReductionChange(
                                    index = Some(2),
                                    key = None,
                                    `type` = JsonByteArrayEquivalentReducibleType(),
                                    originalSize = longArrayUnsigned.arr.size,
                                    reducedSize = maximumLength,
                                    removedElement = None,
                                    changes = Some(ReductionChanges(
                                      List(
                                        reductionChanges.ReductionChange(
                                          index = None,
                                          key = None,
                                          `type` = JsonByteArrayEquivalentReducibleType(),
                                          originalSize = longArrayUnsigned.arr.size,
                                          reducedSize = maximumLength,
                                          removedElement = None,
                                          changes = None
                                        )
                                      )
                                    ))
                                  )
                                )
                              ))
                            )
                          )
                        ))
                      )
                    )
                  ))
                )
              )
            ))
          )
        )
      )
    )
  }

  trait Errors extends BaseFixture {
    val negativeSize: Int = -11
    val zeroSize: Int = 0
  }

}