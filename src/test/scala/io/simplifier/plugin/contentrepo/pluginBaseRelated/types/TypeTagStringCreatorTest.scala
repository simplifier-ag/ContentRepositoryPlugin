package io.simplifier.plugin.contentrepo.pluginBaseRelated.types

import org.json4s.JValue
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec

import scala.reflect.runtime.universe._
import scala.util.matching.Regex

class TypeTagStringCreatorTest extends AnyWordSpec with Matchers {


  "The Type Tag Utils" when {
    "creating a string" should {
      "yield the correct string" in new BaseFixture {
        createString(typeTag[Array[List[Int]]]) mustBe arrayFromTypeSymbol
        createString(TypeTag.Nothing) mustBe nothingFromTypeSymbol
        createString(TypeTag.Null) mustBe nullFromTypeSymbol
        createString(typeTag[String]) mustBe stringFromTypeSymbol
        createString(typeTag[JValue]) mustBe jValueFromTypeSymbol
      }
    }

    "creating a string from the TPE" should {
      "yield the correct string" in new BaseFixture {
        createStringFromTPE(typeTag[Array[List[Int]]]) must fullyMatch regex arrayFromTPERegex
        createStringFromTPE(TypeTag.Nothing) mustBe nothingFromTypeSymbol
        createStringFromTPE(TypeTag.Null) mustBe nullFromTypeSymbol
        createStringFromTPE(typeTag[String]) mustBe stringFromTypeSymbol
        createStringFromTPE(typeTag[JValue]) mustBe jValueFromTPE
      }
    }

    "creating a string from the Type Symbol" should {
      "yield the correct string" in new BaseFixture {
        createStringFromTypeSymbol(typeTag[Array[List[Int]]]) mustBe arrayFromTypeSymbol
        createStringFromTypeSymbol(TypeTag.Nothing) mustBe nothingFromTypeSymbol
        createStringFromTypeSymbol(TypeTag.Null) mustBe nullFromTypeSymbol
        createStringFromTypeSymbol(typeTag[String]) mustBe stringFromTypeSymbol
        createStringFromTypeSymbol(typeTag[JValue]) mustBe jValueFromTypeSymbol
      }
    }
  }


  trait BaseFixture extends TypeTagStringCreator {
    val arrayFromTypeSymbol: String = "Array"
    val nothingFromTypeSymbol: String = "Nothing"
    val nullFromTypeSymbol: String = "Null"
    val stringFromTypeSymbol: String = "String"
    val jValueFromTypeSymbol: String = "JValue"


    val arrayFromTPERegex:Regex = s"(?:scala.)?Array\\[(?:scala.)?List\\[Int\\]\\]".r
    val jValueFromTPE: String = "org.json4s.JValue"

  }

}