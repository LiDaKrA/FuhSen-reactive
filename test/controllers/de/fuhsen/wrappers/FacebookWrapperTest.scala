package controllers.de.fuhsen.wrappers

import org.scalatest.{FlatSpec, MustMatchers}

/**
  * Created on 12/7/16.
  */
class FacebookWrapperTest extends FlatSpec with MustMatchers {
  behavior of "Facebook wrapper"

  val wrapper = new FacebookWrapper()

  it should "extract the next page parameter" in {
    val result =
      """
        |{
        |  "data": [],
        |  "paging": {
        |    "cursors": {
        |      "after": "MTAxNTExOTQ1MjAwNzI5NDE=",
        |      "before": "NDMyNzQyODI3OTQw"
        |    },
        |    "previous": "https://graph.facebook.com/me/albums?limit=25&before=NDMyNzQyODI3OTQw",
        |    "next": "https://graph.facebook.com/me/albums?limit=25&after=MTAxNTExOTQ1MjAwNzI5NDE="
        |  }
        |}
      """.stripMargin
    val nextValue = wrapper.extractNextPageQueryValue(result, Some("not important here"))
    nextValue mustBe Some("MTAxNTExOTQ1MjAwNzI5NDE=")
  }
}
