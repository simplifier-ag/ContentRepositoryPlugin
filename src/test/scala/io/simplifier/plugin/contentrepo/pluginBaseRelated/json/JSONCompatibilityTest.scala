package io.simplifier.plugin.contentrepo.pluginBaseRelated.json

import  io.simplifier.plugin.contentrepo.pluginBaseRelated.json.JSONCompatibility.MalformedJsonString
import org.json4s.jackson.JsonMethods.{parse, pretty}
import org.json4s.{JArray, JField, JInt, JNothing, JObject, JString}
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

import scala.util.{Failure, Success}

class JSONCompatibilityTest extends AnyWordSpec with Matchers {

  "JSONCompatibility" when {

    "parseJsonOrEmptyString" should {

      "parse non empty string as json value" in {
        val expected = JObject(JField("A", JInt(BigInt(123))) :: Nil)
        JSONCompatibility.parseJsonOrEmptyString("""{ "A": 123 }""") should be (expected)
      }

      "parse empty string as JNothing" in {
        JSONCompatibility.parseJsonOrEmptyString("") should be (JNothing)
        JSONCompatibility.parseJsonOrEmptyString(Array.emptyByteArray) should be (JNothing)
      }
    }

    "LegacySearch" should {

      "find same results like liftweb" in {
        val nothing = JSONCompatibility.LegacySearch(JNothing)
        nothing \\ "notContained" shouldBe JObject(List())
        nothing \ "notContained" shouldBe JNothing

        val containsOther = JSONCompatibility.LegacySearch(JObject(List(JField("other", JString("val1")))))
        containsOther \\ "notContained" shouldBe JObject(List())
        containsOther \ "notContained" shouldBe JNothing

        val containsOnce = JSONCompatibility.LegacySearch(JObject(List(JField("containedOnce", JString("val1")))))
        containsOnce \\ "containedOnce" shouldBe JObject(List(JField("containedOnce", JString("val1"))))
        containsOnce \ "containedOnce" shouldBe JString("val1")

        val containsTwice = JSONCompatibility.LegacySearch(JObject(List(JField("twice", JString("val1")), JField("twice", JString("val2")))))
        containsTwice \\ "twice" shouldBe JObject(List(JField("twice", JString("val1")), JField("twice", JString("val2"))))
        containsTwice \ "twice" shouldBe JArray(List(JString("val1"), JString("val2")))
      }
    }


    "fixInputString" should {

      "preserve literals" in {
        JSONCompatibility.fixInputString(""""Hello World!"""") should be(Success(""""Hello World!""""))
        JSONCompatibility.fixInputString("42") should be(Success("42"))
        JSONCompatibility.fixInputString("3.14") should be(Success("3.14"))
        JSONCompatibility.fixInputString("true") should be(Success("true"))
        JSONCompatibility.fixInputString("false") should be(Success("false"))
        JSONCompatibility.fixInputString("null") should be(Success("null"))
      }

      "reject invalid input" in {
        JSONCompatibility.fixInputString("$#@!") should matchPattern {
          case Failure(MalformedJsonString(msg, 1, 1, _)) if msg.contains("Expect JSON value, but found '$'") =>
        }
      }

      "preserve escaped quotes" in {
        fixParseAndRender(""""Hello \"World\""""") should be(""""Hello \"World\""""")
      }

      "preserve unicode characters in strings" in {
        val q = '"'
        val bs = '\\'
        val ae = bs + "u00E4"
        val oe = bs + "u00F6"
        val escapedUnicodeString = q + "H" + ae + "llo W" + oe + "rld!" + q

        JSONCompatibility.fixInputString(escapedUnicodeString) should be(Success(escapedUnicodeString))
      }


      "preserve object" in {
        val jsonObject =
          """{
            | "header": "test",
            | "data": {
            |     "x": 54.233443,
            |     "y": 49.458784
            |   },
            | "isValid": true,
            | "content": undefined
            |}""".stripMargin
        JSONCompatibility.fixInputString(jsonObject) should be(Success(jsonObject))
        JSONCompatibility.fixInputString("{}") should be(Success("{}"))
      }

      "preserve array" in {
        val jsonArray =
          """[
            | 1,2,3,4,
            | "five", "seven",
            | {
            |     "x": 54.233443,
            |     "y": 49.458784
            | },
            | null
            |]""".stripMargin
        JSONCompatibility.fixInputString(jsonArray) should be(Success(jsonArray))
        JSONCompatibility.fixInputString("[]") should be(Success("[]"))
      }


      "fix not escaped control characters in string literal" in {
        val CHAR_TAB: Char = 0x0009
        val CHAR_LF: Char = 0x000A
        val CHAR_CR: Char = 0x000D
        val CHAR_FF: Char = 0x000C
        val CHAR_BS: Char = 0x0008

        JSONCompatibility.fixInputString(s""""123${CHAR_TAB}456"""") should be(Success(""""123\t456""""))
        JSONCompatibility.fixInputString(s""""123${CHAR_CR}456"""") should be(Success(""""123\r456""""))
        JSONCompatibility.fixInputString(s""""123${CHAR_LF}456"""") should be(Success(""""123\n456""""))
        JSONCompatibility.fixInputString(s""""123${CHAR_FF}456"""") should be(Success(""""123\f456""""))
        JSONCompatibility.fixInputString(s""""123${CHAR_BS}456"""") should be(Success(""""123\b456""""))

        JSONCompatibility.fixInputString(
          s""""123
             |456"""".stripMargin) should be(Success(""""123\n456""""))

        JSONCompatibility.fixInputString(
          s"""{
             |  "message": "Hello$CHAR_CR${CHAR_LF}World!"
             |}""".stripMargin) should be(Success(
          """{
            |  "message": "Hello\r\nWorld!"
            |}""".stripMargin))
      }

      "fix missing commas in array" in {
        fixParseAndRender(s"""[ 1 2 3 ]""") should be(s"""[ 1, 2, 3 ]""")
        fixParseAndRender(s"""[ true false "ok or not" ]""") should be(s"""[ true, false, "ok or not" ]""")
        fixParseAndRender(s"""[ "A", "B" "C" ]""") should be(s"""[ "A", "B", "C" ]""")
        fixParseAndRender(s"""[ "A"  "B" "C" ]""") should be(s"""[ "A", "B", "C" ]""")
      }

      "reject unclosed array" in {
        JSONCompatibility.fixInputString(s"""[ "A", "B" """) should matchPattern {
          case Failure(JSONCompatibility.MalformedJsonString(msg, 1, 11, _)) if msg.contains("Expect ']', ',' or array element") =>
        }
      }

      "reject array with additional commas when option is set" in {
        JSONCompatibility.fixInputString(s"""[ , "A", "B" ]""", removeCommas = false) should matchPattern {
          case Failure(JSONCompatibility.MalformedJsonString(msg, 1, 5, _)) if msg.contains("Expect value but found ','") =>
        }
        JSONCompatibility.fixInputString(s"""[ "A", "B", , "C" ]""", removeCommas = false) should matchPattern {
          case Failure(JSONCompatibility.MalformedJsonString(msg, 1, 13, _)) if msg.contains("Expect value but found ','") =>
        }
        JSONCompatibility.fixInputString(s"""[ "A", "B", "C" , ]""", removeCommas = false) should matchPattern {
          case Failure(JSONCompatibility.MalformedJsonString(msg, 1, 19, _)) if msg.contains("Expect array element but found ','") =>
        }
      }

      "fix array with additional commas when option is set" in {
        JSONCompatibility.fixInputString(s"""[ , "A", "B" ]""") should be(Success(s"""[  "A", "B" ]"""))
        JSONCompatibility.fixInputString(s"""[ "A", "B", , "C" ]""") should be(Success(s"""[ "A", "B",  "C" ]"""))
        JSONCompatibility.fixInputString(s"""[ , "A", "B", ,, , "C"  d ]""") should be(Success(s"""[  "A", "B",   "C"  ,d ]"""))
        JSONCompatibility.fixInputString(s"""[ ,,, ]""") should be(Success(s"""[  ]"""))
      }

      "fix missing commas in object" in {
        fixParseAndRender(
          """{
            |  "a" : 42
            |  "b" : "Hello World!"
            |}""".stripMargin) should be(
          """{
            |  "a" : 42,
            |  "b" : "Hello World!"
            |}""".stripMargin)
      }


      "reject unclosed object" in {
        val json =
          s"""{ "test": "unfinished"
             |
             |""".stripMargin
        JSONCompatibility.fixInputString(json) should matchPattern {
          case Failure(JSONCompatibility.MalformedJsonString(msg, 3, 1, `json`)) if msg.contains(s"""Expect '}', ',' or '"'""") =>
        }
      }

      "reject object with missing colon" in {
        JSONCompatibility.fixInputString(s"""{ "test" "missing" }""") should matchPattern {
          case Failure(JSONCompatibility.MalformedJsonString(msg, 1, 10, _)) if msg.contains(s"""Missing ':' after object field name""") =>
        }
      }

      "reject object with additional commas if option is set" in {
        JSONCompatibility.fixInputString(s"""{ , "too": "manny commas" }""", removeCommas = false) should matchPattern {
          case Failure(JSONCompatibility.MalformedJsonString(msg, 1, 5, _)) if msg.contains(s"""Expect quoted field name but found ','""") =>
        }
        JSONCompatibility.fixInputString("""{ "a": 123, , "b": 42 }""", removeCommas = false) should matchPattern {
          case Failure(JSONCompatibility.MalformedJsonString(msg, 1, 13, _)) if msg.contains(s"""Expect value but found ','""") =>
        }
      }

      "fix object with additional commas" in {
        JSONCompatibility.fixInputString(s"""{ , "too": "ok" }""") should be(Success(s"""{  "too": "ok" }"""))
        JSONCompatibility.fixInputString("""{ "a": 123, , "b": 42 }""") should be (Success("""{ "a": 123,  "b": 42 }"""))
        JSONCompatibility.fixInputString("""{ "a": 123, ,,, "b": 42 }""") should be (Success("""{ "a": 123,  "b": 42 }"""))
        JSONCompatibility.fixInputString("""{ "a": 123, "b": 42, }""") should be (Success("""{ "a": 123, "b": 42 }"""))
      }

    }


    def fixParseAndRender(s: String): String =
      pretty(parse(JSONCompatibility.fixInputString(s).get))
  }
}