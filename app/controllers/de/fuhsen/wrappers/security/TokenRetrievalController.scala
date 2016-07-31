/*
 * Copyright (C) 2016 EIS Uni-Bonn
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package controllers.de.fuhsen.wrappers.security

import javax.inject.Inject

import com.typesafe.config.ConfigFactory
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.functional.syntax._
import play.api.libs.json.{JsError, JsPath, JsSuccess, Reads}
import play.api.libs.ws.{WSClient, WSRequest}
import play.api.mvc.{Action, Controller}
import views.html.index

import scala.collection.mutable.ListBuffer
import play.api.libs.oauth.ConsumerKey
import play.api.libs.oauth.ServiceInfo
import play.api.libs.oauth.OAuth
import play.api.libs.oauth.RequestToken

/**
  * Created by cmorales on 23.03.2016.
  */
class TokenRetrievalController @Inject() (ws: WSClient) extends Controller{

  val KEY: ConsumerKey = ConsumerKey(ConfigFactory.load.getString("xing.app.key"),ConfigFactory.load.getString("xing.app.secret"))
  val XING:OAuth = OAuth(ServiceInfo( ConfigFactory.load.getString("xing.request_code.url"),
    ConfigFactory.load.getString("xing.cod2accestoken.url"),
    ConfigFactory.load.getString("xing.authorize.url"), KEY))

  def getToken(provider:String) = Action { request =>

    provider match {
      case "facebook" =>
        Redirect(ConfigFactory.load.getString("facebook.request_code.url")
          +"?client_id="+ConfigFactory.load.getString("facebook.app.key")
          +"&redirect_uri="+ConfigFactory.load.getString("facebook.login.redirect.uri")
          +"&response_type=code"
          +"&scope="+ConfigFactory.load.getString("facebook.scope"))
      case "xing" =>
        XING.retrieveRequestToken(ConfigFactory.load.getString("xing.login.redirect.uri")) match {
          case Right(t) => {
            Redirect(XING.redirectUrl(t.token)).withSession("token" -> t.token, "secret" -> t.secret)
          }
          case Left(e) => throw e
        }
    }
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
          Ok(index.render())
        }
        case e: JsError => {
          throw new Exception("ERROR: Authentication error.")
        }
      }
    }
  }

  def code2tokenX(wrapperId:String, oauth_token:String, oauth_verifier:String) = Action {
    response =>
      var token = response.session.get("token").get
      var secret = response.session.get("secret").get

      var tokenObj:RequestToken = new RequestToken(token, secret)

    XING.retrieveAccessToken(tokenObj, oauth_verifier) match {
      case Right(t) => {
        System.out.println("t.token: "+t.token)
        System.out.println("t.secret: "+t.secret)
        TokenManager.addToken(new Token("xing", t.token, t.secret, 600, System.currentTimeMillis()/1000))
        Ok(index.render())
      }
      case Left(e) => throw e
    }
  }
}



object TokenManager {
  var token_list = new ListBuffer[Token]()

  def getTokenLifeLength(social_network : String): String = {
    token_list.find(current_token => current_token.provider == social_network) match {
      case matched_token : Some[Token] =>
        val curr_token = matched_token.get
        val now = System.currentTimeMillis / 1000
        val elapsed_time = now - curr_token.received_time_in_secs

        elapsed_time match {
          case x if x >= curr_token.expires_in => token_list = new ListBuffer[Token]()//Expired
                        "-1" //Expired
          case _ => ( (curr_token.expires_in-elapsed_time)/60 ).toString //Lifetime in minuten.
        }
      case None => "-1" //No Token found
    }
  }

  def addToken(token_ : Token):Unit = {
    token_list += token_
  }

  def getAccessTokenByProvider(provider:String):Option[Token]={
    token_list.find(current_token => current_token.provider == provider) match {
      case matched_token : Some[Token] => matched_token
      case None =>  None//No Token found
    }
  }
}

case class Token(provider: String, access_token: String, token_type: String, expires_in: Long, received_time_in_secs: Long)