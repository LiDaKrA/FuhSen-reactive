package controllers.de.fuhsen.wrappers

import javax.inject.Inject

import com.typesafe.config.ConfigFactory
import play.api.libs.json.{JsError, JsSuccess, JsPath, Reads}
import play.api.libs.ws.{WSClient, WSRequest}
import play.api.mvc.{Action, Controller}
import views.html.token_retrieval
import scala.collection.mutable.ListBuffer
import play.api.libs.functional.syntax._
import play.api.libs.concurrent.Execution.Implicits.defaultContext

/**
  * Created by cmorales on 23.03.2016.
  */
class TokenRetrievalController @Inject() (ws: WSClient) extends Controller{

  def getToken(provider:String) = Action { request =>
    Redirect(ConfigFactory.load.getString("facebook.request_code.url")
        +"?client_id="+ConfigFactory.load.getString("facebook.app.key")
        +"&redirect_uri="+ConfigFactory.load.getString("facebook.login.redirect.uri")
        +"&response_type=code"
        +"&scope="+ConfigFactory.load.getString("facebook.scope"))
  }

  def code2token(code:String, wrapperId:String) = Action.async{ request =>
    implicit val tokenReads: Reads[Token] =
      (Reads.pure(wrapperId) and
        (JsPath \ "access_token").read[String] and
        (JsPath \ "token_type").read[String] and
        (JsPath \ "expires_in").read[Long] and
        Reads.pure(System.currentTimeMillis()/1000)
      )(Token.apply _)

    val apiResponse: WSRequest = ws.url(ConfigFactory.load.getString("facebook.cod2accestoken.url")
      +"?client_id="+ConfigFactory.load.getString("facebook.app.key")
      +"&redirect_uri="+ConfigFactory.load.getString("facebook.login.redirect.uri")
      +"&client_secret="+ConfigFactory.load.getString("facebook.app.secret")
      +"&code="+code)

    apiResponse.get().map { response =>
      response.json.validate[Token] match {
        case s: JsSuccess[Token] => {
          TokenManager.addToken(s.get)
          Ok(token_retrieval.render(TokenManager.getFBTokenLifeLength))
        }
        case e: JsError => {
          throw new Exception("ERROR: Facebook authentication error.")
        }
      }
    }
  }
}

object TokenManager {
  var token_list = new ListBuffer[Token]()

  def getFBTokenLifeLength(): String = {
    token_list.find(current_token => current_token.provider == "facebook") match {
      case matched_token : Some[Token] =>
        val curr_token = matched_token.get
        val now = System.currentTimeMillis / 1000
        val elapsed_time = now - curr_token.received_time_in_secs

        elapsed_time match {
          case x if x >= curr_token.expires_in => "0" //Expired
          case _ => (curr_token.expires_in - elapsed_time).toString //Lifetime un seconds
        }
      case None => "-1" //No Token found
    }
  }

  def addToken(token_ : Token):Unit = {
    token_list += token_
  }

  def getAccesTokenByProvider(provider:String):String={
    token_list.find(current_token => current_token.provider == "facebook") match {
      case matched_token : Some[Token] => matched_token.get.access_token
      case None => "" //No Token found
    }
  }
}

case class Token(provider: String, access_token: String, token_type: String, expires_in: Long, received_time_in_secs: Long)