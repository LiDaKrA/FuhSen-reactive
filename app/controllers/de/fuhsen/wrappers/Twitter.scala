/*
 * Copyright (C) 2015 EIS Uni-Bonn
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

package controllers.de.fuhsen.wrappers

import javax.inject.Inject

import com.typesafe.config.ConfigFactory
import play.Logger
import play.api.libs.oauth.{OAuthCalculator, ConsumerKey, RequestToken}
import play.api.libs.ws.{WSRequest, WSClient}
import play.api.mvc.{Action, Controller}
import play.api.libs.concurrent.Execution.Implicits.defaultContext

class Twitter @Inject() (ws: WSClient) extends Controller {

  def search (query: String) = Action.async {

    Logger.info("Starting Twitter Search with query: " + query)

    val KEY = ConsumerKey(ConfigFactory.load.getString("twitter.consumer.key"), ConfigFactory.load.getString("twitter.consumer.secret"))
    //TODO Read access token key from config file. The problem is this value is returned as Object not String from the configuration file
    //val accessToken = ConfigFactory.load.getValue("twitter.access.token")
    val accessToken = ""
    val TOKEN: RequestToken = new RequestToken(accessToken, ConfigFactory.load.getString("twitter.access.token.secret"))

    var query_string: String = query
    query_string = query_string.replace(" ", "%20")
    Logger.info("Query String Transformed: " + query_string)

    val url = ConfigFactory.load.getString("twitter.url")
    val request: WSRequest = ws.url(url)
      .withQueryString("q" -> query_string)
      .sign(OAuthCalculator(KEY, TOKEN))

    request.get().map { response =>
      Ok(response.body)
    }
  }

}
