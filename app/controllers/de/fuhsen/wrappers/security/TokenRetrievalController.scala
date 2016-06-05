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
          Ok(index.render())
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
          case _ => ( (curr_token.expires_in-elapsed_time)/3600 ).toString //Lifetime in hours.
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