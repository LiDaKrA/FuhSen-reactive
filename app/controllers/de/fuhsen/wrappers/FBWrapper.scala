package controllers.de.fuhsen.wrappers

import javax.inject.Inject
import play.api.libs.ws.{WSRequest, WSClient, WSResponse}
import play.api.mvc.{Action, Controller, Result}
import play.api.libs.json._
import play.api.libs.json.Reads._
import play.api.libs.functional.syntax._

import play.api.libs.concurrent.Execution.Implicits.defaultContext

import scala.concurrent.Future
import scala.util.{Failure, Success}
import play.api.mvc.Cookie
import play.api.mvc.DiscardingCookie
import play.mvc.Http
import play.mvc.Http.Request

/**
  * Created by cmorales on 16.03.2016.
  */
class FBWrapper @Inject() (ws: WSClient) extends Controller{

  def search(query:String) = Action{ request =>
    Redirect("https://www.facebook.com/dialog/oauth?client_id=1744904279055316&redirect_uri=http://localhost:9000/facebook/code2token&response_type=code&scope=public_profile,user_friends,email,user_about_me,user_posts").withCookies(Cookie("query", query.replace(" ","%20")))
  }

  def actual_search(code:String) = Action.async{ request =>

    val cookie_query_option = request.cookies.get("query")

    val cookie_query : String = cookie_query_option match {
      case Some(name) =>  name.value.replace("%20"," ")
      case None => "No query value"
    }

    implicit val tokenReads: Reads[Token] = (
      (JsPath \ "access_token").read[String] and
        (JsPath \ "token_type").read[String] and
        (JsPath \ "expires_in").read[Int]
      )(Token.apply _)

    val apiResponse: WSRequest = ws.url("https://graph.facebook.com/v2.3/oauth/access_token?client_id=1744904279055316&redirect_uri=http://localhost:9000/facebook/code2token&client_secret=4a10b25a3727e529b3b332bc63a59425&code="+code)

    apiResponse.get().map {
      response =>
        response.json.validate[Token] match {
          case s: JsSuccess[Token] => {
            val response_token: Token = s.get

            val search_response : WSRequest = ws.url("https://graph.facebook.com/search?access_token=" + response_token.access_token + "&type=user&fields=id,name,first_name,last_name,age_range,link,gender,locale,picture,timezone,updated_time,verified,email").withQueryString("q" -> cookie_query)

            for {searchResponse <- search_response.get()}
              yield {
                println(searchResponse.body)
              }
            Ok("Test")
          }
          case e: JsError => {
            println("Error:"+JsError.toFlatJson(e))
            Ok("ERROR")
          }
        }
    }
  }
}
case class Token(access_token: String, token_type: String, expires_in: Int)

