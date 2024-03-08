package io.simplifier.plugin.contentrepo.pluginBaseRelated.reducer

import org.json4s._
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec




class reducerTest extends AnyWordSpec with Matchers {


  "The serialized length determination" when {
    "determining  the length without a reduced message" should {
      "yield the correct JSON representation" in new BaseFixture {
        determineSerializedLength(string) mustBe stringLength
        determineSerializedLength(stringReduced) mustBe stringReducedLength
      }
    }
  }

  trait BaseFixture {
    val string: JString = JString("Long String")
    val stringReduced: JString = JString("Long (... shortened 5 of 10 [Byte])")
    val stringLength:Int = 13
    val stringReducedLength:Int = 6
  }

}
