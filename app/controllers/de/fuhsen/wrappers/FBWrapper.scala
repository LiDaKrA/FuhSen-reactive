package controllers.de.fuhsen.wrappers

import javax.inject.Inject
import com.typesafe.config.ConfigFactory
import play.api.libs.ws.{WSRequest, WSClient}
import play.api.mvc.{Action, Controller}
import play.api.libs.json._
import play.api.libs.json.Reads._
import play.api.libs.functional.syntax._
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.mvc.Cookie

/**
  * Created by cmorales on 16.03.2016.
  */
class FBWrapper @Inject() (ws: WSClient) extends Controller{

  def search(query:String) = Action{ request =>
    Redirect(ConfigFactory.load.getString("facebook.request_code.url")
            +"?client_id="+ConfigFactory.load.getString("facebook.app.key")
            +"&redirect_uri="+ConfigFactory.load.getString("facebook.login.redirect.uri")
            +"&response_type=code"
            +"&scope="+ConfigFactory.load.getString("facebook.scope")).withCookies(Cookie("query", query.replace(" ","%20")))
  }

  def actual_search(code:String) = Action.async{ request =>
    val cookie_query : String = request.cookies.get("query") match {
      case Some(name) =>  name.value.replace("%20"," ")
      case None => throw new Exception("ERROR: No query value.")
    }

    implicit val tokenReads: Reads[Token] = (
      (JsPath \ "access_token").read[String] and
        (JsPath \ "token_type").read[String] and
        (JsPath \ "expires_in").read[Int]
      )(Token.apply _)

    val apiResponse: WSRequest = ws.url(ConfigFactory.load.getString("facebook.cod2accestoken.url")
                                       +"?client_id="+ConfigFactory.load.getString("facebook.app.key")
                                       +"&redirect_uri="+ConfigFactory.load.getString("facebook.login.redirect.uri")
                                       +"&client_secret="+ConfigFactory.load.getString("facebook.app.secret")
                                       +"&code="+code)

    apiResponse.get().map { response =>
        response.json.validate[Token] match {
          case s: JsSuccess[Token] => {
            val search_response : WSRequest = ws.url(ConfigFactory.load.getString("facebook.search.url")
                                                    +"?access_token=" + s.get.access_token
                                                    +"&type=user"
                                                    +"&fields="+ConfigFactory.load.getString("facebook.search.fields")).withQueryString("q" -> cookie_query)
            for {searchResponse <- search_response.get()}
              yield {
                println(searchResponse.body)
              }
            Ok("OK")
          }
          case e: JsError => {
            //println("Error:"+JsError.toFlatJson(e))
            throw new Exception("ERROR: Facebook authentication error.")
          }
        }
    }
  }
}
case class Token(access_token: String, token_type: String, expires_in: Int)