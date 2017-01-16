package controllers.de.fuhsen.wrappers

import org.scalatest.{FlatSpec, MustMatchers}

/**
  * Created on 12/7/16.
  */
class Tor2WebWrapperTest extends FlatSpec with MustMatchers {
  behavior of "tor2web wrapper"

  val wrapper = new Tor2WebWrapper()

  it should "generate the correct next page value" in {
    val nextPageValue = wrapper.extractNextPageQueryValue("not important", Some("100"))
    nextPageValue mustBe Some("200")
  }
}
