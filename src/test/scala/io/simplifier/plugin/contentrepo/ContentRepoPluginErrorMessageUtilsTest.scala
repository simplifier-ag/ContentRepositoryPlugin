package io.simplifier.plugin.contentrepo

import io.simplifier.plugin.contentrepo.controller.BaseController.OperationFailure
import io.simplifier.plugin.contentrepo.dto.RestMessages.RestMessage
import com.typesafe.config.{Config, ConfigFactory}
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatest.{MustMatchers, WordSpec}
import org.slf4j.{Logger, LoggerFactory}

class ContentRepoPluginErrorMessageUtilsTest extends AnyWordSpec with Matchers {
  "A ContentRepoPluginErrorMessageUtils" when {
    "determining the message part length " must {
      "return the default length when null values are provided" in new DeterminingConfigurationValues {
        determineMaximumMessageLength(null, defaultLength, null) mustBe defaultLength
        determineMaximumMessageLength(null, defaultLength, testPathInt) mustBe defaultLength
        determineMaximumMessageLength(goodConfig, defaultLength, null) mustBe defaultLength
      }

      "return the default length when the path is wrong" in new DeterminingConfigurationValues {
        determineMaximumMessageLength(goodConfig, defaultLength, testPathIntBad) mustBe defaultLength
      }

      "return the default length when the datatype of the value is not an integer" in new DeterminingConfigurationValues {
        determineMaximumMessageLength(badConfig, defaultLength, testPathInt) mustBe defaultLength

      }

      "return the correct length, when everything is OK" in new DeterminingConfigurationValues {
        determineMaximumMessageLength(goodConfig, defaultLength, testPathInt) mustBe foundLength
      }
    }


    "reducing the message text of an operational failure" must {
      "return an empty string when the message text is null" in new ShorteningMessages {
        shortenOperationFailureMessageTexts(null, length) mustBe OpFailureDefault
      }

      "return a reduced message text" in new ShorteningMessages {
        shortenOperationFailureMessageTexts(OpFailureLong, length) mustBe OpFailureShort
        shortenOperationFailureMessageTexts(OpFailureLong2, length) mustBe OpFailureShort2
      }
    }

    "reducing the message text of a string" must {
      "return an empty string when the message text is null" in new ShorteningMessages {
        shortenMessageText(null, length) mustBe defaultMessage
      }

      "return the original message, when the length is negative or 0" in new ShorteningMessages {
        shortenMessageText(longMessage, -1) mustBe longMessage
        shortenMessageText(longMessage, 0) mustBe longMessage
      }

      "return a reduced message text" in new ShorteningMessages {
        shortenMessageText(longMessage, length) mustBe shortMessage
      }
    }
  }


  trait BaseFixture extends ContentRepoPluginErrorMessageUtils {
    override val Logger: Logger = LoggerFactory.getLogger(getClass.getName.stripSuffix("$"))
  }


  trait DeterminingConfigurationValues extends BaseFixture {

    val testPathInt: String = "log.int"
    val testPathIntBad: String = "log.int2"



    val goodConfig: Config = ConfigFactory.parseString(
      s"""log {
         |  int: 500
         |}
         |""".stripMargin
    )

    val badConfig: Config = ConfigFactory.parseString(
      s"""log {
         |  int: [100, 500]
         |}
         |""".stripMargin
    )

    val defaultLength: Int = 1337
    val foundLength: Int = 500
  }


  trait ShorteningMessages extends BaseFixture with LongMessages {

    val length: Int = 300

    val RestMessageNull: RestMessage = RestMessage("1", "E", null)
    val RestMessageEmpty: RestMessage = RestMessage("1", "E", "")
    val RestMessageLong: RestMessage = RestMessage("1", "E", longMessage)
    val RestMessageLong2: RestMessage = RestMessage("1", "E", longMessage2)
    val RestMessageShort: RestMessage = RestMessage("1", "E", shortMessage)
    val RestMessageShort2: RestMessage = RestMessage("1", "E", shortMessage2)

    val OpFailureNull: OperationFailure = new OperationFailure(RestMessageNull)
    val OpFailureLong: OperationFailure = new OperationFailure(RestMessageLong)
    val OpFailureLong2: OperationFailure = new OperationFailure(RestMessageLong2)
    val OpFailureEmpty: OperationFailure = new OperationFailure(RestMessageEmpty)
    val OpFailureShort: OperationFailure = new OperationFailure(RestMessageShort)
    val OpFailureShort2: OperationFailure = new OperationFailure(RestMessageShort2)
    val OpFailureDefault: OperationFailure = OperationFailure(
      RestMessage(
        msgId = "",
        msgType = "Internal Error",
        msgText = "Received null instead of operation failure"
      )
    )
  }


  trait LongMessages {
    val defaultMessage: String = ""

    val shortMessage: String = "File cannot be created due to the following reason: " +
      "Missing Parameters Parsed JSON values do not match with class constructor\n" +
      "args=null,test.jpg,Some(),public,App,Kundenmarktplatz,Some(ZHNmc2RmZHNmc2Rmc2RmZGZzYWRmM2Zkc2YzMnIzNHJnZHNnd" +
      "jNyZHNmc2RmZHNmc2Rmc2RmZGZzYWRmM2Zkc2YzMnIzNHJnZHNndjNyZHNmc2RmZHN (... shortened 300 of 7.024 [Byte])"


    val longMessage: String = "File cannot be created due to the following reason: " +
      "Missing Parameters Parsed JSON values do not match with class constructor\n" +
      "args=null,test.jpg,Some(),public,App,Kundenmarktplatz," +
      "Some(ZHNmc2RmZHNmc2Rmc2RmZGZzYWRmM2Zkc2YzMnIzNHJnZHNndjNyZHNmc2RmZHNmc2Rmc2RmZGZzYWRmM2Zkc2YzMnIzNHJn" +
      "ZHNndjNyZHNmc2RmZHNmc2Rmc2RmZGZzYWRmM2Zkc2YzMnIzNHJnZHNndjNyZHNmc2RmZHNmc2Rmc2RmZGZzYWRmM2Zkc2YzMnIzNHJn" +
      "ZHNndjNyZHNmc2RmZHNmc2Rmc2RmZGZzYWRmM2Zkc2YzMnIzNHJnZHNndjNyZHNmc2RmZHNmc2Rmc2RmZGZzYWRmM2Zkc2YzMnIzNHJn" +
      "ZHNndjNyZHNmc2RmZHNmc2Rmc2RmZGZzYWRmM2Zkc2YzMnIzNHJnZHNndjNyZHNmc2RmZHNmc2Rmc2RmZGZzYWRmM2Zkc2YzMnIzNHJn" +
      "ZHNndjNyZHNmc2RmZHNmc2Rmc2RmZGZzYWRmM2Zkc2YzMnIzNHJnZHNndjNyZHNmc2RmZHNmc2Rmc2RmZGZzYWRmM2Zkc2YzMnIzNHJn" +
      "ZHNndjNyZHNmc2RmZHNmc2Rmc2RmZGZzYWRmM2Zkc2YzMnIzNHJnZHNndjNyZHNmc2RmZHNmc2Rmc2RmZGZzYWRmM2Zkc2YzMnIzNHJn" +
      "ZHNndjNyZHNmc2RmZHNmc2Rmc2RmZGZzYWRmM2Zkc2YzMnIzNHJnZHNndjNyZHNmc2RmZHNmc2Rmc2RmZGZzYWRmM2Zkc2YzMnIzNHJn" +
      "ZHNndjNyZHNmc2RmZHNmc2Rmc2RmZGZzYWRmM2Zkc2YzMnIzNHJnZHNndjNyZHNmc2RmZHNmc2Rmc2RmZGZzYWRmM2Zkc2YzMnIzNHJn" +
      "ZHNndjNyZHNmc2RmZHNmc2Rmc2RmZGZzYWRmM2Zkc2YzMnIzNHJnZHNndjNyZHNmc2RmZHNmc2Rmc2RmZGZzYWRmM2Zkc2YzMnIzNHJn" +
      "ZHNndjNyZHNmc2RmZHNmc2Rmc2RmZGZzYWRmM2Zkc2YzMnIzNHJnZHNndjNyZHNmc2RmZHNmc2Rmc2RmZGZzYWRmM2Zkc2YzMnIzNHJn" +
      "ZHNndjNyZHNmc2RmZHNmc2Rmc2RmZGZzYWRmM2Zkc2YzMnIzNHJnZHNndjNyZHNmc2RmZHNmc2Rmc2RmZGZzYWRmM2Zkc2YzMnIzNHJn" +
      "ZHNndjNyZHNmc2RmZHNmc2Rmc2RmZGZzYWRmM2Zkc2YzMnIzNHJnZHNndjNyZHNmc2RmZHNmc2Rmc2RmZGZzYWRmM2Zkc2YzMnIzNHJn" +
      "ZHNndjNyZHNmc2RmZHNmc2Rmc2RmZGZzYWRmM2Zkc2YzMnIzNHJnZHNndjNyZHNmc2RmZHNmc2Rmc2RmZGZzYWRmM2Zkc2YzMnIzNHJn" +
      "ZHNndjNyZHNmc2RmZHNmc2Rmc2RmZGZzYWRmM2Zkc2YzMnIzNHJnZHNndjNyZHNmc2RmZHNmc2Rmc2RmZGZzYWRmM2Zkc2YzMnIzNHJn" +
      "ZHNndjNyZHNmc2RmZHNmc2Rmc2RmZGZzYWRmM2Zkc2YzMnIzNHJnZHNndjNyZHNmc2RmZHNmc2Rmc2RmZGZzYWRmM2Zkc2YzMnIzNHJn" +
      "ZHNndjNyZHNmc2RmZHNmc2Rmc2RmZGZzYWRmM2Zkc2YzMnIzNHJnZHNndjNyZHNmc2RmZHNmc2Rmc2RmZGZzYWRmM2Zkc2YzMnIzNHJn" +
      "ZHNndjNyZHNmc2RmZHNmc2Rmc2RmZGZzYWRmM2Zkc2YzMnIzNHJnZHNndjNyZHNmc2RmZHNmc2Rmc2RmZGZzYWRmM2Zkc2YzMnIzNHJn" +
      "ZHNndjNyZHNmc2RmZHNmc2Rmc2RmZGZzYWRmM2Zkc2YzMnIzNHJnZHNndjNyZHNmc2RmZHNmc2Rmc2RmZGZzYWRmM2Zkc2YzMnIzNHJn" +
      "ZHNndjNyZHNmc2RmZHNmc2Rmc2RmZGZzYWRmM2Zkc2YzMnIzNHJnZHNndjNyZHNmc2RmZHNmc2Rmc2RmZGZzYWRmM2Zkc2YzMnIzNHJn" +
      "ZHNndjNyZHNmc2RmZHNmc2Rmc2RmZGZzYWRmM2Zkc2YzMnIzNHJnZHNndjNyZHNmc2RmZHNmc2Rmc2RmZGZzYWRmM2Zkc2YzMnIzNHJn" +
      "ZHNndjNyZHNmc2RmZHNmc2Rmc2RmZGZzYWRmM2Zkc2YzMnIzNHJnZHNndjNyZHNmc2RmZHNmc2Rmc2RmZGZzYWRmM2Zkc2YzMnIzNHJn" +
      "ZHNndjNyZHNmc2RmZHNmc2Rmc2RmZGZzYWRmM2Zkc2YzMnIzNHJnZHNndjNyZHNmc2RmZHNmc2Rmc2RmZGZzYWRmM2Zkc2YzMnIzNHJn" +
      "ZHNndjNyZHNmc2RmZHNmc2Rmc2RmZGZzYWRmM2Zkc2YzMnIzNHJnZHNndjNyZHNmc2RmZHNmc2Rmc2RmZGZzYWRmM2Zkc2YzMnIzNHJn" +
      "ZHNndjNyZHNmc2RmZHNmc2Rmc2RmZGZzYWRmM2Zkc2YzMnIzNHJnZHNndjNyZHNmc2RmZHNmc2Rmc2RmZGZzYWRmM2Zkc2YzMnIzNHJn" +
      "ZHNndjNyZHNmc2RmZHNmc2Rmc2RmZGZzYWRmM2Zkc2YzMnIzNHJnZHNndjNyZHNmc2RmZHNmc2Rmc2RmZGZzYWRmM2Zkc2YzMnIzNHJn" +
      "ZHNndjNyZHNmc2RmZHNmc2Rmc2RmZGZzYWRmM2Zkc2YzMnIzNHJnZHNndjNyZHNmc2RmZHNmc2Rmc2RmZGZzYWRmM2Zkc2YzMnIzNHJn" +
      "ZHNndjNyZHNmc2RmZHNmc2Rmc2RmZGZzYWRmM2Zkc2YzMnIzNHJnZHNndjNyZHNmc2RmZHNmc2Rmc2RmZGZzYWRmM2Zkc2YzMnIzNHJn" +
      "ZHNndjNyZHNmc2RmZHNmc2Rmc2RmZGZzYWRmM2Zkc2YzMnIzNHJnZHNndjNyZHNmc2RmZHNmc2Rmc2RmZGZzYWRmM2Zkc2YzMnIzNHJn" +
      "ZHNndjNyZHNmc2RmZHNmc2Rmc2RmZGZzYWRmM2Zkc2YzMnIzNHJnZHNndjNyZHNmc2RmZHNmc2Rmc2RmZGZzYWRmM2Zkc2YzMnIzNHJn" +
      "ZHNndjNyZHNmc2RmZHNmc2Rmc2RmZGZzYWRmM2Zkc2YzMnIzNHJnZHNndjNyZHNmc2RmZHNmc2Rmc2RmZGZzYWRmM2Zkc2YzMnIzNHJn" +
      "ZHNndjNyZHNmc2RmZHNmc2Rmc2RmZGZzYWRmM2Zkc2YzMnIzNHJnZHNndjNyZHNmc2RmZHNmc2Rmc2RmZGZzYWRmM2Zkc2YzMnIzNHJn" +
      "ZHNndjNyZHNmc2RmZHNmc2Rmc2RmZGZzYWRmM2Zkc2YzMnIzNHJnZHNndjNyZHNmc2RmZHNmc2Rmc2RmZGZzYWRmM2Zkc2YzMnIzNHJn" +
      "ZHNndjNyZHNmc2RmZHNmc2Rmc2RmZGZzYWRmM2Zkc2YzMnIzNHJnZHNndjNyZHNmc2RmZHNmc2Rmc2RmZGZzYWRmM2Zkc2YzMnIzNHJn" +
      "ZHNndjNyZHNmc2RmZHNmc2Rmc2RmZGZzYWRmM2Zkc2YzMnIzNHJnZHNndjNyZHNmc2RmZHNmc2Rmc2RmZGZzYWRmM2Zkc2YzMnIzNHJn" +
      "ZHNndjNyZHNmc2RmZHNmc2Rmc2RmZGZzYWRmM2Zkc2YzMnIzNHJnZHNndjNyZHNmc2RmZHNmc2Rmc2RmZGZzYWRmM2Zkc2YzMnIzNHJn" +
      "ZHNndjNyZHNmc2RmZHNmc2Rmc2RmZGZzYWRmM2Zkc2YzMnIzNHJnZHNndjNyZHNmc2RmZHNmc2Rmc2RmZGZzYWRmM2Zkc2YzMnIzNHJn" +
      "ZHNndjNyZHNmc2RmZHNmc2Rmc2RmZGZzYWRmM2Zkc2YzMnIzNHJnZHNndjNyZHNmc2RmZHNmc2Rmc2RmZGZzYWRmM2Zkc2YzMnIzNHJn" +
      "ZHNndjNyZHNmc2RmZHNmc2Rmc2RmZGZzYWRmM2Zkc2YzMnIzNHJnZHNndjNyZHNmc2RmZHNmc2Rmc2RmZGZzYWRmM2Zkc2YzMnIzNHJn" +
      "ZHNndjNyZHNmc2RmZHNmc2Rmc2RmZGZzYWRmM2Zkc2YzMnIzNHJnZHNndjNyZHNmc2RmZHNmc2Rmc2RmZGZzYWRmM2Zkc2YzMnIzNHJn" +
      "ZHNndjNyZHNmc2RmZHNmc2Rmc2RmZGZzYWRmM2Zkc2YzMnIzNHJnZHNndjNyZHNmc2RmZHNmc2Rmc2RmZGZzYWRmM2Zkc2YzMnIzNHJn" +
      "ZHNndjNyZHNmc2RmZHNmc2Rmc2RmZGZzYWRmM2Zkc2YzMnIzNHJnZHNndjNyZHNmc2RmZHNmc2Rmc2RmZGZzYWRmM2Zkc2YzMnIzNHJn" +
      "ZHNndjNyZHNmc2RmZHNmc2Rmc2RmZGZzYWRmM2Zkc2YzMnIzNHJnZHNndjNyZHNmc2RmZHNmc2Rmc2RmZGZzYWRmM2Zkc2YzMnIzNHJn" +
      "ZHNndjNyZHNmc2RmZHNmc2Rmc2RmZGZzYWRmM2Zkc2YzMnIzNHJnZHNndjNyZHNmc2RmZHNmc2Rmc2RmZGZzYWRmM2Zkc2YzMnIzNHJn" +
      "ZHNndjNyZHNmc2RmZHNmc2Rmc2RmZGZzYWRmM2Zkc2YzMnIzNHJnZHNndjNyZHNmc2RmZHNmc2Rmc2RmZGZzYWRmM2Zkc2YzMnIzNHJn" +
      "ZHNndjNyZHNmc2RmZHNmc2Rmc2RmZGZzYWRmM2Zkc2YzMnIzNHJnZHNndjNyZHNmc2RmZHNmc2Rmc2RmZGZzYWRmM2Zkc2YzMnIzNHJn" +
      "ZHNndjNyZHNmc2RmZHNmc2Rmc2RmZGZzYWRmM2Zkc2YzMnIzNHJnZHNndjNyZHNmc2RmZHNmc2Rmc2RmZGZzYWRmM2Zkc2YzMnIzNHJn" +
      "ZHNndjNyZHNmc2RmZHNmc2Rmc2RmZGZzYWRmM2Zkc2YzMnIzNHJnZHNndjNyZHNmc2RmZHNmc2Rmc2RmZGZzYWRmM2Zkc2YzMnIzNHJn" +
      "ZHNndjNyZHNmc2RmZHNmc2Rmc2RmZGZzYWRmM2Zkc2YzMnIzNHJnZHNndjNyZHNmc2RmZHNmc2Rmc2RmZGZzYWRmM2Zkc2YzMnIzNHJn" +
      "ZHNndjNyZHNmc2RmZHNmc2Rmc2RmZGZzYWRmM2Zkc2YzMnIzNHJnZHNndjNyZHNmc2RmZHNmc2Rmc2RmZGZzYWRmM2Zkc2YzMnIzNHJn" +
      "ZHNndjNyZHNmc2RmZHNmc2Rmc2RmZGZzYWRmM2Zkc2YzMnIzNHJnZHNndjNyZHNmc2RmZHNmc2Rmc2RmZGZzYWRmM2Zkc2YzMnIzNHJn" +
      "ZHNndjNyZHNmc2RmZHNmc2Rmc2RmZGZzYWRmM2Zkc2YzMnIzNHJnZHNndjNyZHNmc2RmZHNmc2Rmc2RmZGZzYWRmM2Zkc2YzMnIzNHJn" +
      "ZHNndjNyZHNmc2RmZHNmc2Rmc2RmZGZzYWRmM2Zkc2YzMnIzNHJnZHNndjNyZHNmc2RmZHNmc2Rmc2RmZGZzYWRmM2Zkc2YzMnIzNHJn" +
      "ZHNndjNyZHNmc2RmZHNmc2Rmc2RmZGZzYWRmM2Zkc2YzMnIzNHJnZHNndjNyZHNmc2RmZHNmc2Rmc2RmZGZzYWRmM2Zkc2YzMnIzNHJn" +
      "ZHNndjNyZHNmc2RmZHNmc2Rmc2RmZGZzYWRmM2Zkc2YzMnIzNHJnZHNndjNyZHNmc2RmZHNmc2Rmc2RmZGZzYWRmM2Zkc2YzMnIzNHJn" +
      "ZHNndjNyZHNmc2RmZHNmc2Rmc2RmZGZzYWRmM2Zkc2YzMnIzNHJnZHNndjNyZHNmc2RmZHNmc2Rmc2RmZGZzYWRmM2Zkc2YzMnIzNHJn" +
      "ZHNndjNyZHNmc2RmZHNmc2Rmc2RmZGZzYWRmM2Zkc2YzMnIzNHJnZHNndjNyZHNmc2RmZHNmc2Rmc2RmZGZzYWRmM2Zkc2YzMnIzNHJn" +
      "ZHNndjNyZHNmc2RmZHNmc2Rmc2RmZGZzYWRmM2Zkc2YzMnIzNHJnZHNndjNyZHNmc2RmZHNmc2Rmc2RmZGZzYWRmM2Zkc2YzMnIzNHJn" +
      "ZHNndjNyZHNmc2RmZHNmc2Rmc2RmZGZzYWRmM2Zkc2YzMnIzNHJnZHNndjNyZHNmc2RmZHNmc2Rmc2RmZGZzYWRmM2Zkc2YzMnIzNHJn" +
      "ZHNndjNyZHNmc2RmZHNmc2Rmc2RmZGZzYWRmM2Zkc2YzMnIzNHJnZHNndjNyZHNmc2RmZHNmc2Rmc2RmZGZzYWRmM2Zkc2YzMnIzNHJn" +
      "ZHNndjNyZHNmc2RmZHNmc2Rmc2RmZGZzYWRmM2Zkc2YzMnIzNHJnZHNndjNyZHNmc2RmZHNmc2Rmc2RmZGZzYWRmM2Zkc2YzMnIzNHJn" +
      "ZHNndjNyZHNmc2RmZHNmc2Rmc2RmZGZzYWRmM2Zkc2YzMnIzNHJnZHNndjNyZHNmc2RmZHNmc2Rmc2RmZGZzYWRmM2Zkc2YzMnIzNHJn" +
      "ZHNndjNyZHNmc2RmZHNmc2Rmc2RmZGZzYWRmM2Zkc2YzMnIzNHJnZHNndjNyZHNmc2RmZHNmc2Rmc2RmZGZzYWRmM2Zkc2YzMnIzNHJn" +
      "ZHNndjNydg==),None,None\n" +
      "arg types=null,java.lang.String,scala.Some,java.lang.String,java.lang.String,java.lang.String,scala.Some," +
      "scala.None$,scala.None$\n" +
      "constructor=public io.simplifier.plugin.contentrepo.definitions.caseClasses.ContentFileCaseClasses$" +
      "ContentFileAddRequest(int,java.lang.String,scala.Option,java.lang.String,java.lang.String,java.lang.String," +
      "scala.Option,scala.Option,scala.Option)."


    val shortMessage2: String = "File cannot be created due to the following reason: " +
      "Missing Parameters Parsed JSON values do not match with class constructor\n" +
      "args=null,test.jpg,Some(),public,App,Kundenmarktplatz,Some(ZHNmc2RmZHNmc2Rmc2RmZGZzYWRmM2Zkc2YzMnIzNHJnZHNnd" +
      "jNyZHNmc2RmZHNmc2Rmc2RmZGZzYWRmM2Zkc2YzMnIzNHJnZHNndjNyZHNmc2RmZHN (... shortened 300 of 13.485 [Byte])"


    val longMessage2: String = "File cannot be created due to the following reason: " +
      "Missing Parameters Parsed JSON values do not match with class constructor\n" +
      "args=null,test.jpg,Some(),public,App,Kundenmarktplatz," +
      "Some(ZHNmc2RmZHNmc2Rmc2RmZGZzYWRmM2Zkc2YzMnIzNHJnZHNndjNyZHNmc2RmZHNmc2Rmc2RmZGZzYWRmM2Zkc2YzMnIzNHJn" +
      "ZHNndjNyZHNmc2RmZHNmc2Rmc2RmZGZzYWRmM2Zkc2YzMnIzNHJnZHNndjNyZHNmc2RmZHNmc2Rmc2RmZGZzYWRmM2Zkc2YzMnIzNHJn" +
      "ZHNndjNyZHNmc2RmZHNmc2Rmc2RmZGZzYWRmM2Zkc2YzMnIzNHJnZHNndjNyZHNmc2RmZHNmc2Rmc2RmZGZzYWRmM2Zkc2YzMnIzNHJn" +
      "ZHNndjNyZHNmc2RmZHNmc2Rmc2RmZGZzYWRmM2Zkc2YzMnIzNHJnZHNndjNyZHNmc2RmZHNmc2Rmc2RmZGZzYWRmM2Zkc2YzMnIzNHJn" +
      "ZHNndjNyZHNmc2RmZHNmc2Rmc2RmZGZzYWRmM2Zkc2YzMnIzNHJnZHNndjNyZHNmc2RmZHNmc2Rmc2RmZGZzYWRmM2Zkc2YzMnIzNHJn" +
      "ZHNndjNyZHNmc2RmZHNmc2Rmc2RmZGZzYWRmM2Zkc2YzMnIzNHJnZHNndjNyZHNmc2RmZHNmc2Rmc2RmZGZzYWRmM2Zkc2YzMnIzNHJn" +
      "ZHNndjNyZHNmc2RmZHNmc2Rmc2RmZGZzYWRmM2Zkc2YzMnIzNHJnZHNndjNyZHNmc2RmZHNmc2Rmc2RmZGZzYWRmM2Zkc2YzMnIzNHJn" +
      "ZHNndjNyZHNmc2RmZHNmc2Rmc2RmZGZzYWRmM2Zkc2YzMnIzNHJnZHNndjNyZHNmc2RmZHNmc2Rmc2RmZGZzYWRmM2Zkc2YzMnIzNHJn" +
      "ZHNndjNyZHNmc2RmZHNmc2Rmc2RmZGZzYWRmM2Zkc2YzMnIzNHJnZHNndjNyZHNmc2RmZHNmc2Rmc2RmZGZzYWRmM2Zkc2YzMnIzNHJn" +
      "ZHNndjNyZHNmc2RmZHNmc2Rmc2RmZGZzYWRmM2Zkc2YzMnIzNHJnZHNndjNyZHNmc2RmZHNmc2Rmc2RmZGZzYWRmM2Zkc2YzMnIzNHJn" +
      "ZHNndjNyZHNmc2RmZHNmc2Rmc2RmZGZzYWRmM2Zkc2YzMnIzNHJnZHNndjNyZHNmc2RmZHNmc2Rmc2RmZGZzYWRmM2Zkc2YzMnIzNHJn" +
      "ZHNndjNyZHNmc2RmZHNmc2Rmc2RmZGZzYWRmM2Zkc2YzMnIzNHJnZHNndjNyZHNmc2RmZHNmc2Rmc2RmZGZzYWRmM2Zkc2YzMnIzNHJn" +
      "ZHNndjNyZHNmc2RmZHNmc2Rmc2RmZGZzYWRmM2Zkc2YzMnIzNHJnZHNndjNyZHNmc2RmZHNmc2Rmc2RmZGZzYWRmM2Zkc2YzMnIzNHJn" +
      "ZHNndjNyZHNmc2RmZHNmc2Rmc2RmZGZzYWRmM2Zkc2YzMnIzNHJnZHNndjNyZHNmc2RmZHNmc2Rmc2RmZGZzYWRmM2Zkc2YzMnIzNHJn" +
      "ZHNndjNyZHNmc2RmZHNmc2Rmc2RmZGZzYWRmM2Zkc2YzMnIzNHJnZHNndjNyZHNmc2RmZHNmc2Rmc2RmZGZzYWRmM2Zkc2YzMnIzNHJn" +
      "ZHNndjNyZHNmc2RmZHNmc2Rmc2RmZGZzYWRmM2Zkc2YzMnIzNHJnZHNndjNyZHNmc2RmZHNmc2Rmc2RmZGZzYWRmM2Zkc2YzMnIzNHJn" +
      "ZHNndjNyZHNmc2RmZHNmc2Rmc2RmZGZzYWRmM2Zkc2YzMnIzNHJnZHNndjNyZHNmc2RmZHNmc2Rmc2RmZGZzYWRmM2Zkc2YzMnIzNHJn" +
      "ZHNndjNyZHNmc2RmZHNmc2Rmc2RmZGZzYWRmM2Zkc2YzMnIzNHJnZHNndjNyZHNmc2RmZHNmc2Rmc2RmZGZzYWRmM2Zkc2YzMnIzNHJn" +
      "ZHNndjNyZHNmc2RmZHNmc2Rmc2RmZGZzYWRmM2Zkc2YzMnIzNHJnZHNndjNyZHNmc2RmZHNmc2Rmc2RmZGZzYWRmM2Zkc2YzMnIzNHJn" +
      "ZHNndjNyZHNmc2RmZHNmc2Rmc2RmZGZzYWRmM2Zkc2YzMnIzNHJnZHNndjNyZHNmc2RmZHNmc2Rmc2RmZGZzYWRmM2Zkc2YzMnIzNHJn" +
      "ZHNndjNyZHNmc2RmZHNmc2Rmc2RmZGZzYWRmM2Zkc2YzMnIzNHJnZHNndjNyZHNmc2RmZHNmc2Rmc2RmZGZzYWRmM2Zkc2YzMnIzNHJn" +
      "ZHNndjNyZHNmc2RmZHNmc2Rmc2RmZGZzYWRmM2Zkc2YzMnIzNHJnZHNndjNyZHNmc2RmZHNmc2Rmc2RmZGZzYWRmM2Zkc2YzMnIzNHJn" +
      "ZHNndjNyZHNmc2RmZHNmc2Rmc2RmZGZzYWRmM2Zkc2YzMnIzNHJnZHNndjNyZHNmc2RmZHNmc2Rmc2RmZGZzYWRmM2Zkc2YzMnIzNHJn" +
      "ZHNndjNyZHNmc2RmZHNmc2Rmc2RmZGZzYWRmM2Zkc2YzMnIzNHJnZHNndjNyZHNmc2RmZHNmc2Rmc2RmZGZzYWRmM2Zkc2YzMnIzNHJn" +
      "ZHNndjNyZHNmc2RmZHNmc2Rmc2RmZGZzYWRmM2Zkc2YzMnIzNHJnZHNndjNyZHNmc2RmZHNmc2Rmc2RmZGZzYWRmM2Zkc2YzMnIzNHJn" +
      "ZHNndjNyZHNmc2RmZHNmc2Rmc2RmZGZzYWRmM2Zkc2YzMnIzNHJnZHNndjNyZHNmc2RmZHNmc2Rmc2RmZGZzYWRmM2Zkc2YzMnIzNHJn" +
      "ZHNndjNyZHNmc2RmZHNmc2Rmc2RmZGZzYWRmM2Zkc2YzMnIzNHJnZHNndjNyZHNmc2RmZHNmc2Rmc2RmZGZzYWRmM2Zkc2YzMnIzNHJn" +
      "ZHNndjNyZHNmc2RmZHNmc2Rmc2RmZGZzYWRmM2Zkc2YzMnIzNHJnZHNndjNyZHNmc2RmZHNmc2Rmc2RmZGZzYWRmM2Zkc2YzMnIzNHJn" +
      "ZHNndjNyZHNmc2RmZHNmc2Rmc2RmZGZzYWRmM2Zkc2YzMnIzNHJnZHNndjNyZHNmc2RmZHNmc2Rmc2RmZGZzYWRmM2Zkc2YzMnIzNHJn" +
      "ZHNndjNyZHNmc2RmZHNmc2Rmc2RmZGZzYWRmM2Zkc2YzMnIzNHJnZHNndjNyZHNmc2RmZHNmc2Rmc2RmZGZzYWRmM2Zkc2YzMnIzNHJn" +
      "ZHNndjNyZHNmc2RmZHNmc2Rmc2RmZGZzYWRmM2Zkc2YzMnIzNHJnZHNndjNyZHNmc2RmZHNmc2Rmc2RmZGZzYWRmM2Zkc2YzMnIzNHJn" +
      "ZHNndjNyZHNmc2RmZHNmc2Rmc2RmZGZzYWRmM2Zkc2YzMnIzNHJnZHNndjNyZHNmc2RmZHNmc2Rmc2RmZGZzYWRmM2Zkc2YzMnIzNHJn" +
      "ZHNndjNyZHNmc2RmZHNmc2Rmc2RmZGZzYWRmM2Zkc2YzMnIzNHJnZHNndjNyZHNmc2RmZHNmc2Rmc2RmZGZzYWRmM2Zkc2YzMnIzNHJn" +
      "ZHNndjNyZHNmc2RmZHNmc2Rmc2RmZGZzYWRmM2Zkc2YzMnIzNHJnZHNndjNyZHNmc2RmZHNmc2Rmc2RmZGZzYWRmM2Zkc2YzMnIzNHJn" +
      "ZHNndjNyZHNmc2RmZHNmc2Rmc2RmZGZzYWRmM2Zkc2YzMnIzNHJnZHNndjNyZHNmc2RmZHNmc2Rmc2RmZGZzYWRmM2Zkc2YzMnIzNHJn" +
      "ZHNndjNyZHNmc2RmZHNmc2Rmc2RmZGZzYWRmM2Zkc2YzMnIzNHJnZHNndjNyZHNmc2RmZHNmc2Rmc2RmZGZzYWRmM2Zkc2YzMnIzNHJn" +
      "ZHNndjNyZHNmc2RmZHNmc2Rmc2RmZGZzYWRmM2Zkc2YzMnIzNHJnZHNndjNyZHNmc2RmZHNmc2Rmc2RmZGZzYWRmM2Zkc2YzMnIzNHJn" +
      "ZHNndjNyZHNmc2RmZHNmc2Rmc2RmZGZzYWRmM2Zkc2YzMnIzNHJnZHNndjNyZHNmc2RmZHNmc2Rmc2RmZGZzYWRmM2Zkc2YzMnIzNHJn" +
      "ZHNndjNyZHNmc2RmZHNmc2Rmc2RmZGZzYWRmM2Zkc2YzMnIzNHJnZHNndjNyZHNmc2RmZHNmc2Rmc2RmZGZzYWRmM2Zkc2YzMnIzNHJn" +
      "ZHNndjNyZHNmc2RmZHNmc2Rmc2RmZGZzYWRmM2Zkc2YzMnIzNHJnZHNndjNyZHNmc2RmZHNmc2Rmc2RmZGZzYWRmM2Zkc2YzMnIzNHJn" +
      "ZHNndjNyZHNmc2RmZHNmc2Rmc2RmZGZzYWRmM2Zkc2YzMnIzNHJnZHNndjNyZHNmc2RmZHNmc2Rmc2RmZGZzYWRmM2Zkc2YzMnIzNHJn" +
      "ZHNndjNyZHNmc2RmZHNmc2Rmc2RmZGZzYWRmM2Zkc2YzMnIzNHJnZHNndjNyZHNmc2RmZHNmc2Rmc2RmZGZzYWRmM2Zkc2YzMnIzNHJn" +
      "ZHNndjNyZHNmc2RmZHNmc2Rmc2RmZGZzYWRmM2Zkc2YzMnIzNHJnZHNndjNyZHNmc2RmZHNmc2Rmc2RmZGZzYWRmM2Zkc2YzMnIzNHJn" +
      "ZHNndjNyZHNmc2RmZHNmc2Rmc2RmZGZzYWRmM2Zkc2YzMnIzNHJnZHNndjNyZHNmc2RmZHNmc2Rmc2RmZGZzYWRmM2Zkc2YzMnIzNHJn" +
      "ZHNndjNyZHNmc2RmZHNmc2Rmc2RmZGZzYWRmM2Zkc2YzMnIzNHJnZHNndjNyZHNmc2RmZHNmc2Rmc2RmZGZzYWRmM2Zkc2YzMnIzNHJn" +
      "ZHNndjNyZHNmc2RmZHNmc2Rmc2RmZGZzYWRmM2Zkc2YzMnIzNHJnZHNndjNyZHNmc2RmZHNmc2Rmc2RmZGZzYWRmM2Zkc2YzMnIzNHJn" +
      "ZHNndjNyZHNmc2RmZHNmc2Rmc2RmZGZzYWRmM2Zkc2YzMnIzNHJnZHNndjNyZHNmc2RmZHNmc2Rmc2RmZGZzYWRmM2Zkc2YzMnIzNHJn" +
      "ZHNndjNyZHNmc2RmZHNmc2Rmc2RmZGZzYWRmM2Zkc2YzMnIzNHJnZHNndjNyZHNmc2RmZHNmc2Rmc2RmZGZzYWRmM2Zkc2YzMnIzNHJn" +
      "ZHNndjNyZHNmc2RmZHNmc2Rmc2RmZGZzYWRmM2Zkc2YzMnIzNHJnZHNndjNyZHNmc2RmZHNmc2Rmc2RmZGZzYWRmM2Zkc2YzMnIzNHJn" +
      "ZHNndjNyZHNmc2RmZHNmc2Rmc2RmZGZzYWRmM2Zkc2YzMnIzNHJnZHNndjNyZHNmc2RmZHNmc2Rmc2RmZGZzYWRmM2Zkc2YzMnIzNHJn" +
      "ZHNndjNyZHNmc2RmZHNmc2Rmc2RmZGZzYWRmM2Zkc2YzMnIzNHJnZHNndjNyZHNmc2RmZHNmc2Rmc2RmZGZzYWRmM2Zkc2YzMnIzNHJn" +
      "ZHNndjNyZHNmc2RmZHNmc2Rmc2RmZGZzYWRmM2Zkc2YzMnIzNHJnZHNndjNyZHNmc2RmZHNmc2Rmc2RmZGZzYWRmM2Zkc2YzMnIzNHJn" +
      "ZHNndjNyZHNmc2RmZHNmc2Rmc2RmZGZzYWRmM2Zkc2YzMnIzNHJnZHNndjNyZHNmc2RmZHNmc2Rmc2RmZGZzYWRmM2Zkc2YzMnIzNHJn" +
      "ZHNndjNyZHNmc2RmZHNmc2Rmc2RmZGZzYWRmM2Zkc2YzMnIzNHJnZHNndjNyZHNmc2RmZHNmc2Rmc2RmZGZzYWRmM2Zkc2YzMnIzNHJn" +
      "ZHNndjNyZHNmc2RmZHNmc2Rmc2RmZGZzYWRmM2Zkc2YzMnIzNHJnZHNndjNyZHNmc2RmZHNmc2Rmc2RmZGZzYWRmM2Zkc2YzMnIzNHJn" +
      "ZHNndjNyZHNmc2RmZHNmc2Rmc2RmZGZzYWRmM2Zkc2YzMnIzNHJnZHNndjNyZHNmc2RmZHNmc2Rmc2RmZGZzYWRmM2Zkc2YzMnIzNHJn" +
      "ZHNndjNyZHNmc2RmZHNmc2Rmc2RmZGZzYWRmM2Zkc2YzMnIzNHJnZHNndjNyZHNmc2RmZHNmc2Rmc2RmZGZzYWRmM2Zkc2YzMnIzNHJn" +
      "ZHNndjNyZHNmc2RmZHNmc2Rmc2RmZGZzYWRmM2Zkc2YzMnIzNHJnZHNndjNyZHNmc2RmZHNmc2Rmc2RmZGZzYWRmM2Zkc2YzMnIzNHJn" +
      "ZHNndjNyZHNmc2RmZHNmc2Rmc2RmZGZzYWRmM2Zkc2YzMnIzNHJnZHNndjNyZHNmc2RmZHNmc2Rmc2RmZGZzYWRmM2Zkc2YzMnIzNHJn" +
      "ZHNndjNyZHNmc2RmZHNmc2Rmc2RmZGZzYWRmM2Zkc2YzMnIzNHJnZHNndjNyZHNmc2RmZHNmc2Rmc2RmZGZzYWRmM2Zkc2YzMnIzNHJn" +
      "ZHNndjNyZHNmc2RmZHNmc2Rmc2RmZGZzYWRmM2Zkc2YzMnIzNHJnZHNndjNyZHNmc2RmZHNmc2Rmc2RmZGZzYWRmM2Zkc2YzMnIzNHJn" +
      "ZHNndjNyZHNmc2RmZHNmc2Rmc2RmZGZzYWRmM2Zkc2YzMnIzNHJnZHNndjNyZHNmc2RmZHNmc2Rmc2RmZGZzYWRmM2Zkc2YzMnIzNHJn" +
      "ZHNndjNydg==), " +
      "Some(ZHNmc2RmZHNmc2Rmc2RmZGZzYWRmM2Zkc2YzMnIzNHJnZHNndjNyZHNmc2RmZHNmc2Rmc2RmZGZzYWRmM2Zkc2YzMnIzNHJn" +
      "ZHNndjNyZHNmc2RmZHNmc2Rmc2RmZGZzYWRmM2Zkc2YzMnIzNHJnZHNndjNyZHNmc2RmZHNmc2Rmc2RmZGZzYWRmM2Zkc2YzMnIzNHJn" +
      "ZHNndjNyZHNmc2RmZHNmc2Rmc2RmZGZzYWRmM2Zkc2YzMnIzNHJnZHNndjNyZHNmc2RmZHNmc2Rmc2RmZGZzYWRmM2Zkc2YzMnIzNHJn" +
      "ZHNndjNyZHNmc2RmZHNmc2Rmc2RmZGZzYWRmM2Zkc2YzMnIzNHJnZHNndjNyZHNmc2RmZHNmc2Rmc2RmZGZzYWRmM2Zkc2YzMnIzNHJn" +
      "ZHNndjNyZHNmc2RmZHNmc2Rmc2RmZGZzYWRmM2Zkc2YzMnIzNHJnZHNndjNyZHNmc2RmZHNmc2Rmc2RmZGZzYWRmM2Zkc2YzMnIzNHJn" +
      "ZHNndjNyZHNmc2RmZHNmc2Rmc2RmZGZzYWRmM2Zkc2YzMnIzNHJnZHNndjNyZHNmc2RmZHNmc2Rmc2RmZGZzYWRmM2Zkc2YzMnIzNHJn" +
      "ZHNndjNyZHNmc2RmZHNmc2Rmc2RmZGZzYWRmM2Zkc2YzMnIzNHJnZHNndjNyZHNmc2RmZHNmc2Rmc2RmZGZzYWRmM2Zkc2YzMnIzNHJn" +
      "ZHNndjNyZHNmc2RmZHNmc2Rmc2RmZGZzYWRmM2Zkc2YzMnIzNHJnZHNndjNyZHNmc2RmZHNmc2Rmc2RmZGZzYWRmM2Zkc2YzMnIzNHJn" +
      "ZHNndjNyZHNmc2RmZHNmc2Rmc2RmZGZzYWRmM2Zkc2YzMnIzNHJnZHNndjNyZHNmc2RmZHNmc2Rmc2RmZGZzYWRmM2Zkc2YzMnIzNHJn" +
      "ZHNndjNyZHNmc2RmZHNmc2Rmc2RmZGZzYWRmM2Zkc2YzMnIzNHJnZHNndjNyZHNmc2RmZHNmc2Rmc2RmZGZzYWRmM2Zkc2YzMnIzNHJn" +
      "ZHNndjNyZHNmc2RmZHNmc2Rmc2RmZGZzYWRmM2Zkc2YzMnIzNHJnZHNndjNyZHNmc2RmZHNmc2Rmc2RmZGZzYWRmM2Zkc2YzMnIzNHJn" +
      "ZHNndjNyZHNmc2RmZHNmc2Rmc2RmZGZzYWRmM2Zkc2YzMnIzNHJnZHNndjNyZHNmc2RmZHNmc2Rmc2RmZGZzYWRmM2Zkc2YzMnIzNHJn" +
      "ZHNndjNyZHNmc2RmZHNmc2Rmc2RmZGZzYWRmM2Zkc2YzMnIzNHJnZHNndjNyZHNmc2RmZHNmc2Rmc2RmZGZzYWRmM2Zkc2YzMnIzNHJn" +
      "ZHNndjNyZHNmc2RmZHNmc2Rmc2RmZGZzYWRmM2Zkc2YzMnIzNHJnZHNndjNyZHNmc2RmZHNmc2Rmc2RmZGZzYWRmM2Zkc2YzMnIzNHJn" +
      "ZHNndjNyZHNmc2RmZHNmc2Rmc2RmZGZzYWRmM2Zkc2YzMnIzNHJnZHNndjNyZHNmc2RmZHNmc2Rmc2RmZGZzYWRmM2Zkc2YzMnIzNHJn" +
      "ZHNndjNyZHNmc2RmZHNmc2Rmc2RmZGZzYWRmM2Zkc2YzMnIzNHJnZHNndjNyZHNmc2RmZHNmc2Rmc2RmZGZzYWRmM2Zkc2YzMnIzNHJn" +
      "ZHNndjNyZHNmc2RmZHNmc2Rmc2RmZGZzYWRmM2Zkc2YzMnIzNHJnZHNndjNyZHNmc2RmZHNmc2Rmc2RmZGZzYWRmM2Zkc2YzMnIzNHJn" +
      "ZHNndjNyZHNmc2RmZHNmc2Rmc2RmZGZzYWRmM2Zkc2YzMnIzNHJnZHNndjNyZHNmc2RmZHNmc2Rmc2RmZGZzYWRmM2Zkc2YzMnIzNHJn" +
      "ZHNndjNyZHNmc2RmZHNmc2Rmc2RmZGZzYWRmM2Zkc2YzMnIzNHJnZHNndjNyZHNmc2RmZHNmc2Rmc2RmZGZzYWRmM2Zkc2YzMnIzNHJn" +
      "ZHNndjNyZHNmc2RmZHNmc2Rmc2RmZGZzYWRmM2Zkc2YzMnIzNHJnZHNndjNyZHNmc2RmZHNmc2Rmc2RmZGZzYWRmM2Zkc2YzMnIzNHJn" +
      "ZHNndjNyZHNmc2RmZHNmc2Rmc2RmZGZzYWRmM2Zkc2YzMnIzNHJnZHNndjNyZHNmc2RmZHNmc2Rmc2RmZGZzYWRmM2Zkc2YzMnIzNHJn" +
      "ZHNndjNyZHNmc2RmZHNmc2Rmc2RmZGZzYWRmM2Zkc2YzMnIzNHJnZHNndjNyZHNmc2RmZHNmc2Rmc2RmZGZzYWRmM2Zkc2YzMnIzNHJn" +
      "ZHNndjNyZHNmc2RmZHNmc2Rmc2RmZGZzYWRmM2Zkc2YzMnIzNHJnZHNndjNyZHNmc2RmZHNmc2Rmc2RmZGZzYWRmM2Zkc2YzMnIzNHJn" +
      "ZHNndjNyZHNmc2RmZHNmc2Rmc2RmZGZzYWRmM2Zkc2YzMnIzNHJnZHNndjNyZHNmc2RmZHNmc2Rmc2RmZGZzYWRmM2Zkc2YzMnIzNHJn" +
      "ZHNndjNyZHNmc2RmZHNmc2Rmc2RmZGZzYWRmM2Zkc2YzMnIzNHJnZHNndjNyZHNmc2RmZHNmc2Rmc2RmZGZzYWRmM2Zkc2YzMnIzNHJn" +
      "ZHNndjNyZHNmc2RmZHNmc2Rmc2RmZGZzYWRmM2Zkc2YzMnIzNHJnZHNndjNyZHNmc2RmZHNmc2Rmc2RmZGZzYWRmM2Zkc2YzMnIzNHJn" +
      "ZHNndjNyZHNmc2RmZHNmc2Rmc2RmZGZzYWRmM2Zkc2YzMnIzNHJnZHNndjNyZHNmc2RmZHNmc2Rmc2RmZGZzYWRmM2Zkc2YzMnIzNHJn" +
      "ZHNndjNyZHNmc2RmZHNmc2Rmc2RmZGZzYWRmM2Zkc2YzMnIzNHJnZHNndjNyZHNmc2RmZHNmc2Rmc2RmZGZzYWRmM2Zkc2YzMnIzNHJn" +
      "ZHNndjNyZHNmc2RmZHNmc2Rmc2RmZGZzYWRmM2Zkc2YzMnIzNHJnZHNndjNyZHNmc2RmZHNmc2Rmc2RmZGZzYWRmM2Zkc2YzMnIzNHJn" +
      "ZHNndjNyZHNmc2RmZHNmc2Rmc2RmZGZzYWRmM2Zkc2YzMnIzNHJnZHNndjNyZHNmc2RmZHNmc2Rmc2RmZGZzYWRmM2Zkc2YzMnIzNHJn" +
      "ZHNndjNyZHNmc2RmZHNmc2Rmc2RmZGZzYWRmM2Zkc2YzMnIzNHJnZHNndjNyZHNmc2RmZHNmc2Rmc2RmZGZzYWRmM2Zkc2YzMnIzNHJn" +
      "ZHNndjNyZHNmc2RmZHNmc2Rmc2RmZGZzYWRmM2Zkc2YzMnIzNHJnZHNndjNyZHNmc2RmZHNmc2Rmc2RmZGZzYWRmM2Zkc2YzMnIzNHJn" +
      "ZHNndjNyZHNmc2RmZHNmc2Rmc2RmZGZzYWRmM2Zkc2YzMnIzNHJnZHNndjNyZHNmc2RmZHNmc2Rmc2RmZGZzYWRmM2Zkc2YzMnIzNHJn" +
      "ZHNndjNyZHNmc2RmZHNmc2Rmc2RmZGZzYWRmM2Zkc2YzMnIzNHJnZHNndjNyZHNmc2RmZHNmc2Rmc2RmZGZzYWRmM2Zkc2YzMnIzNHJn" +
      "ZHNndjNyZHNmc2RmZHNmc2Rmc2RmZGZzYWRmM2Zkc2YzMnIzNHJnZHNndjNyZHNmc2RmZHNmc2Rmc2RmZGZzYWRmM2Zkc2YzMnIzNHJn" +
      "ZHNndjNyZHNmc2RmZHNmc2Rmc2RmZGZzYWRmM2Zkc2YzMnIzNHJnZHNndjNyZHNmc2RmZHNmc2Rmc2RmZGZzYWRmM2Zkc2YzMnIzNHJn" +
      "ZHNndjNyZHNmc2RmZHNmc2Rmc2RmZGZzYWRmM2Zkc2YzMnIzNHJnZHNndjNyZHNmc2RmZHNmc2Rmc2RmZGZzYWRmM2Zkc2YzMnIzNHJn" +
      "ZHNndjNyZHNmc2RmZHNmc2Rmc2RmZGZzYWRmM2Zkc2YzMnIzNHJnZHNndjNyZHNmc2RmZHNmc2Rmc2RmZGZzYWRmM2Zkc2YzMnIzNHJn" +
      "ZHNndjNyZHNmc2RmZHNmc2Rmc2RmZGZzYWRmM2Zkc2YzMnIzNHJnZHNndjNyZHNmc2RmZHNmc2Rmc2RmZGZzYWRmM2Zkc2YzMnIzNHJn" +
      "ZHNndjNyZHNmc2RmZHNmc2Rmc2RmZGZzYWRmM2Zkc2YzMnIzNHJnZHNndjNyZHNmc2RmZHNmc2Rmc2RmZGZzYWRmM2Zkc2YzMnIzNHJn" +
      "ZHNndjNyZHNmc2RmZHNmc2Rmc2RmZGZzYWRmM2Zkc2YzMnIzNHJnZHNndjNyZHNmc2RmZHNmc2Rmc2RmZGZzYWRmM2Zkc2YzMnIzNHJn" +
      "ZHNndjNyZHNmc2RmZHNmc2Rmc2RmZGZzYWRmM2Zkc2YzMnIzNHJnZHNndjNyZHNmc2RmZHNmc2Rmc2RmZGZzYWRmM2Zkc2YzMnIzNHJn" +
      "ZHNndjNyZHNmc2RmZHNmc2Rmc2RmZGZzYWRmM2Zkc2YzMnIzNHJnZHNndjNyZHNmc2RmZHNmc2Rmc2RmZGZzYWRmM2Zkc2YzMnIzNHJn" +
      "ZHNndjNyZHNmc2RmZHNmc2Rmc2RmZGZzYWRmM2Zkc2YzMnIzNHJnZHNndjNyZHNmc2RmZHNmc2Rmc2RmZGZzYWRmM2Zkc2YzMnIzNHJn" +
      "ZHNndjNyZHNmc2RmZHNmc2Rmc2RmZGZzYWRmM2Zkc2YzMnIzNHJnZHNndjNyZHNmc2RmZHNmc2Rmc2RmZGZzYWRmM2Zkc2YzMnIzNHJn" +
      "ZHNndjNyZHNmc2RmZHNmc2Rmc2RmZGZzYWRmM2Zkc2YzMnIzNHJnZHNndjNyZHNmc2RmZHNmc2Rmc2RmZGZzYWRmM2Zkc2YzMnIzNHJn" +
      "ZHNndjNyZHNmc2RmZHNmc2Rmc2RmZGZzYWRmM2Zkc2YzMnIzNHJnZHNndjNyZHNmc2RmZHNmc2Rmc2RmZGZzYWRmM2Zkc2YzMnIzNHJn" +
      "ZHNndjNyZHNmc2RmZHNmc2Rmc2RmZGZzYWRmM2Zkc2YzMnIzNHJnZHNndjNyZHNmc2RmZHNmc2Rmc2RmZGZzYWRmM2Zkc2YzMnIzNHJn" +
      "ZHNndjNyZHNmc2RmZHNmc2Rmc2RmZGZzYWRmM2Zkc2YzMnIzNHJnZHNndjNyZHNmc2RmZHNmc2Rmc2RmZGZzYWRmM2Zkc2YzMnIzNHJn" +
      "ZHNndjNyZHNmc2RmZHNmc2Rmc2RmZGZzYWRmM2Zkc2YzMnIzNHJnZHNndjNyZHNmc2RmZHNmc2Rmc2RmZGZzYWRmM2Zkc2YzMnIzNHJn" +
      "ZHNndjNyZHNmc2RmZHNmc2Rmc2RmZGZzYWRmM2Zkc2YzMnIzNHJnZHNndjNyZHNmc2RmZHNmc2Rmc2RmZGZzYWRmM2Zkc2YzMnIzNHJn" +
      "ZHNndjNyZHNmc2RmZHNmc2Rmc2RmZGZzYWRmM2Zkc2YzMnIzNHJnZHNndjNyZHNmc2RmZHNmc2Rmc2RmZGZzYWRmM2Zkc2YzMnIzNHJn" +
      "ZHNndjNyZHNmc2RmZHNmc2Rmc2RmZGZzYWRmM2Zkc2YzMnIzNHJnZHNndjNyZHNmc2RmZHNmc2Rmc2RmZGZzYWRmM2Zkc2YzMnIzNHJn" +
      "ZHNndjNyZHNmc2RmZHNmc2Rmc2RmZGZzYWRmM2Zkc2YzMnIzNHJnZHNndjNyZHNmc2RmZHNmc2Rmc2RmZGZzYWRmM2Zkc2YzMnIzNHJn" +
      "ZHNndjNyZHNmc2RmZHNmc2Rmc2RmZGZzYWRmM2Zkc2YzMnIzNHJnZHNndjNyZHNmc2RmZHNmc2Rmc2RmZGZzYWRmM2Zkc2YzMnIzNHJn" +
      "ZHNndjNyZHNmc2RmZHNmc2Rmc2RmZGZzYWRmM2Zkc2YzMnIzNHJnZHNndjNyZHNmc2RmZHNmc2Rmc2RmZGZzYWRmM2Zkc2YzMnIzNHJn" +
      "ZHNndjNyZHNmc2RmZHNmc2Rmc2RmZGZzYWRmM2Zkc2YzMnIzNHJnZHNndjNyZHNmc2RmZHNmc2Rmc2RmZGZzYWRmM2Zkc2YzMnIzNHJn" +
      "ZHNndjNyZHNmc2RmZHNmc2Rmc2RmZGZzYWRmM2Zkc2YzMnIzNHJnZHNndjNyZHNmc2RmZHNmc2Rmc2RmZGZzYWRmM2Zkc2YzMnIzNHJn" +
      "ZHNndjNyZHNmc2RmZHNmc2Rmc2RmZGZzYWRmM2Zkc2YzMnIzNHJnZHNndjNyZHNmc2RmZHNmc2Rmc2RmZGZzYWRmM2Zkc2YzMnIzNHJn" +
      "ZHNndjNyZHNmc2RmZHNmc2Rmc2RmZGZzYWRmM2Zkc2YzMnIzNHJnZHNndjNyZHNmc2RmZHNmc2Rmc2RmZGZzYWRmM2Zkc2YzMnIzNHJn" +
      "ZHNndjNyZHNmc2RmZHNmc2Rmc2RmZGZzYWRmM2Zkc2YzMnIzNHJnZHNndjNyZHNmc2RmZHNmc2Rmc2RmZGZzYWRmM2Zkc2YzMnIzNHJn" +
      "ZHNndjNyZHNmc2RmZHNmc2Rmc2RmZGZzYWRmM2Zkc2YzMnIzNHJnZHNndjNyZHNmc2RmZHNmc2Rmc2RmZGZzYWRmM2Zkc2YzMnIzNHJn" +
      "ZHNndjNydg==), None,None\n" +
      "arg types=null,java.lang.String,scala.Some,java.lang.String,java.lang.String,java.lang.String,scala.Some," +
      "scala.None$,scala.None$\n" +
      "constructor=public io.simplifier.plugin.contentrepo.definitions.caseClasses.ContentFileCaseClasses$" +
      "ContentFileAddRequest(int,java.lang.String,scala.Option,java.lang.String,java.lang.String,java.lang.String," +
      "scala.Option,scala.Option,scala.Option)."


  }

}
